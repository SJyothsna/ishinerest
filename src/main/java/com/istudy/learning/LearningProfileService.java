package com.istudy.learning;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LearningProfileService {

  private static final int RECENT_FAMILIES = 12;
  private static final int EVIDENCE_DAYS = 90;
  private static final List<String> DIFFICULTIES = List.of(
    "ADVANCED", "INTERMEDIATE", "FOUNDATION"
  );
  private final JdbcTemplate db;

  public LearningProfileService(JdbcTemplate db) {
    this.db = db;
  }

  public enum Status {
    UNASSESSED, DEVELOPING, STRENGTH, NEEDS_PRACTICE
  }

  public record Evidence(
    UUID objectiveId,
    UUID sectionId,
    UUID chapterId,
    String title,
    Status status,
    int evidenceCount,
    int independentCorrect,
    int correctionsAfterFeedback,
    int freshSuccessAfterCorrection,
    boolean recentUnresolvedCorrection,
    String demonstratedDifficulty,
    int availableQuestions,
    int freshQuestions
  ) {
    UUID coverageKey() {
      return objectiveId == null ? sectionId : objectiveId;
    }
  }

  public record Recommendation(
    String title, String reason, UUID offeringId, UUID chapterId, UUID sectionId,
    UUID objectiveId, int evidenceCount
  ) {}

  public record Profile(
    UUID offeringId, int objectiveCount, int objectivesAttempted,
    List<Evidence> strengths, List<Recommendation> recommendations,
    List<Evidence> objectives
  ) {
    boolean hasSufficientHistory() {
      return objectives.stream().anyMatch(e ->
        e.status() == Status.STRENGTH || e.status() == Status.NEEDS_PRACTICE
      );
    }
  }

  private record Target(
    UUID objectiveId, UUID sectionId, UUID chapterId, String title,
    int availableQuestions,
    int freshQuestions
  ) {
    UUID coverageKey() {
      return objectiveId == null ? sectionId : objectiveId;
    }
  }

  private record Attempt(
    UUID coverageKey, UUID sessionId, UUID questionId, String familyCode,
    String difficulty, boolean fresh, boolean correct, boolean corrected,
    OffsetDateTime submittedAt
  ) {
    String familyKey() {
      return familyCode == null ? questionId.toString() : familyCode;
    }
  }

  public Profile profile(UUID student, UUID offering, UUID chapter) {
    var targets = db.query(
      """
      select lo.objective_id,s.section_id,c.chapter_id,coalesce(lo.title,s.title) as title,
      count(distinct q.question_id) as available_questions,
      count(distinct case when not exists (
        select 1 from session_questions sq join practice_sessions ps on ps.session_id=sq.session_id
        where ps.offering_id=c.offering_id and (ps.student_id=? or ps.claimed_by=?)
          and (sq.question_id=q.question_id or (sq.objective_id=p.objective_id and sq.family_code=p.family_code))
      ) then q.question_id else null end) as fresh_questions
      from sections s
      join chapters c on c.chapter_id=s.chapter_id
      join subject_offerings so on so.offering_id=c.offering_id
      left join learning_objectives lo on lo.section_id=s.section_id
      left join question_placements p on p.section_id=s.section_id and p.active
        and ((lo.objective_id is null and p.objective_id is null) or p.objective_id=lo.objective_id)
        and p.usage_type in ('PRACTICE','BOTH','ALL')
      left join questions q on q.question_id=p.question_id and q.approval_status='PUBLISHED'
        and q.visibility='GLOBAL' and q.question_type<>'WORKED_EXAMPLE'
      where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid)
        and so.status='PUBLISHED' and c.status='ACTIVE' and s.status='ACTIVE'
      group by lo.objective_id,s.section_id,c.chapter_id,coalesce(lo.title,s.title)
      order by c.chapter_id,s.section_id,coalesce(lo.title,s.title)
      """,
      (rs, row) -> new Target(
        rs.getObject("objective_id", UUID.class),
        rs.getObject("section_id", UUID.class),
        rs.getObject("chapter_id", UUID.class),
        rs.getString("title"),
        rs.getInt("available_questions"),
        rs.getInt("fresh_questions")
      ),
      student, student, offering, chapter, chapter
    );
    var attempts = db.query(
      """
      select coalesce(sq.objective_id,p.section_id) as coverage_key,
        sq.session_id,sq.question_id,sq.family_code,sq.effective_difficulty,sq.previously_seen,sq.family_previously_seen,
        coalesce(ck.is_correct,a.is_correct) as independent_correct,
        case when ck.is_correct=false and a.is_correct then true else false end as corrected,
        a.submitted_at
      from question_attempts a
      join session_questions sq on sq.session_question_id=a.session_question_id
      join practice_sessions ps on ps.session_id=sq.session_id
      join question_placements p on p.placement_id=sq.placement_id
      join sections s on s.section_id=p.section_id
      left join session_answer_checks ck on ck.session_question_id=sq.session_question_id
      where a.student_id=? and ps.offering_id=? and ps.status='SUBMITTED'
        and a.grading_version<>'self-review-v1' and (?::uuid is null or s.chapter_id=?::uuid)
      order by a.submitted_at,sq.question_position,a.attempt_id
      """,
      (rs, row) -> new Attempt(
        rs.getObject("coverage_key", UUID.class),
        rs.getObject("session_id", UUID.class),
        rs.getObject("question_id", UUID.class),
        rs.getString("family_code"),
        rs.getString("effective_difficulty"),
        Boolean.FALSE.equals(rs.getObject("previously_seen")) &&
          (rs.getString("family_code") == null || Boolean.FALSE.equals(rs.getObject("family_previously_seen"))),
        rs.getBoolean("independent_correct"),
        rs.getBoolean("corrected"),
        rs.getObject("submitted_at", OffsetDateTime.class)
      ),
      student, offering, chapter, chapter
    );
    var byTarget = new HashMap<UUID, List<Attempt>>();
    for (Attempt attempt : attempts) {
      byTarget.computeIfAbsent(attempt.coverageKey(), k -> new ArrayList<>()).add(attempt);
    }
    var cutoff = OffsetDateTime.now().minusDays(EVIDENCE_DAYS);
    var evidence = targets.stream().map(target -> assess(
      target, byTarget.getOrDefault(target.coverageKey(), List.of()), cutoff
    )).filter(e -> e.objectiveId() != null || e.availableQuestions() > 0).toList();
    var recommended = evidence.stream()
      .filter(e -> e.freshQuestions() > 0 && e.status() != Status.STRENGTH)
      .sorted(Comparator.comparingInt(LearningProfileService::priority)
        .thenComparing(Comparator.comparingInt(Evidence::evidenceCount).reversed())
        .thenComparing(Evidence::title))
      .limit(3)
      .map(e -> new Recommendation(
        (e.status() == Status.UNASSESSED ? "Try " : "Practise ") + e.title(),
        reason(e), offering, e.chapterId(), e.sectionId(), e.objectiveId(), e.evidenceCount()
      )).toList();
    return new Profile(
      offering,
      (int) targets.stream().filter(t -> t.objectiveId() != null).count(),
      (int) targets.stream().filter(t -> t.objectiveId() != null && byTarget.containsKey(t.coverageKey())).count(),
      evidence.stream().filter(e -> e.status() == Status.STRENGTH)
        .sorted(Comparator.comparingInt(Evidence::evidenceCount).reversed()).limit(3).toList(),
      recommended,
      evidence
    );
  }

  static int priority(Evidence evidence) {
    if (evidence == null) return 4;
    if (evidence.status() == Status.NEEDS_PRACTICE) return 0;
    if (evidence.recentUnresolvedCorrection()) return 1;
    if (evidence.objectiveId() != null && evidence.status() == Status.UNASSESSED) return 2;
    if (evidence.status() == Status.DEVELOPING) return 3;
    return evidence.status() == Status.STRENGTH ? 5 : 4;
  }

  private String reason(Evidence e) {
    return switch (e.status()) {
      case NEEDS_PRACTICE -> "Recent answers on different problems suggest this is a good place to focus.";
      case UNASSESSED -> "Try this topic to see how you get on.";
      case DEVELOPING -> e.recentUnresolvedCorrection()
        ? "You made a correction after feedback. Try a fresh problem to check your understanding."
        : "Try a few more different questions to see how consistently you can do this.";
      case STRENGTH -> throw new IllegalArgumentException("Strengths do not need a practice recommendation");
    };
  }

  private Evidence assess(Target target, List<Attempt> attempts, OffsetDateTime cutoff) {
    // Only a recent first exposure gives independent credit; closely related variants
    // contribute at most one outcome per family. An older attempt never acquires new tags.
    var firstByFamily = new LinkedHashMap<String, Attempt>();
    for (Attempt attempt : attempts) {
      if (attempt.fresh() && !attempt.submittedAt().isBefore(cutoff)) {
        firstByFamily.putIfAbsent(attempt.familyKey(), attempt);
      }
    }
    var distinct = firstByFamily.values().stream()
      .skip(Math.max(0, firstByFamily.size() - RECENT_FAMILIES)).toList();
    int correct = (int) distinct.stream().filter(Attempt::correct).count();
    long sessions = distinct.stream().map(Attempt::sessionId).distinct().count();
    Status status = Status.DEVELOPING;
    if (attempts.isEmpty()) status = Status.UNASSESSED;
    else if (target.objectiveId() != null && distinct.stream().allMatch(a -> a.familyCode() != null)
      && distinct.size() >= 3 && sessions >= 2 && correct * 5 >= distinct.size() * 4) {
      status = Status.STRENGTH;
    } else if (distinct.size() - correct >= 2 && correct * 10 < distinct.size() * 7) {
      status = Status.NEEDS_PRACTICE;
    }
    String difficulty = DIFFICULTIES.stream().filter(level -> {
      var atLevel = distinct.stream()
        .filter(a -> target.objectiveId() != null && a.familyCode() != null && level.equals(a.difficulty())).toList();
      return atLevel.stream().map(Attempt::sessionId).distinct().count() >= 2
        && atLevel.stream().filter(Attempt::correct).count() * 5 >= atLevel.size() * 4;
    }).findFirst().orElse(null);
    var pendingCorrections = new HashMap<String, Attempt>();
    var recoveredFamilies = new HashSet<String>();
    int corrections = 0;
    for (Attempt attempt : attempts) {
      if (attempt.corrected()) corrections++;
      if (attempt.familyCode() == null || attempt.submittedAt().isBefore(cutoff)) continue;
      if (attempt.fresh() && attempt.correct() && pendingCorrections.values().removeIf(previous ->
        !previous.familyKey().equals(attempt.familyKey()) &&
        !previous.sessionId().equals(attempt.sessionId()) &&
        previous.submittedAt().isBefore(attempt.submittedAt())
      )) recoveredFamilies.add(attempt.familyKey());
      if (attempt.corrected()) pendingCorrections.put(attempt.familyKey(), attempt);
    }
    return new Evidence(target.objectiveId(), target.sectionId(), target.chapterId(),
      target.title(), status, distinct.size(), correct, corrections, recoveredFamilies.size(),
      !pendingCorrections.isEmpty(),
      difficulty, target.availableQuestions(), target.freshQuestions());
  }
}
