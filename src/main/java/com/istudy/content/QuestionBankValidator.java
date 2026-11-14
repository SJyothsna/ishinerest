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
    copy.remove(List.of("externalId", "sourcePage", "objectiveCode", "familyCode"));
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
    String version = manifest.path("formatVersion").asText();
    boolean planned = version.equals("2.0");
    if (!planned && !version.equals("1.0")) errors.add("formatVersion must be 2.0 or 1.0.");
    String track = db.queryForObject(
      "select track_code from subject_offerings where offering_id=?",
      String.class, offering
    );
    boolean levelled = Set.of("ORDINARY", "HIGHER").contains(track);
    var publishedObjectives = planned ? publishedObjectives(offering) : Map.<String, Map<String, Objective>>of();
    var coverage = new ArrayList<Map<String, Object>>();
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
        var objectives = planned
          ? validateObjectives(key, topic, set, publishedObjectives.getOrDefault(key, Map.of()), errors)
          : Map.<String, Objective>of();
        var batchFamilies = new HashMap<String, Set<String>>();
        if (
          !topic.path("questions").isArray() ||
          ((!planned || set != 1) && topic.path("questions").isEmpty())
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
          if (planned) {
            String objectiveCode = q.path("objectiveCode").asText();
            String familyCode = q.path("familyCode").asText();
            if (!objectives.containsKey(objectiveCode)) errors.add(
              prefix + ": choose an objectiveCode from this subtopic's plan."
            );
            if (!code(familyCode)) errors.add(prefix + ": include a stable familyCode.");
            if (
              set == 1 && objectives.containsKey(objectiveCode) &&
              code(familyCode) && !"worked".equals(q.path("type").asText())
            ) batchFamilies.computeIfAbsent(objectiveCode, unused -> new HashSet<>()).add(familyCode);
          }
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
          String examLevel = q.path("examLevel").asText("COMMON");
          if (levelled && !examLevel.equals("COMMON") && !examLevel.equals(track)) errors.add(
            prefix + ": " + examLevel + " questions do not belong to this " + track + " course."
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
        if (planned) for (var entry : objectives.entrySet()) {
          var objective = entry.getValue();
          Set<String> incoming = batchFamilies.getOrDefault(entry.getKey(), Set.of());
          var available = new HashSet<>(objective.publishedFamilies());
          available.addAll(incoming);
          coverage.add(Map.of(
            "chapterCode", ch,
            "topicCode", topicCode,
            "objectiveCode", entry.getKey(),
            "title", objective.title(),
            "required", objective.required(),
            "publishedFamilies", objective.publishedFamilies().size(),
            "uploadFamilies", incoming.size(),
            "resultingFamilies", available.size()
          ));
          if (objective.required() && available.size() < 2) warnings.add(
            key + " / " + entry.getKey() + ": " +
            (available.isEmpty() ? "no graded Set 1 questions" : "only one Set 1 question family") +
            " across the live bank and this upload."
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
          if (!planned && actual.getOrDefault(level, 0) == 0) warnings.add(
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
      "objectiveCoverage",
      coverage,
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

  private record Objective(String title, boolean required, Set<String> publishedFamilies) {}

  private Map<String, Map<String, Objective>> publishedObjectives(UUID offering) {
    var result = new LinkedHashMap<String, Map<String, Objective>>();
    db.query(
      """
      select c.code as chapter_code, s.code as topic_code, o.code, o.title, o.required,
             case when q.question_id is not null then p.family_code end as family_code
      from learning_objectives o
      join sections s on s.section_id=o.section_id
      join chapters c on c.chapter_id=s.chapter_id
      left join question_placements p on p.objective_id=o.objective_id and p.active and p.question_set=1
      left join questions q on q.question_id=p.question_id and q.approval_status='PUBLISHED'
        and q.visibility='GLOBAL' and q.question_type<>'WORKED_EXAMPLE'
      where c.offering_id=?
      order by c.code,s.code,o.code
      """,
      rs -> {
        String key = rs.getString("chapter_code") + "/" + rs.getString("topic_code");
        var byCode = result.computeIfAbsent(key, unused -> new LinkedHashMap<>());
        String code = rs.getString("code");
        if (!byCode.containsKey(code)) byCode.put(
          code, new Objective(rs.getString("title"), rs.getBoolean("required"), new HashSet<>())
        );
        String family = rs.getString("family_code");
        if (family != null) byCode.get(code).publishedFamilies().add(family);
      },
      offering
    );
    return result;
  }

  private Map<String, Objective> validateObjectives(
    String key, JsonNode topic, int set, Map<String, Objective> existing, List<String> errors
  ) {
    var result = new LinkedHashMap<String, Objective>();
    if (!topic.path("objectives").isArray() || topic.path("objectives").isEmpty()) {
      errors.add(key + ": include the reviewed subtopic objectives.");
      return result;
    }
    for (var definition : topic.path("objectives")) {
      String code = definition.path("code").asText();
      String title = definition.path("title").asText();
      if (!code(code) || !definition.path("title").isTextual() || title.isBlank() ||
          title.length() > 300 || !definition.path("required").isBoolean() || result.containsKey(code)) {
        errors.add(key + ": each objective needs a unique code, title and required flag.");
        continue;
      }
      boolean required = definition.path("required").asBoolean();
      var previous = existing.get(code);
      if (set == 2 && (previous == null || !previous.title().equals(title) || previous.required() != required))
        errors.add(key + "/" + code + ": Set 2 must reuse the published Set 1 objective.");
      result.put(code, new Objective(title, required, previous == null ? Set.of() : previous.publishedFamilies()));
    }
    for (String code : existing.keySet()) if (!result.containsKey(code)) errors.add(
      key + ": include existing objective " + code + " in the plan."
    );
    if (set == 1 && result.values().stream().noneMatch(Objective::required)) errors.add(
      key + ": include at least one required objective."
    );
    return result;
  }

  private boolean code(String value) {
    return value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,119}");
  }
}
