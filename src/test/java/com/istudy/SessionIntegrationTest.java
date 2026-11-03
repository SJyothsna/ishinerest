package com.istudy;

import static com.istudy.learning.LearningModels.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.istudy.identity.LocalAuthService;
import com.istudy.learning.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest(
  classes = StudyApplication.class,
  properties = {
    "spring.datasource.url=${test.database.url:jdbc:h2:mem:learning;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${test.database.user:sa}",
    "spring.datasource.password=${test.database.password:${ISTUDY_DB_PASSWORD:}}",
    "spring.flyway.locations=${test.migration.locations:classpath:db/h2}",
    "spring.jpa.hibernate.ddl-auto=none",
  }
)
class SessionIntegrationTest {

  @Autowired
  JdbcTemplate db;

  @Autowired
  SessionService sessions;

  @Autowired
  LocalAuthService auth;

  @Autowired
  ObjectMapper json;

  @Autowired
  org.springframework.test.web.servlet.MockMvc mvc;

  String authToken;
  UUID student, offering, chapter, section, question;

  @Test
  void browserPreflightAllowsConfiguredFrontendAndRejectsOtherOrigins()
    throws Exception {
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
          "/api/v1/auth/login"
        )
          .header("Origin", "http://localhost:3000")
          .header("Access-Control-Request-Method", "POST")
          .header("Access-Control-Request-Headers", "content-type")
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
          .string("Access-Control-Allow-Origin", "http://localhost:3000")
      );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
          "/api/v1/auth/login"
        )
          .header("Origin", "https://untrusted.example")
          .header("Access-Control-Request-Method", "POST")
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isForbidden()
      );
  }

  @BeforeEach
  void fixture() {
    String suffix = UUID.randomUUID().toString();
    var account = auth.signup(
      "Learner",
      suffix + "@example.test",
      "test-password",
      "STUDENT"
    );
    student = (UUID) account.get("studentId");
    authToken = account.get("token").toString();
    UUID curriculum = UUID.randomUUID(), level = UUID.randomUUID(), subject =
      UUID.randomUUID();
    offering = UUID.randomUUID();
    chapter = UUID.randomUUID();
    section = UUID.randomUUID();
    question = UUID.randomUUID();
    db.update(
      "insert into curricula(curriculum_id,code,name) values(?,?,?)",
      curriculum,
      suffix,
      "Curriculum"
    );
    db.update(
      "insert into academic_levels(level_id,code,name) values(?,?,?)",
      level,
      suffix,
      "Year"
    );
    db.update(
      "insert into subjects(subject_id,code,name) values(?,?,?)",
      subject,
      suffix,
      "Subject"
    );
    db.update(
      "insert into subject_offerings(offering_id,curriculum_id,level_id,subject_id,syllabus_version,status) values(?,?,?,?,?,'PUBLISHED')",
      offering,
      curriculum,
      level,
      subject,
      "v1"
    );
    db.update(
      "insert into chapters(chapter_id,offering_id,code,title) values(?,?,?,?)",
      chapter,
      offering,
      "ch1",
      "Chapter"
    );
    db.update(
      "insert into sections(section_id,chapter_id,code,title) values(?,?,?,?)",
      section,
      chapter,
      "sec1",
      "Topic"
    );
    db.update(
      "insert into questions(question_id,question_text,correct_answer,difficulty_level,approval_status) values(?,'2+2','A','FOUNDATION','PUBLISHED')",
      question
    );
    db.update(
      "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,1,?)",
      question,
      "{\"prompt\":\"2+2\",\"type\":\"single\",\"options\":[{\"id\":\"A\",\"text\":\"4\"}],\"correctAnswer\":\"A\",\"explanation\":\"Two plus two equals four.\"}"
    );
    db.update(
      "insert into question_placements(question_id,section_id,usage_type) values(?,?,'BOTH')",
      question,
      section
    );
    db.update(
      "insert into student_subjects(student_id,offering_id) values(?,?)",
      student,
      offering
    );
  }

  CreateSession config() {
    return new CreateSession(
      offering,
      chapter,
      List.of(),
      "PRACTICE",
      "QUICK",
      "CHAPTER_ALL",
      "ALL",
      1,
      null
    );
  }

  UUID id(Map<String, Object> response) {
    return (UUID) response.get("sessionId");
  }

  @SuppressWarnings("unchecked")
  UUID sq(Map<String, Object> response) {
    return UUID.fromString(
      ((List<com.fasterxml.jackson.databind.node.ObjectNode>) response.get(
          "questions"
        )).get(0)
        .get("sessionQuestionId")
        .asText()
    );
  }

  @Test
  void initialResponseDoesNotExposeAnswersAndRetryIsIdempotent() {
    var session = sessions.create(student, null, config());
    String initial = session.toString();
    assertFalse(initial.contains("correctAnswer"));
    assertFalse(initial.contains("explanation"));
    var answer = new Answer(sq(session), json.getNodeFactory().textNode("A"));
    var submit = new Submit(
      (UUID) session.get("submissionToken"),
      List.of(answer)
    );
    var result = sessions.submit(id(session), student, null, submit);
    var retry = sessions.submit(id(session), student, null, submit);
    result.remove("serverTime");
    retry.remove("serverTime");
    assertEquals(result, retry);
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from question_attempts where student_id=?",
        Integer.class,
        student
      )
    );
  }

  @Test
  void ownershipAndExplicitScopeAreEnforced() {
    var session = sessions.create(student, null, config());
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () -> sessions.read(id(session), UUID.randomUUID(), null)
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.create(
          student,
          null,
          new CreateSession(
            offering,
            chapter,
            List.of(UUID.randomUUID()),
            "PRACTICE",
            "QUICK",
            "SELECTED_SECTIONS",
            "ALL",
            1,
            null
          )
        )
    );
  }

  @Test
  void historicalGradeUsesFrozenRevision() {
    var session = sessions.create(student, null, config());
    db.update(
      "update questions set correct_answer='B',current_revision=2 where question_id=?",
      question
    );
    db.update(
      "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,2,?)",
      question,
      "{\"prompt\":\"New revision\",\"type\":\"single\",\"correctAnswer\":\"B\"}"
    );
    var result = sessions.submit(
      id(session),
      student,
      null,
      new Submit(
        (UUID) session.get("submissionToken"),
        List.of(new Answer(sq(session), json.getNodeFactory().textNode("A")))
      )
    );
    assertTrue(result.toString().contains("\"correct\":1"));
  }

  @Test
  void fixedDifficultyIsNeverBroadened() {
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.create(
          student,
          null,
          new CreateSession(
            offering,
            chapter,
            List.of(),
            "PRACTICE",
            "QUICK",
            "CHAPTER_ALL",
            "ADVANCED",
            1,
            null
          )
        )
    );
  }

  @Test
  void testsCannotRevealImmediateCorrectness() {
    var session = sessions.create(
      student,
      null,
      new CreateSession(
        offering,
        chapter,
        List.of(),
        "TEST",
        "QUICK",
        "CHAPTER_ALL",
        "ALL",
        1,
        null
      )
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.check(
          id(session),
          student,
          new Answer(sq(session), json.getNodeFactory().textNode("A"))
        )
    );
  }

  @Test
  void guestReviewRequiresClaimAndDoesNotCreateAuthoritativeAttempts() {
    UUID guest = UUID.randomUUID();
    db.update(
      "insert into guest_principals(guest_id,secret_hash,expires_at) values(?,?,?)",
      guest,
      UUID.randomUUID().toString(),
      java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(3600))
    );
    var session = sessions.create(
      null,
      guest,
      new CreateSession(
        offering,
        null,
        List.of(),
        "SAMPLE",
        "QUICK",
        "SUBJECT_ALL",
        "ALL",
        1,
        null
      )
    );
    var submitted = sessions.submit(
      id(session),
      null,
      guest,
      new Submit(
        (UUID) session.get("submissionToken"),
        List.of(new Answer(sq(session), json.getNodeFactory().textNode("A")))
      )
    );
    var basic = json.valueToTree(submitted);
    assertFalse(basic.path("result").has("gradedAnswers"));
    assertFalse(basic.path("questions").get(0).has("correctAnswer"));
    var full = json.valueToTree(sessions.claim(id(session), guest, student));
    assertTrue(full.path("questions").get(0).has("correctAnswer"));
    assertFalse(
      json
        .valueToTree(sessions.read(id(session), null, guest))
        .path("questions")
        .get(0)
        .has("correctAnswer")
    );
    assertEquals(
      0,
      db.queryForObject(
        "select count(*) from question_attempts where student_id=?",
        Integer.class,
        student
      )
    );
  }

  @Test
  void repeatedPracticeChangesAccuracyWithoutIncreasingDistinctCoverage() {
    var first = sessions.create(student, null, config());
    sessions.submit(
      id(first),
      student,
      null,
      new Submit(
        (UUID) first.get("submissionToken"),
        List.of(new Answer(sq(first), json.getNodeFactory().textNode("B")))
      )
    );
    var second = sessions.create(student, null, config());
    sessions.submit(
      id(second),
      student,
      null,
      new Submit(
        (UUID) second.get("submissionToken"),
        List.of(new Answer(sq(second), json.getNodeFactory().textNode("A")))
      )
    );
    var insights = new InsightsController(
      db,
      new com.istudy.identity.CurrentStudent()
    );
    var progress = insights.progressFor(student, offering, null);
    assertEquals(1, progress.get("distinctQuestionsAttempted"));
    assertEquals(2, progress.get("totalAttempts"));
    assertEquals(50L, ((Number) progress.get("accuracyPercent")).longValue());
  }

  @Test
  void apiDoesNotTrustAnArbitraryStudentIdentifier() throws Exception {
    var session = sessions.create(student, null, config());
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/practice-sessions/" + id(session)
        )
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isUnauthorized()
      );
    var other = auth.signup(
      "Other",
      UUID.randomUUID() + "@example.test",
      "test-password",
      "STUDENT"
    );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/practice-sessions/" + id(session)
        ).header("Authorization", "Bearer " + other.get("token"))
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isNotFound()
      );
  }

  @Autowired
  com.istudy.content.QuestionBankValidator bankValidator;

  @Autowired
  com.istudy.content.QuestionBankController bankController;

  @Autowired
  SetProgressionService progression;

  @org.junit.jupiter.api.AfterEach
  void clearIdentity() {
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  private UUID textbook() {
    db.update(
      "update app_users set content_admin=true where user_id=?",
      student
    );
    org.springframework.security.core.context.SecurityContextHolder.getContext()
      .setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
          student,
          null,
          List.of()
        )
      );
    UUID asset = UUID.randomUUID(), doc = UUID.randomUUID();
    db.update(
      "insert into media_assets(asset_id,bucket_name,object_key,content_type) values(?,'test',?,'application/pdf')",
      asset,
      asset.toString()
    );
    db.update(
      "insert into content_documents(document_id,asset_id,document_type,title,licence_status) values(?,?,'TEXTBOOK','Source textbook','PERMITTED')",
      doc,
      asset
    );
    return doc;
  }

  private com.fasterxml.jackson.databind.node.ObjectNode manifest(int set)
    throws Exception {
    return (
      (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(
        """
        {"formatVersion":"1.0","questionSet":1,"coverageScope":"TOPICS","chapters":[{"code":"ch1","title":"Chapter","topics":[{"code":"sec1","title":"Topic","expectedCounts":{"EASY":1,"MEDIUM":0,"HARD":0},"questions":[{"externalId":"BANK-001","type":"single","difficulty":"EASY","prompt":"What is five plus two?","options":[{"id":"A","text":"7"},{"id":"B","text":"8"}],"correctAnswer":"A","explanation":"Adding two to five gives seven.","sourcePage":1}]}]}]}
        """
      )
    ).put("questionSet", set);
  }

  @Test
  void manualBankValidationCatchesCountShortagesAndCopiedSetOneQuestions()
    throws Exception {
    var bank = manifest(2);
    ((com.fasterxml.jackson.databind.node.ObjectNode) bank
        .path("chapters")
        .get(0)
        .path("topics")
        .get(0)
        .path("expectedCounts")).put("EASY", 2);
    assertEquals(false, bankValidator.validate(offering, bank).get("valid"));
    bank = manifest(2);
    var q = (com.fasterxml.jackson.databind.node.ObjectNode) bank
      .path("chapters")
      .get(0)
      .path("topics")
      .get(0)
      .path("questions")
      .get(0);
    q.put("prompt", "2+2");
    q.putArray("options").addObject().put("id", "A").put("text", "4");
    assertEquals(false, bankValidator.validate(offering, bank).get("valid"));
  }

  @Test
  void setTwoDraftsStayHiddenUntilReviewAndThenPrecedeRepeats()
    throws Exception {
    UUID doc = textbook();
    var bank = manifest(2);
    var request = new com.istudy.content.QuestionBankController.Bank(
      offering,
      doc,
      bank
    );
    UUID imported = (UUID) bankController.upload(request).get("importId");
    var first = sessions.create(student, null, config());
    assertEquals(
      "2+2",
      json.valueToTree(first).path("questions").get(0).path("prompt").asText()
    );
    var draftHidden = sessions.create(student, null, config());
    assertEquals(
      "2+2",
      json
        .valueToTree(draftHidden)
        .path("questions")
        .get(0)
        .path("prompt")
        .asText()
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        bankController.publish(
          imported,
          new com.istudy.content.QuestionBankController.Review(false)
        )
    );
    bankController.publish(
      imported,
      new com.istudy.content.QuestionBankController.Review(true)
    );
    var fresh = sessions.create(student, null, config());
    assertEquals(
      "What is five plus two?",
      json.valueToTree(fresh).path("questions").get(0).path("prompt").asText()
    );
    assertEquals(0L, ((Number) fresh.get("repeatedCount")).longValue());
    assertEquals(imported, bankController.upload(request).get("importId"));
    bankController.publish(
      imported,
      new com.istudy.content.QuestionBankController.Review(true)
    );
    assertEquals(
      2,
      db.queryForObject(
        "select count(distinct p.question_id) from question_placements p join sections s on s.section_id=p.section_id where s.chapter_id=?",
        Integer.class,
        chapter
      )
    );
  }

  @Test
  void preparationTriggersAtEightyPercentExposureWithoutInventingProgress() {
    for (int i = 0; i < 4; i++) {
      UUID q = UUID.randomUUID();
      db.update(
        "insert into questions(question_id,question_text,difficulty_level,approval_status) values(?,?,'FOUNDATION','PUBLISHED')",
        q,
        "Question " + i
      );
      db.update(
        "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,1,?)",
        q,
        "{\"prompt\":\"Question " +
        i +
        "\",\"type\":\"single\",\"correctAnswer\":\"A\"}"
      );
      db.update(
        "insert into question_placements(question_id,section_id,usage_type) values(?,?,'BOTH')",
        q,
        section
      );
    }
    sessions.create(
      student,
      null,
      new CreateSession(
        offering,
        chapter,
        List.of(),
        "PRACTICE",
        "QUICK",
        "CHAPTER_ALL",
        "ALL",
        4,
        null
      )
    );
    assertEquals(
      80,
      progression.status(student, offering, chapter).get("set1ExposurePercent")
    );
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from content_jobs where chapter_id=?",
        Integer.class,
        chapter
      )
    );
    sessions.create(student, null, config());
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from content_jobs where chapter_id=?",
        Integer.class,
        chapter
      )
    );
    assertEquals(
      0,
      db.queryForObject(
        "select count(*) from question_attempts where student_id=?",
        Integer.class,
        student
      )
    );
  }

  @Test
  void expiredTimedSessionUsesPreviouslySavedDrafts() {
    var session = sessions.create(
      student,
      null,
      new CreateSession(
        offering,
        chapter,
        List.of(),
        "TEST",
        "QUICK",
        "CHAPTER_ALL",
        "ALL",
        1,
        60
      )
    );
    sessions.drafts(
      id(session),
      student,
      null,
      List.of(new Answer(sq(session), json.getNodeFactory().textNode("A")))
    );
    db.update(
      "update practice_sessions set expires_at=? where session_id=?",
      java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),
      id(session)
    );
    var result = sessions.submit(
      id(session),
      student,
      null,
      new Submit(
        (UUID) session.get("submissionToken"),
        List.of(new Answer(sq(session), json.getNodeFactory().textNode("B")))
      )
    );
    assertEquals(
      1,
      json.valueToTree(result).path("result").path("correct").asInt()
    );
  }

  @Test
  @org.springframework.transaction.annotation.Transactional
  void irelandStarterIsIdempotentAndReviewedBankRetiresAllMockQuestions()
    throws Exception {
    UUID doc = textbook();
    var seed = new com.istudy.bootstrap.StarterContent(db, json);
    seed.run(null);
    seed.run(null);
    UUID starter = com.istudy.bootstrap.StarterContent.id("offering");
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from subject_offerings where status='PUBLISHED'",
        Integer.class
      )
    );
    assertEquals(
      5,
      db.queryForObject(
        "select count(*) from chapters where offering_id=?",
        Integer.class,
        starter
      )
    );
    assertEquals(
      30,
      db.queryForObject(
        "select count(*) from question_placements p join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and p.is_starter and p.active",
        Integer.class,
        starter
      )
    );
    assertEquals(
      0,
      db.queryForObject(
        "select count(*) from question_attempts where student_id=?",
        Integer.class,
        student
      )
    );
    var bank = manifest(1);
    bank.put("coverageScope", "SUBJECT");
    var request = new com.istudy.content.QuestionBankController.Bank(
      starter,
      doc,
      bank
    );
    UUID imported = (UUID) bankController.upload(request).get("importId");
    bankController.publish(
      imported,
      new com.istudy.content.QuestionBankController.Review(true)
    );
    assertFalse(
      db.queryForObject(
        "select is_starter from subject_offerings where offering_id=?",
        Boolean.class,
        starter
      )
    );
    assertEquals(
      0,
      db.queryForObject(
        "select count(*) from question_placements p join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and p.is_starter and p.active",
        Integer.class,
        starter
      )
    );
    assertEquals(
      0,
      db.queryForObject(
        "select count(*) from study_notes where offering_id=? and is_starter",
        Integer.class,
        starter
      )
    );
    seed.run(null);
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from chapters where offering_id=? and status='ACTIVE'",
        Integer.class,
        starter
      )
    );
    var live = sessions.create(
      student,
      null,
      new CreateSession(
        starter,
        null,
        List.of(),
        "PRACTICE",
        "QUICK",
        "SUBJECT_ALL",
        "ALL",
        5,
        null
      )
    );
    assertEquals(1, ((Number) live.get("selectedCount")).intValue());
    assertFalse((Boolean) live.get("isStarter"));
  }

  @Test
  void flagsAreRestoredWhenOwnedSessionResumes() {
    var session = sessions.create(student, null, config());
    db.update(
      "insert into student_question_flags(student_id,question_id) values(?,?)",
      student,
      question
    );
    var restored = json.valueToTree(sessions.read(id(session), student, null));
    assertTrue(restored.path("questions").get(0).path("flagged").asBoolean());
  }

  @Autowired
  com.istudy.identity.AccountRecoveryController recovery;

  @Test
  void repeatingAConsumedEmailVerificationLinkIsSafe() {
    String raw = LocalAuthService.secret();
    db.update(
      "insert into account_action_tokens(token_hash,user_id,purpose,expires_at) values(?,?,'VERIFY',?)",
      LocalAuthService.hash(raw),
      student,
      java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(3600))
    );
    recovery.verify(
      new com.istudy.identity.AccountRecoveryController.Token(raw)
    );
    var repeated = recovery.verify(
      new com.istudy.identity.AccountRecoveryController.Token(raw)
    );
    assertEquals(true, repeated.get("alreadyVerified"));
    assertTrue(
      db.queryForObject(
        "select email_verified from app_users where user_id=?",
        Boolean.class,
        student
      )
    );
  }

  @Test
  @org.springframework.transaction.annotation.Transactional
  void starterMultipleChoiceUpgradeKeepsFrozenTextSessionsAndIsIdempotent()
    throws Exception {
    var seed = new com.istudy.bootstrap.StarterContent(db, json);
    seed.run(null);
    var course = json.readTree(
      new org.springframework.core.io.ClassPathResource(
        "starter/ireland-leaving-cert-maths.json"
      ).getInputStream()
    );
    int checked = 0;
    var grader = new Grader();
    for (var c : course.path("chapters")) for (var t : c.path(
      "topics"
    )) for (var q : t.path("questions")) {
      assertEquals("single", q.path("type").asText());
      assertEquals(4, q.path("options").size());
      var labels = new HashSet<String>();
      int correctCount = 0;
      for (var option : q.path("options")) {
        assertTrue(labels.add(option.path("text").asText()));
        if (grader.correct(q, option.path("id"))) correctCount++;
      }
      assertEquals(1, correctCount);
      checked++;
    }
    assertEquals(30, checked);
    UUID starter = com.istudy.bootstrap.StarterContent.id(
      "offering"
    ), questionId = com.istudy.bootstrap.StarterContent.id(
      "question:starter-probability-1"
    );
    var old = (com.fasterxml.jackson.databind.node.ObjectNode) course
      .path("chapters")
      .get(0)
      .path("topics")
      .get(0)
      .path("questions")
      .get(0)
      .deepCopy();
    old.put("type", "text").put("correctAnswer", "1/6");
    old.remove("options");
    db.update(
      "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,2,?)",
      questionId,
      old.toString()
    );
    db.update(
      "update questions set question_type='TEXT',correct_answer='1/6',current_revision=2 where question_id=?",
      questionId
    );
    var request = new CreateSession(
      starter,
      com.istudy.bootstrap.StarterContent.id("chapter:statistics-probability"),
      List.of(com.istudy.bootstrap.StarterContent.id("topic:probability")),
      "PRACTICE",
      "QUICK",
      "SELECTED_SECTIONS",
      "FOUNDATION",
      1,
      null
    );
    var frozen = sessions.create(student, null, request);
    assertEquals(
      "text",
      json.valueToTree(frozen).path("questions").get(0).path("type").asText()
    );
    seed.run(null);
    seed.run(null);
    assertEquals(
      3,
      db.queryForObject(
        "select current_revision from questions where question_id=?",
        Integer.class,
        questionId
      )
    );
    assertEquals(
      "text",
      json
        .valueToTree(sessions.read(id(frozen), student, null))
        .path("questions")
        .get(0)
        .path("type")
        .asText()
    );
    var submitted = sessions.submit(
      id(frozen),
      student,
      null,
      new Submit(
        (UUID) frozen.get("submissionToken"),
        List.of(new Answer(sq(frozen), json.getNodeFactory().textNode("1/6")))
      )
    );
    assertEquals(
      1,
      json.valueToTree(submitted).path("result").path("correct").asInt()
    );
    var fresh = json
      .valueToTree(sessions.create(student, null, request))
      .path("questions")
      .get(0);
    assertEquals("single", fresh.path("type").asText());
    assertEquals(4, fresh.path("options").size());
    assertFalse(fresh.has("correctAnswer"));
  }

  @Test
  void practiceCheckingReturnsAnswerKeyOnlyAfterOwnedCheck() {
    var session = sessions.create(student, null, config());
    assertFalse(
      json.valueToTree(session).path("questions").get(0).has("correctAnswer")
    );
    var checked = sessions.check(
      id(session),
      student,
      new Answer(sq(session), json.getNodeFactory().textNode("B"))
    );
    assertEquals(false, checked.get("correct"));
    assertEquals("A", json.valueToTree(checked).path("correctAnswer").asText());
    assertEquals("Two plus two equals four.", checked.get("explanation"));
  }

  @Test
  void originalQuestionSourcesPublishWithoutInventedPagesAndCannotBypassTextbookProvenance()
    throws Exception {
    UUID pdf = textbook();
    var manifest = manifest(1);
    manifest.put("sourceType", "ORIGINAL");
    var q = (com.fasterxml.jackson.databind.node.ObjectNode) manifest
      .path("chapters")
      .get(0)
      .path("topics")
      .get(0)
      .path("questions")
      .get(0);
    q.remove("sourcePage");
    q.put("examLevel", "HIGHER");
    var pdfRequest = new com.istudy.content.QuestionBankController.Bank(
      offering,
      pdf,
      manifest
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () -> bankController.upload(pdfRequest)
    );
    var file = new org.springframework.mock.web.MockMultipartFile(
      "file",
      "original.json",
      "application/json",
      "{\"questions\":[{\"question\":\"Five plus two\"}]}".getBytes(
          java.nio.charset.StandardCharsets.UTF_8
        )
    );
    UUID source = (UUID) bankController
      .book(file, "Original maths", true, "ORIGINAL")
      .get("documentId");
    var request = new com.istudy.content.QuestionBankController.Bank(
      offering,
      source,
      manifest
    );
    assertEquals(true, bankController.validate(request).get("valid"));
    UUID imported = (UUID) bankController.upload(request).get("importId");
    bankController.publish(
      imported,
      new com.istudy.content.QuestionBankController.Review(true)
    );
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from question_sources where document_id=? and page_number is null",
        Integer.class,
        source
      )
    );
    assertTrue(
      bankController
        .banks()
        .stream()
        .anyMatch(
          b ->
            b.get("importId").equals(imported) &&
            b.get("sourceTitle").equals("Original maths")
        )
    );
    manifest.put("sourceType", "TEXTBOOK");
    assertEquals(
      false,
      bankValidator.validate(offering, manifest).get("valid")
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        bankController.upload(
          new com.istudy.content.QuestionBankController.Bank(
            offering,
            source,
            manifest
          )
        )
    );
  }

  @Test
  void originalSourceUploadRejectsMalformedJsonAndNonAdministrators()
    throws Exception {
    textbook();
    var invalid = new org.springframework.mock.web.MockMultipartFile(
      "file",
      "bad.json",
      "application/json",
      "not json".getBytes()
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () -> bankController.book(invalid, "Original", true, "ORIGINAL")
    );
    var file = new org.springframework.mock.web.MockMultipartFile(
      "file",
      "original.json",
      "application/json",
      "{\"questions\":[]}".getBytes()
    );
    UUID asset = (UUID) bankController
      .book(file, "Original", true, "ORIGINAL")
      .get("assetId");
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/assets/" + asset + "/content"
        ).header("Authorization", "Bearer " + authToken)
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      );
    db.update(
      "update app_users set content_admin=false where user_id=?",
      student
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () -> bankController.book(file, "Original", true, "ORIGINAL")
    );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/assets/" + asset + "/content"
        ).header("Authorization", "Bearer " + authToken)
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isNotFound()
      );
  }

  @Test
  void explanationVideosAreRelevantActiveAndOnlyShownWithFeedback() {
    UUID video = UUID.randomUUID(), hidden = UUID.randomUUID();
    db.update(
      "insert into learning_videos(video_id,youtube_video_id,title) values(?,?,'Related concept')",
      video,
      "abc" + UUID.randomUUID().toString().substring(0, 8)
    );
    db.update(
      "insert into learning_videos(video_id,youtube_video_id,title,status) values(?,?,'Hidden concept','HIDDEN')",
      hidden,
      "xyz" + UUID.randomUUID().toString().substring(0, 8)
    );
    db.update(
      "insert into video_sections(video_id,section_id) values(?,?)",
      video,
      section
    );
    db.update(
      "insert into video_sections(video_id,section_id) values(?,?)",
      hidden,
      section
    );
    var session = sessions.create(student, null, config());
    assertFalse(
      json.valueToTree(session).path("questions").get(0).has("videos")
    );
    var checked = sessions.check(
      id(session),
      student,
      new Answer(sq(session), json.getNodeFactory().textNode("A"))
    );
    assertEquals(1, json.valueToTree(checked).path("videos").size());
    assertEquals(
      "Related concept",
      json.valueToTree(checked).path("videos").get(0).path("title").asText()
    );
  }

  @Test
  void samplesRandomiseWithinChapterSortByDifficultyAndFreezeOnResume()
    throws Exception {
    UUID guest = UUID.randomUUID(), focused = UUID.randomUUID(), topic =
      UUID.randomUUID();
    db.update(
      "insert into guest_principals(guest_id,secret_hash,expires_at) values(?,?,?)",
      guest,
      UUID.randomUUID().toString(),
      java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(3600))
    );
    db.update(
      "insert into chapters(chapter_id,offering_id,code,title) values(?,?,'sample-focus','Sample chapter')",
      focused,
      offering
    );
    db.update(
      "insert into sections(section_id,chapter_id,code,title) values(?,?,'sample-topic','Topic')",
      topic,
      focused
    );
    var expected = new HashSet<UUID>();
    for (int i = 0; i < 12; i++) {
      UUID q = UUID.randomUUID();
      expected.add(q);
      String difficulty = List.of("FOUNDATION", "INTERMEDIATE", "ADVANCED").get(
        i % 3
      );
      db.update(
        "insert into questions(question_id,question_text,difficulty_level,question_type,approval_status) values(?,?,?,'SINGLE_CHOICE','PUBLISHED')",
        q,
        "Sample question " + i,
        difficulty
      );
      db.update(
        "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,1,?)",
        q,
        json
          .createObjectNode()
          .put("prompt", "Sample question " + i)
          .put("difficulty", difficulty)
          .put("type", "single")
          .put("correctAnswer", "A")
          .put("explanation", "Verified")
          .set(
            "options",
            json.readTree(
              "[{\"id\":\"A\",\"text\":\"Correct\"},{\"id\":\"B\",\"text\":\"Other\"}]"
            )
          )
          .toString()
      );
      db.update(
        "insert into question_placements(question_id,section_id,usage_type) values(?,?,'BOTH')",
        q,
        topic
      );
    }
    var request = new CreateSession(
      offering,
      focused,
      List.of(),
      "SAMPLE",
      "QUICK",
      "CHAPTER_ALL",
      "ALL",
      7,
      null
    );
    var draws = new HashSet<String>();
    for (int run = 0; run < 8; run++) {
      var session = sessions.create(null, guest, request);
      var qs = json.valueToTree(session).path("questions");
      assertEquals(7, qs.size());
      var ids = new HashSet<String>();
      int previous = -1;
      for (var q : qs) {
        assertTrue(
          expected.contains(UUID.fromString(q.path("questionId").asText()))
        );
        ids.add(q.path("questionId").asText());
        int rank = List.of("FOUNDATION", "INTERMEDIATE", "ADVANCED").indexOf(
          q.path("difficulty").asText()
        );
        assertTrue(rank >= previous);
        previous = rank;
      }
      assertEquals(7, ids.size());
      draws.add(new TreeSet<>(ids).toString());
      assertEquals(
        qs,
        json
          .valueToTree(sessions.read(id(session), null, guest))
          .path("questions")
      );
    }
    assertTrue(
      draws.size() > 1,
      "New sessions should draw different random subsets"
    );
    var shortSession = sessions.create(
      null,
      guest,
      new CreateSession(
        offering,
        chapter,
        List.of(),
        "SAMPLE",
        "QUICK",
        "CHAPTER_ALL",
        "ALL",
        7,
        null
      )
    );
    assertEquals(1, ((Number) shortSession.get("selectedCount")).intValue());
    assertEquals(true, shortSession.get("shortage"));
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.create(
          null,
          guest,
          new CreateSession(
            offering,
            UUID.randomUUID(),
            List.of(),
            "SAMPLE",
            "QUICK",
            "CHAPTER_ALL",
            "ALL",
            7,
            null
          )
        )
    );
  }

  @Test
  void firstPracticeCheckSurvivesResumeAndCorrectionsRemainSeparateFromIndependentEvidence() {
    var session = sessions.create(student, null, config());
    var first = sessions.check(
      id(session),
      student,
      new Answer(sq(session), json.getNodeFactory().textNode("B"))
    );
    assertEquals(false, first.get("correct"));
    var again = sessions.check(
      id(session),
      student,
      new Answer(sq(session), json.getNodeFactory().textNode("A"))
    );
    assertEquals(first, again);
    var resumed = json.valueToTree(sessions.read(id(session), student, null));
    assertEquals(
      "B",
      resumed
        .path("questions")
        .get(0)
        .path("feedback")
        .path("firstResponse")
        .asText()
    );
    var result = json.valueToTree(
      sessions.submit(
        id(session),
        student,
        null,
        new Submit(
          (UUID) session.get("submissionToken"),
          List.of(new Answer(sq(session), json.getNodeFactory().textNode("A")))
        )
      )
    );
    assertEquals(1, result.path("result").path("correct").asInt());
    assertEquals(
      1,
      result.path("result").path("correctedAfterExplanation").asInt()
    );
    assertEquals(0, result.path("result").path("unanswered").asInt());
    assertTrue(result.path("questions").get(0).path("correct").asBoolean());
    assertFalse(
      result.path("questions").get(0).path("firstCheckCorrect").asBoolean()
    );
    assertEquals(
      0,
      db.queryForObject(
        "select correct_attempts from student_question_stats where student_id=? and question_id=?",
        Integer.class,
        student,
        question
      )
    );
    assertEquals(
      0,
      new InsightsController(db, new com.istudy.identity.CurrentStudent())
        .progressFor(student, offering, chapter)
        .get("correctAttempts")
    );
  }

  @Test
  void guestFlagsAndReportsAreOwnedIdempotentAndPreservedWithoutAnswerDisclosure() {
    UUID guest = UUID.randomUUID();
    db.update(
      "insert into guest_principals(guest_id,secret_hash,expires_at) values(?,?,?)",
      guest,
      UUID.randomUUID().toString(),
      java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(3600))
    );
    var sample = sessions.create(
      null,
      guest,
      new CreateSession(
        offering,
        chapter,
        List.of(),
        "SAMPLE",
        "QUICK",
        "CHAPTER_ALL",
        "ALL",
        1,
        null
      )
    );
    sessions.reviewFlag(id(sample), null, guest, sq(sample), true);
    assertTrue(
      json
        .valueToTree(sessions.read(id(sample), null, guest))
        .path("questions")
        .get(0)
        .path("flagged")
        .asBoolean()
    );
    sessions.reportIssue(
      id(sample),
      null,
      guest,
      sq(sample),
      "UNCLEAR_WORDING"
    );
    sessions.reportIssue(
      id(sample),
      null,
      guest,
      sq(sample),
      "UNCLEAR_WORDING"
    );
    assertEquals(
      1,
      db.queryForObject(
        "select count(*) from question_issue_reports where session_question_id=?",
        Integer.class,
        sq(sample)
      )
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.reviewFlag(
          id(sample),
          null,
          UUID.randomUUID(),
          sq(sample),
          false
        )
    );
    assertThrows(
      org.springframework.web.server.ResponseStatusException.class,
      () ->
        sessions.reportIssue(
          id(sample),
          null,
          guest,
          UUID.randomUUID(),
          "UNCLEAR_WORDING"
        )
    );
    assertFalse(
      json
        .valueToTree(sessions.read(id(sample), null, guest))
        .path("questions")
        .get(0)
        .has("feedback")
    );
    sessions.reviewFlag(id(sample), null, guest, sq(sample), false);
    assertFalse(
      json
        .valueToTree(sessions.read(id(sample), null, guest))
        .path("questions")
        .get(0)
        .path("flagged")
        .asBoolean()
    );
  }

  @Test
  void reportRoutesEnforceOwnershipAndAdminReviewPermission() throws Exception {
    var session = sessions.create(student, null, config());
    String path =
      "/api/v1/practice-sessions/" + id(session) + "/questions/" + sq(session);
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
          path + "/flag"
        )
          .header("Authorization", "Bearer " + authToken)
          .contentType("application/json")
          .content("{\"flagged\":true}")
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
          path + "/issues"
        )
          .header("Authorization", "Bearer " + authToken)
          .contentType("application/json")
          .content("{\"reason\":\"UNCLEAR_WORDING\"}")
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/content/question-issues"
        ).header("Authorization", "Bearer " + authToken)
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isForbidden()
      );
    db.update(
      "update app_users set content_admin=true where user_id=?",
      student
    );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
          "/api/v1/content/question-issues"
        ).header("Authorization", "Bearer " + authToken)
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
          "$[?(@.sessionQuestionId == '" + sq(session) + "')].reason"
        ).value(org.hamcrest.Matchers.hasItem("UNCLEAR_WORDING"))
      );
    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
          "/api/v1/content/question-issues/" + sq(session)
        )
          .header("Authorization", "Bearer " + authToken)
          .contentType("application/json")
          .content("{\"reason\":\"UNCLEAR_WORDING\",\"reviewed\":true}")
      )
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
          .isOk()
      );
    assertTrue(
      db.queryForObject(
        "select reviewed from question_issue_reports where session_question_id=?",
        Boolean.class,
        sq(session)
      )
    );
  }

  @Test
  void clearingFinalSelectionDoesNotEraseTheFirstCheckedAttempt() {
    var session = sessions.create(student, null, config());
    sessions.check(
      id(session),
      student,
      new Answer(sq(session), json.getNodeFactory().textNode("B"))
    );
    var result = json.valueToTree(
      sessions.submit(
        id(session),
        student,
        null,
        new Submit(
          (UUID) session.get("submissionToken"),
          List.of(new Answer(sq(session), json.getNodeFactory().nullNode()))
        )
      )
    );
    assertEquals(1, result.path("result").path("unanswered").asInt());
    assertTrue(result.path("questions").get(0).path("answer").isNull());
    assertEquals(
      1,
      db.queryForObject(
        "select total_attempts from student_question_stats where student_id=? and question_id=?",
        Integer.class,
        student,
        question
      )
    );
    assertEquals(
      0,
      db.queryForObject(
        "select correct_attempts from student_question_stats where student_id=? and question_id=?",
        Integer.class,
        student,
        question
      )
    );
  }
}
