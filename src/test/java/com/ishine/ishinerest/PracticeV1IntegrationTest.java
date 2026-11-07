package com.ishine.ishinerest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishine.ishinerest.entity.*;
import com.ishine.ishinerest.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against an isolated in-memory H2 database; the real data/istudydb file is not touched. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:practicev1;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=localhost"
})
@AutoConfigureMockMvc
class PracticeV1IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired ChapterRepository chapterRepository;
    @Autowired SectionRepository sectionRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired PracticeSessionDetailRepository legacyRepository;
    @Autowired TransactionTemplate tx;

    Long studentId;
    Chapter chapter;

    @BeforeEach
    void setUp() {
        legacyRepository.deleteAll();
        questionRepository.deleteAll();
        sectionRepository.deleteAll();

        studentId = tx.execute(status -> {
            User u = new User();
            u.setEmail("s" + System.nanoTime() + "@x.com");
            u.setName("S");
            u.setPasswordHash("x");
            u.setRole(UserRole.STUDENT);
            u = userRepository.save(u);
            Student s = new Student();
            s.setUser(u);
            return studentRepository.save(s).getStudentId();
        });

        SubjectEntity subject = new SubjectEntity();
        subject.setSubjectId("SUBJ" + System.nanoTime());
        subject.setSubjectName("Maths");
        subject = subjectRepository.save(subject);
        chapter = new Chapter();
        chapter.setChapterId("CH" + System.nanoTime());
        chapter.setChapterName("Percentages");
        chapter.setSubject(subject);
        chapter = chapterRepository.save(chapter);

        // 6 sections: 3 foundation, 2 intermediate, 1 advanced; 4 questions each (easy/medium/hard mix)
        String[] cx = { "FOUNDATION", "FOUNDATION", "FOUNDATION", "INTERMEDIATE", "INTERMEDIATE", "ADVANCED" };
        String[] diff = { "Basic", "Basic", "Intermediate", "Advanced" };
        for (int i = 0; i < cx.length; i++) {
            Section sec = new Section();
            sec.setSectionId(chapter.getChapterId() + "-S" + (i + 1));
            sec.setSectionName("Section " + (i + 1));
            sec.setDisplayOrder(i + 1);
            sec.setComplexityLevel(cx[i]);
            sec.setChapter(chapter);
            sectionRepository.save(sec);
            for (int j = 0; j < 4; j++) {
                Question q = new Question();
                q.setQuestionText("Q" + i + "-" + j);
                q.setOptionA("a");
                q.setOptionB("b");
                q.setOptionC("c");
                q.setOptionD("d");
                q.setCorrectAnswer("A");
                q.setQuestionType(1);
                q.setDifficultyLevel(diff[j]);
                q.setUsageType("Both");
                q.setQuestionSet("1");
                q.setSectionId(sec.getSectionId());
                q.setQuestionStyle(j % 2 == 0 ? "DIRECT" : "WORD_PROBLEM");
                q.setChapter(chapter);
                questionRepository.save(q);
            }
        }
    }

    JsonNode select(int count) throws Exception {
        MvcResult r = mvc.perform(post("/v1/practice/chapters/" + chapter.getChapterId() + "/questions/select")
                        .param("studentId", String.valueOf(studentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":" + count + ",\"difficulty\":\"AUTO\",\"usageType\":\"PRACTICE\",\"questionSet\":1}"))
                .andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void firstTimeFetchReturnsUniqueQuestionsAcrossSections() throws Exception {
        JsonNode res = select(10);
        assertEquals("FIRST_TIME", res.get("studentType").asText());
        assertEquals(10, res.get("returnedCount").asInt());
        Set<Long> ids = new HashSet<>();
        Set<String> sections = new HashSet<>();
        for (JsonNode q : res.get("questions")) {
            assertTrue(ids.add(q.get("questionId").asLong()), "duplicate question");
            sections.add(q.get("sectionId").asText());
        }
        assertTrue(sections.size() >= 5, "should span several sections but was " + sections.size());
        // learning order: section ids ascend with display order
        String prev = "";
        for (JsonNode q : res.get("questions")) {
            String cur = q.get("sectionId").asText();
            assertTrue(cur.compareTo(prev) >= 0);
            prev = cur;
        }
        // fetching is read-only
        assertEquals(0, legacyRepository.count());
    }

    @Test
    void asksForMoreThanAvailableReturnsWhatExists() throws Exception {
        JsonNode res = select(50);
        assertEquals(24, res.get("returnedCount").asInt());
    }

    @Test
    void submitGradesOnServerIsIdempotentAndDrivesNextFetch() throws Exception {
        JsonNode first = select(10);
        // answer: section 1 questions wrong, everything else correct, one left unanswered
        StringBuilder answers = new StringBuilder("[");
        int i = 0;
        Set<Long> wrongIds = new HashSet<>();
        Set<Long> allowedRepeats = new HashSet<>(); // previously wrong or left unanswered
        for (JsonNode q : first.get("questions")) {
            if (i > 0) answers.append(",");
            String sec = q.get("sectionId").asText();
            String pick = sec.endsWith("-S1") ? "B" : "A";
            if (pick.equals("B") && i != 9) wrongIds.add(q.get("questionId").asLong());
            if (pick.equals("B") || i == 9) allowedRepeats.add(q.get("questionId").asLong());
            if (i == 9) {
                answers.append("{\"questionId\":").append(q.get("questionId").asLong()).append("}");
            } else {
                answers.append("{\"questionId\":").append(q.get("questionId").asLong())
                        .append(",\"selectedAnswers\":[\"").append(pick).append("\"]}");
            }
            i++;
        }
        answers.append("]");
        String body = "{\"sessionId\":\"sess-1\",\"answers\":" + answers + "}";

        MvcResult r = mvc.perform(post("/v1/practice/chapters/" + chapter.getChapterId() + "/submit")
                        .param("studentId", String.valueOf(studentId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode sub = om.readTree(r.getResponse().getContentAsString());
        assertEquals("SUBMITTED", sub.get("status").asText());
        assertEquals(10, sub.get("totalQuestions").asInt());
        assertEquals(9, sub.get("answeredCount").asInt());
        assertEquals(sub.get("answeredCount").asInt(),
                sub.get("correctCount").asInt() + sub.get("incorrectCount").asInt());
        assertFalse(sub.get("alreadySubmitted").asBoolean());
        long legacyAfterFirst = legacyRepository.count();
        assertEquals(9, legacyAfterFirst);

        // resubmitting the same session is not counted twice
        MvcResult again = mvc.perform(post("/v1/practice/chapters/" + chapter.getChapterId() + "/submit")
                        .param("studentId", String.valueOf(studentId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        assertTrue(om.readTree(again.getResponse().getContentAsString()).get("alreadySubmitted").asBoolean());
        assertEquals(legacyAfterFirst, legacyRepository.count());

        // next fetch is RETURNING and flags the failed section
        JsonNode next = select(10);
        assertEquals("RETURNING", next.get("studentType").asText());
        assertEquals(10, next.get("returnedCount").asInt());
        Set<Long> ids = new HashSet<>();
        next.get("questions").forEach(q -> assertTrue(ids.add(q.get("questionId").asLong())));
        for (JsonNode s : next.get("sections")) {
            if (s.get("sectionId").asText().endsWith("-S1") && s.get("attempts").asInt() > 0) {
                assertTrue(s.get("accuracy").asDouble() < 1.0);
            }
        }
        // 14 unseen questions remain: correctly answered questions never return, and repeats of
        // previously wrong or unanswered questions stay within the 20% cap (2 of 10)
        Set<Long> firstIds = new HashSet<>();
        first.get("questions").forEach(q -> firstIds.add(q.get("questionId").asLong()));
        long repeats = ids.stream().filter(wrongIds::contains).count();
        assertTrue(repeats <= 2, "too many repeats: " + repeats);
        ids.stream().filter(firstIds::contains)
                .forEach(id -> assertTrue(allowedRepeats.contains(id), "correct question repeated: " + id));
    }



    @Test
    void submitRejectsQuestionFromAnotherChapter() throws Exception {
        mvc.perform(post("/v1/practice/chapters/" + chapter.getChapterId() + "/submit")
                        .param("studentId", String.valueOf(studentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"bad\",\"answers\":[{\"questionId\":999999,\"selectedAnswers\":[\"A\"]}]}"))
                .andExpect(status().isBadRequest());
    }
}

