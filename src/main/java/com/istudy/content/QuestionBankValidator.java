package com.istudy.content;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankValidator {

  private final QuestionDraftValidator questions;
  private final JdbcTemplate db;
  private final com.istudy.identity.CurrentStudent current;

  public QuestionBankValidator(
    QuestionDraftValidator questions,
    JdbcTemplate db,
    com.istudy.identity.CurrentStudent current
  ) {
    this.questions = questions;
    this.db = db;
    this.current = current;
  }

  public ObjectNode snapshot(JsonNode q) {
    var copy = ((ObjectNode) q).deepCopy();
    copy.remove(List.of("externalId", "sourcePage"));
    String value = q.path("difficulty").asText();
    copy.put(
      "difficulty",
      switch (value) {
        case "EASY" -> "FOUNDATION";
        case "MEDIUM" -> "INTERMEDIATE";
        case "HARD" -> "ADVANCED";
        default -> value;
      }
    );
    return copy;
  }

  public Map<String, Object> validate(UUID offering, JsonNode manifest) {
    var errors = new ArrayList<String>();
    var warnings = new ArrayList<String>();
    var keys = new HashMap<String, JsonNode>();
    var fingerprints = new HashMap<String, String>();
    var set1Fingerprints = new HashSet<String>();
    var chapterCodes = new HashSet<String>();
    var topicKeys = new HashSet<String>();
    int topicCount = 0, total = 0;
    var levels = new LinkedHashMap<String, Integer>();
    for (String level : List.of("EASY", "MEDIUM", "HARD")) levels.put(level, 0);
    if (!manifest.path("formatVersion").asText().equals("1.0")) errors.add(
      "formatVersion must be 1.0."
    );
    String sourceType = manifest.path("sourceType").asText("TEXTBOOK");
    if (!Set.of("TEXTBOOK", "ORIGINAL").contains(sourceType)) errors.add(
      "sourceType must be TEXTBOOK or ORIGINAL."
    );
    boolean original = sourceType.equals("ORIGINAL");
    int set = manifest.path("questionSet").asInt();
    if (set != 1 && set != 2) errors.add("questionSet must be 1 or 2.");
    if (set == 2) for (String old : db.queryForList(
      "select distinct r.content_snapshot from questions q join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision join question_placements p on p.question_id=q.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and p.question_set=1 and p.active and q.approval_status='PUBLISHED'",
      String.class,
      offering
    )) {
      try {
        set1Fingerprints.add(
          questions.fingerprint(new ObjectMapper().readTree(old))
        );
      } catch (Exception ignored) {}
    }
    String scope = manifest.path("coverageScope").asText();
    if (!Set.of("SUBJECT", "CHAPTER", "TOPICS").contains(scope)) errors.add(
      "Choose coverageScope SUBJECT, CHAPTER or TOPICS."
    );
    if (
      !manifest.path("chapters").isArray() ||
      manifest.path("chapters").isEmpty()
    ) errors.add("Include at least one chapter.");
    for (var chapter : manifest.path("chapters")) {
      String ch = chapter.path("code").asText();
      if (
        !code(ch) ||
        !chapterCodes.add(ch) ||
        chapter.path("title").asText().isBlank()
      ) errors.add("Every chapter needs a unique code and a title.");
      if (
        !chapter.path("topics").isArray() || chapter.path("topics").isEmpty()
      ) errors.add(ch + ": include at least one topic.");
      for (var topic : chapter.path("topics")) {
        topicCount++;
        String topicCode = topic.path("code").asText(), key =
          ch + "/" + topicCode;
        if (
          !code(topicCode) ||
          !topicKeys.add(key) ||
          topic.path("title").asText().isBlank()
        ) errors.add(key + ": topic code/title is missing or repeated.");
        var actual = new HashMap<String, Integer>();
        if (
          !topic.path("questions").isArray() ||
          topic.path("questions").isEmpty()
        ) errors.add(key + ": include questions.");
        for (var q : topic.path("questions")) {
          total++;
          String external = q.path("externalId").asText();
          String prefix = key + " / " + external;
          if (!code(external)) errors.add(
            prefix +
            ": use a readable externalId with letters, numbers, dots, hyphens or underscores."
          );
          String level = q.path("difficulty").asText();
          if (!levels.containsKey(level)) errors.add(
            prefix + ": difficulty must be EASY, MEDIUM or HARD."
          );
          else {
            levels.put(level, levels.get(level) + 1);
            actual.merge(level, 1, Integer::sum);
          }
          if (q.hasNonNull("imageAssetId")) {
            try {
              UUID asset = UUID.fromString(q.get("imageAssetId").asText());
              if (
                !Boolean.TRUE.equals(
                  db.queryForObject(
                    "select exists(select 1 from media_assets a where a.asset_id=? and a.scan_status='CLEAN' and a.content_type in ('image/png','image/jpeg') and (a.owner_user_id=? or exists(select 1 from questions old where old.question_image_asset_id=a.asset_id and old.approval_status='PUBLISHED' and old.visibility='GLOBAL')))",
                    Boolean.class,
                    asset,
                    current.id()
                  )
                )
              ) errors.add(
                prefix + ": imageAssetId is not an available diagram."
              );
            } catch (Exception e) {
              errors.add(
                prefix + ": imageAssetId must identify an image you uploaded."
              );
            }
          }
          if (
            (!original || q.has("sourcePage")) &&
            q.path("sourcePage").asInt() < 1
          ) errors.add(prefix + ": sourcePage must be a positive page number.");
          if (
            q.has("examLevel") &&
            !Set.of("COMMON", "ORDINARY", "HIGHER").contains(
              q.path("examLevel").asText()
            )
          ) errors.add(
            prefix + ": examLevel must be COMMON, ORDINARY or HIGHER."
          );
          try {
            questions.validate(snapshot(q));
          } catch (Exception e) {
            errors.add(prefix + ": " + e.getMessage());
          }
          String fingerprint = q.isObject()
            ? questions.fingerprint(q)
            : "invalid";
          JsonNode normalized = q.isObject() ? snapshot(q) : q;
          if (
            keys.containsKey(external) && !keys.get(external).equals(normalized)
          ) errors.add(
            prefix +
            ": this externalId has different content elsewhere in the file."
          );
          else keys.put(external, normalized);
          if (
            fingerprints.containsKey(fingerprint) &&
            !fingerprints.get(fingerprint).equals(external)
          ) errors.add(
            prefix +
            ": repeated question content; reuse the same externalId when mapping one question to several topics."
          );
          else fingerprints.put(fingerprint, external);
          if (set == 2 && set1Fingerprints.contains(fingerprint)) errors.add(
            prefix +
            ": this repeats a Set 1 question. Set 2 needs a fresh variation."
          );
        }
        for (String level : levels.keySet()) {
          JsonNode expected = topic.path("expectedCounts").path(level);
          if (!expected.isIntegralNumber() || expected.asInt() < 0) errors.add(
            key +
            ": provide expectedCounts for " +
            level +
            " (zero is allowed if intentional)."
          );
          else if (
            expected.asInt() != actual.getOrDefault(level, 0)
          ) errors.add(
            key +
            ": planned " +
            expected.asInt() +
            " " +
            level +
            " questions, found " +
            actual.getOrDefault(level, 0) +
            "."
          );
          if (actual.getOrDefault(level, 0) == 0) warnings.add(
            key + ": no " + level.toLowerCase(Locale.ROOT) + " questions."
          );
        }
      }
    }
    if (total > 2000) errors.add(
      "Use at most 2,000 questions per upload; split larger books by chapter."
    );
    boolean replacingStarter =
      set == 1 &&
      Boolean.TRUE.equals(
        db.queryForObject(
          "select is_starter from subject_offerings where offering_id=?",
          Boolean.class,
          offering
        )
      );
    if (replacingStarter) warnings.add(
      "This first Set 1 bank replaces the starter structure. Review the intended textbook chapters and topic coverage before publishing."
    );
    if (
      !replacingStarter && !scope.equals("TOPICS")
    ) for (var row : db.queryForList(
      "select c.code as chapter_code,s.code as topic_code from chapters c left join sections s on s.chapter_id=c.chapter_id and s.status='ACTIVE' where c.offering_id=? and c.status='ACTIVE'",
      offering
    )) {
      String ch = row.get("chapter_code").toString();
      if (scope.equals("SUBJECT") && !chapterCodes.contains(ch)) errors.add(
        "Missing chapter: " + ch
      );
      if (
        chapterCodes.contains(ch) &&
        row.get("topic_code") != null &&
        !topicKeys.contains(ch + "/" + row.get("topic_code"))
      ) errors.add(
        "Missing existing topic: " + ch + "/" + row.get("topic_code")
      );
    }
    return Map.of(
      "valid",
      errors.isEmpty(),
      "issues",
      errors,
      "warnings",
      warnings,
      "summary",
      Map.of(
        "chapters",
        chapterCodes.size(),
        "topics",
        topicCount,
        "questions",
        total,
        "uniqueQuestions",
        keys.size(),
        "difficultyCounts",
        levels,
        "questionSet",
        set
      )
    );
  }

  private boolean code(String value) {
    return value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,119}");
  }
}
