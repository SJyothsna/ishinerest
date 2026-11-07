package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.PracticeSession;
import com.ishine.ishinerest.entity.PracticeSessionAnswer;
import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.pojo.PracticeSubmitRequest;
import com.ishine.ishinerest.pojo.PracticeSubmitResponse;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.PracticeSessionAnswerRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.PracticeSessionRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Saves a completed practice session. Answers are graded on the server from the
 * stored question data; the client never supplies correctness.
 */
@Service
@RequiredArgsConstructor
public class PracticeSubmissionService {

    private final PracticeSessionRepository sessionRepository;
    private final PracticeSessionAnswerRepository answerRepository;
    private final PracticeSessionDetailRepository legacyDetailRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final ChapterRepository chapterRepository;

    @Transactional
    public PracticeSubmitResponse submit(Long studentId, String chapterId, PracticeSubmitRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
        if (!chapterRepository.existsById(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId);
        }

        // Idempotency: an already submitted session returns its stored result
        Optional<PracticeSession> existing = sessionRepository.findById(request.getSessionId());
        if (existing.isPresent()) {
            PracticeSession s = existing.get();
            if (!s.getStudentId().equals(studentId) || !s.getChapterId().equals(chapterId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Session id already used by a different student or chapter");
            }
            if (s.getStatus() == PracticeSession.Status.SUBMITTED) {
                return buildResponse(s, answerRepository.findBySession_SessionIdOrderByAttemptOrderAsc(s.getSessionId()), true);
            }
        }

        // Validate question ids
        Set<Long> seen = new HashSet<>();
        for (PracticeSubmitRequest.Answer a : request.getAnswers()) {
            if (!seen.add(a.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate questionId in submission: " + a.getQuestionId());
            }
        }
        Map<Long, Question> questions = new HashMap<>();
        questionRepository.findAllById(seen).forEach(q -> questions.put(q.getQuestionId(), q));
        for (Long id : seen) {
            Question q = questions.get(id);
            if (q == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not found: " + id);
            }
            if (q.getChapter() == null || !chapterId.equals(q.getChapter().getChapterId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + id + " does not belong to chapter " + chapterId);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        PracticeSession session = existing.orElseGet(PracticeSession::new);
        session.setSessionId(request.getSessionId());
        session.setStudentId(studentId);
        session.setChapterId(chapterId);
        session.setStartedAt(request.getStartedAt());

        List<PracticeSessionAnswer> answers = new ArrayList<>();
        int answered = 0;
        int graded = 0;
        int correct = 0;
        int order = 0;
        for (PracticeSubmitRequest.Answer a : request.getAnswers()) {
            order++;
            Question q = questions.get(a.getQuestionId());
            List<String> selected = cleanSelected(a.getSelectedAnswers());

            PracticeSessionAnswer row = new PracticeSessionAnswer();
            row.setSession(session);
            row.setQuestionId(q.getQuestionId());
            row.setAttemptOrder(a.getAttemptOrder() != null ? a.getAttemptOrder() : order);
            row.setTimeTakenSeconds(a.getTimeTakenSeconds());
            if (!selected.isEmpty()) {
                answered++;
                row.setSelectedAnswers(String.join(",", selected));
                row.setAnsweredAt(now);
                Boolean result = grade(q, selected);
                row.setIsCorrect(result);
                if (result != null) {
                    graded++;
                    if (result) {
                        correct++;
                    }
                }
            }
            answers.add(row);
        }

        session.setTotalQuestions(answers.size());
        session.setAnsweredCount(answered);
        session.setCorrectCount(correct);
        session.setIncorrectCount(graded - correct);
        session.setScorePercentage(graded == 0 ? 0.0 : Math.round(correct * 10000.0 / graded) / 100.0);
        session.setStatus(PracticeSession.Status.SUBMITTED);
        session.setSubmittedAt(now);

        try {
            session = sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same session id won the race
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already being submitted");
        }
        for (PracticeSessionAnswer row : answers) {
            row.setSession(session);
        }
        answerRepository.saveAll(answers);

        // Keep the existing per-question progress table in step so current progress APIs stay accurate
        for (PracticeSessionAnswer row : answers) {
            if (row.getIsCorrect() != null) {
                upsertLegacyDetail(student, questions.get(row.getQuestionId()), row);
            }
        }

        return buildResponse(session, answers, false);
    }

    private void upsertLegacyDetail(Student student, Question question, PracticeSessionAnswer row) {
        Optional<PracticeSessionDetail> existing = legacyDetailRepository
                .findByStudentIdAndQuestionId(student.getStudentId(), question.getQuestionId());
        if (existing.isPresent()) {
            PracticeSessionDetail d = existing.get();
            d.setStudentAnswer(row.getSelectedAnswers());
            d.setIsCorrect(row.getIsCorrect());
            d.setAttemptCount(d.getAttemptCount() + 1);
            legacyDetailRepository.save(d);
        } else {
            PracticeSessionDetail d = new PracticeSessionDetail();
            d.setStudent(student);
            d.setQuestion(question);
            d.setStudentAnswer(row.getSelectedAnswers());
            d.setIsCorrect(row.getIsCorrect());
            d.setAttemptCount(1);
            legacyDetailRepository.save(d);
        }
    }

    private PracticeSubmitResponse buildResponse(PracticeSession s, List<PracticeSessionAnswer> answers, boolean already) {
        PracticeSubmitResponse r = new PracticeSubmitResponse();
        r.setSessionId(s.getSessionId());
        r.setChapterId(s.getChapterId());
        r.setStatus(s.getStatus().name());
        r.setTotalQuestions(s.getTotalQuestions());
        r.setAnsweredCount(s.getAnsweredCount());
        r.setCorrectCount(s.getCorrectCount());
        r.setIncorrectCount(s.getIncorrectCount());
        r.setScorePercentage(s.getScorePercentage());
        r.setSubmittedAt(s.getSubmittedAt());
        r.setAlreadySubmitted(already);
        for (PracticeSessionAnswer a : answers) {
            PracticeSubmitResponse.Result res = new PracticeSubmitResponse.Result();
            res.setQuestionId(a.getQuestionId());
            res.setAttemptOrder(a.getAttemptOrder());
            res.setAnswered(a.getSelectedAnswers() != null);
            res.setIsCorrect(a.getIsCorrect());
            r.getResults().add(res);
        }
        return r;
    }

    // ---------------------------------------------------------------- grading

    private List<String> cleanSelected(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw != null) {
            for (String s : raw) {
                if (s != null && !s.trim().isEmpty()) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    /**
     * Returns TRUE/FALSE, or null when the question has no stored answer to grade against.
     * Types: 1 choices, 2 blanks, 3 self check / true-false (text comparison), 4 multi-select.
     */
    private Boolean grade(Question q, List<String> selected) {
        int type = q.getQuestionType();
        if (type == 2 || type == 3) {
            String expected = q.getCorrectAnswer();
            if (expected == null || expected.isBlank()) {
                return null;
            }
            return normalizeText(expected).equals(normalizeText(String.join(",", selected)));
        }

        if (type == 4) {
            String expectedRaw = (q.getCorrectAnswers() != null && !q.getCorrectAnswers().isBlank())
                    ? q.getCorrectAnswers() : q.getCorrectAnswer();
            if (expectedRaw == null || expectedRaw.isBlank()) {
                return null;
            }
            Set<String> expected = new LinkedHashSet<>();
            for (String part : expectedRaw.split(",")) {
                if (!part.isBlank()) {
                    expected.add(toLetter(q, part));
                }
            }
            Set<String> actual = new LinkedHashSet<>();
            for (String s : selected) {
                String asOption = optionLetterForText(q, s);
                if (asOption != null) {
                    actual.add(asOption);
                } else {
                    for (String part : s.split(",")) {
                        if (!part.isBlank()) {
                            actual.add(toLetter(q, part));
                        }
                    }
                }
            }
            return expected.equals(actual);
        }

        String expectedRaw = q.getCorrectAnswer();
        if (expectedRaw == null || expectedRaw.isBlank()) {
            return null;
        }
        Set<String> actual = new LinkedHashSet<>();
        for (String s : selected) {
            actual.add(toLetter(q, s));
        }
        return actual.size() == 1 && actual.contains(toLetter(q, expectedRaw));
    }

    /** Maps a value to an option letter (A-F) if it is one or matches an option's text; else upper-cased text. */
    private String toLetter(Question q, String value) {
        String v = value.trim();
        if (v.length() == 1 && "ABCDEF".contains(v.toUpperCase(Locale.ROOT))) {
            return v.toUpperCase(Locale.ROOT);
        }
        String letter = optionLetterForText(q, v);
        return letter != null ? letter : normalizeText(v).toUpperCase(Locale.ROOT);
    }

    private String optionLetterForText(Question q, String text) {
        String t = normalizeText(text);
        String[] options = { q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getOptionE(), q.getOptionF() };
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && !options[i].isBlank() && normalizeText(options[i]).equals(t)) {
                return String.valueOf((char) ('A' + i));
            }
        }
        return null;
    }

    private String normalizeText(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
