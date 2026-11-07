package com.ishine.ishinerest.service;

import com.ishine.ishinerest.config.PracticeSelectionProperties;
import com.ishine.ishinerest.entity.PracticeSession;
import com.ishine.ishinerest.entity.PracticeSessionAnswer;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.Section;
import com.ishine.ishinerest.pojo.PracticeSelectRequest;
import com.ishine.ishinerest.pojo.PracticeSelectionResponse;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionAnswerRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.SectionRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptive selection of practice questions for a chapter. Read-only: it never
 * records anything about the student's progress.
 *
 * Section progress is derived from submitted practice sessions
 * (practice_sessions / practice_session_answers).
 */
@Service
@RequiredArgsConstructor
public class PracticeSelectionService {

    private static final String NONE_KEY = "__NONE__";
    private static final List<String> COMPLEXITIES = List.of("FOUNDATION", "INTERMEDIATE", "ADVANCED");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;
    private final PracticeSessionAnswerRepository answerRepository;
    private final FlaggedQuestionRepository flaggedQuestionRepository;
    private final StudentRepository studentRepository;
    private final ChapterRepository chapterRepository;
    private final PracticeSelectionProperties props;

    private final Random random = new Random();

    // ------------------------------------------------------------------ model

    private static class Sec {
        String key;
        String id;
        String name;
        int order;
        String complexity;
        final List<Question> eligible = new ArrayList<>();
        int attempts;
        int correct;
        LocalDateTime lastAttemptedAt;
        final Set<Long> distinctQuestions = new HashSet<>();
        String status = "UNSEEN";
        int weight;
        int planned;
        int returned;
    }

    private static class QHist {
        boolean lastCorrect;
        LocalDateTime lastAt;
    }

    // ------------------------------------------------------------------- main

    @Transactional(readOnly = true)
    public PracticeSelectionResponse selectQuestions(Long studentId, String chapterId, PracticeSelectRequest req) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
        if (!chapterRepository.existsById(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapterId);
        }
        String requestedDifficulty = parseRequestedDifficulty(req.getDifficulty());

        // 1. Sections of the chapter, in learning order
        Map<String, Sec> sections = new LinkedHashMap<>();
        for (Section s : sectionRepository.findByChapter_ChapterIdOrderByDisplayOrderAscSectionIdAsc(chapterId)) {
            Sec sec = new Sec();
            sec.key = s.getSectionId();
            sec.id = s.getSectionId();
            sec.name = s.getSectionName();
            sec.order = s.getDisplayOrder() != null ? s.getDisplayOrder() : 0;
            sec.complexity = normalizeComplexity(s.getComplexityLevel());
            sections.put(sec.key, sec);
        }

        // 2. All chapter questions -> section key; eligible subset
        List<Question> all = questionRepository.findByChapter_ChapterId(chapterId);
        Map<Long, String> sectionKeyByQuestion = new HashMap<>();
        for (Question q : all) {
            String sid = q.getSectionId();
            sectionKeyByQuestion.put(q.getQuestionId(), sid != null && sections.containsKey(sid) ? sid : NONE_KEY);
        }
        for (Question q : all) {
            if (!isEligible(q, req)) {
                continue;
            }
            String key = sectionKeyByQuestion.get(q.getQuestionId());
            Sec sec = sections.get(key);
            if (sec == null) {
                sec = new Sec();
                sec.key = NONE_KEY;
                sec.name = "Unsectioned";
                sec.order = Integer.MAX_VALUE;
                sec.complexity = "INTERMEDIATE";
                sections.put(NONE_KEY, sec);
            }
            sec.eligible.add(q);
        }
        List<Sec> active = sections.values().stream()
                .filter(s -> !s.eligible.isEmpty())
                .sorted(Comparator.comparingInt((Sec s) -> s.order))
                .collect(Collectors.toList());

        PracticeSelectionResponse response = new PracticeSelectionResponse();
        response.setRequestedCount(req.getCount());
        int totalEligible = active.stream().mapToInt(s -> s.eligible.size()).sum();
        if (totalEligible == 0) {
            response.setStudentType("FIRST_TIME");
            return response;
        }
        int count = Math.min(req.getCount(), totalEligible);

        // 3. Student history -> per-section stats and per-question last result
        List<PracticeSessionAnswer> history = answerRepository
                .findGradedAnswers(studentId, chapterId, PracticeSession.Status.SUBMITTED);
        Map<Long, QHist> questionHistory = new HashMap<>();
        for (PracticeSessionAnswer a : history) {
            String key = sectionKeyByQuestion.get(a.getQuestionId());
            if (key == null) {
                continue;
            }
            Sec sec = sections.get(key);
            if (sec != null) {
                sec.attempts++;
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    sec.correct++;
                }
                sec.distinctQuestions.add(a.getQuestionId());
                if (a.getAnsweredAt() != null
                        && (sec.lastAttemptedAt == null || a.getAnsweredAt().isAfter(sec.lastAttemptedAt))) {
                    sec.lastAttemptedAt = a.getAnsweredAt();
                }
            }
            QHist h = questionHistory.computeIfAbsent(a.getQuestionId(), k -> new QHist());
            h.lastCorrect = Boolean.TRUE.equals(a.getIsCorrect()); // history is ordered oldest first
            h.lastAt = a.getAnsweredAt();
        }
        for (Sec s : active) {
            s.status = statusOf(s.attempts, s.correct);
            s.weight = weightOf(s.status);
        }
        boolean firstTime = history.isEmpty();
        response.setStudentType(firstTime ? "FIRST_TIME" : "RETURNING");

        // 4. Slots per section
        Map<String, Integer> slots = firstTime ? allocateFirstTime(active, count) : allocateReturning(active, count);
        for (Sec s : active) {
            s.planned = slots.getOrDefault(s.key, 0);
        }

        // 5. Difficulty targets per section
        Map<String, List<String>> targets = difficultyTargets(active, slots, firstTime, requestedDifficulty);

        // 6. Pick individual questions
        int maxRepeats = (int) Math.ceil(count * props.getMaxRepeatFraction());
        Picker picker = new Picker(questionHistory, maxRepeats, active);
        for (Sec s : active) {
            for (String target : targets.getOrDefault(s.key, List.of())) {
                Question q = picker.choose(s.eligible, target, true, 2);
                if (q == null) {
                    for (String neighbour : neighbours(target)) {
                        q = picker.choose(s.eligible, neighbour, true, 2);
                        if (q != null) {
                            break;
                        }
                    }
                }
                if (q != null) {
                    picker.add(q);
                }
            }
        }

        // 7. Fallbacks: fill any shortage from any section. Unseen questions anywhere come before questions
        //    answered correctly; the repeat cap is honoured first and only relaxed as a last resort.
        List<Question> pool = active.stream().flatMap(s -> s.eligible.stream()).collect(Collectors.toList());
        for (boolean enforceCap : new boolean[] { true, false }) {
            while (picker.chosen.size() < count) {
                Question q = picker.choose(pool, null, enforceCap, 4);
                if (q == null) {
                    break;
                }
                picker.add(q);
            }
        }

        // 8. Order in learning sequence and build the response
        Map<Long, Sec> secByQuestion = new HashMap<>();
        for (Sec s : active) {
            for (Question q : s.eligible) {
                secByQuestion.put(q.getQuestionId(), s);
            }
        }
        List<Question> ordered = new ArrayList<>(picker.chosen);
        ordered.sort(Comparator
                .comparingInt((Question q) -> secByQuestion.get(q.getQuestionId()).order)
                .thenComparingInt(q -> DIFFICULTIES.indexOf(difficultyOf(q))));
        for (Question q : ordered) {
            secByQuestion.get(q.getQuestionId()).returned++;
        }

        Set<Long> flagged = flaggedQuestionRepository.findByStudent_StudentId(studentId).stream()
                .map(fq -> fq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());
        for (Question q : ordered) {
            response.getQuestions().add(new QuestionWithFlagDTO(q, flagged.contains(q.getQuestionId())));
        }
        response.setReturnedCount(ordered.size());
        for (Sec s : active) {
            PracticeSelectionResponse.SectionPlan plan = new PracticeSelectionResponse.SectionPlan();
            plan.setSectionId(s.id);
            plan.setSectionName(s.name);
            plan.setDisplayOrder(s.order == Integer.MAX_VALUE ? 0 : s.order);
            plan.setComplexityLevel(s.complexity);
            plan.setStatus(s.status);
            plan.setAttempts(s.attempts);
            plan.setAccuracy(s.attempts == 0 ? null : Math.round(s.correct * 10000.0 / s.attempts) / 100.0);
            plan.setEligibleQuestions(s.eligible.size());
            plan.setPlanned(s.planned);
            plan.setReturned(s.returned);
            response.getSections().add(plan);
        }
        return response;
    }

    // ------------------------------------------------------------ eligibility

    private boolean isEligible(Question q, PracticeSelectRequest req) {
        // custom questions: only public ones (system questions are not custom)
        if (Boolean.TRUE.equals(q.getIsCustom()) && !"PUBLIC".equalsIgnoreCase(q.getVisibility())) {
            return false;
        }
        String usage = req.getUsageType();
        if (usage != null && !usage.isBlank() && !usage.equalsIgnoreCase("ALL")) {
            String qUsage = q.getUsageType() == null || q.getUsageType().isBlank() ? "both" : q.getUsageType();
            if (!qUsage.equalsIgnoreCase(usage) && !qUsage.equalsIgnoreCase("both")) {
                return false;
            }
        }
        if (req.getQuestionSet() != null) {
            String qSet = q.getQuestionSet() == null || q.getQuestionSet().isBlank() ? "1" : q.getQuestionSet().trim();
            if (!qSet.equals(String.valueOf(req.getQuestionSet()))) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------- status / weights

    private String statusOf(int attempts, int correct) {
        if (attempts == 0) {
            return "UNSEEN";
        }
        if (attempts < props.getMinAttemptsForRating()) {
            return "LEARNING";
        }
        double accuracy = correct * 100.0 / attempts;
        if (accuracy < props.getNeedsPracticeBelowPct()) {
            return "NEEDS_PRACTICE";
        }
        if (accuracy < props.getStrongAtOrAbovePct()) {
            return "DEVELOPING";
        }
        return "STRONG";
    }

    private int weightOf(String status) {
        return switch (status) {
            case "NEEDS_PRACTICE" -> props.getWeightNeedsPractice();
            case "UNSEEN" -> props.getWeightUnseen();
            case "LEARNING" -> props.getWeightLearning();
            case "DEVELOPING" -> props.getWeightDeveloping();
            default -> props.getWeightStrong();
        };
    }

    // -------------------------------------------------------- slot allocation

    /** First-time student: complexity quotas, then round-robin over sections by display order. */
    private Map<String, Integer> allocateFirstTime(List<Sec> active, int count) {
        List<String> complexityOrder = new ArrayList<>();
        Map<String, Double> pct = new HashMap<>();
        Map<String, Integer> caps = new HashMap<>();
        pct.put("FOUNDATION", (double) props.getFoundationPct());
        pct.put("INTERMEDIATE", (double) props.getIntermediatePct());
        pct.put("ADVANCED", (double) props.getAdvancedPct());
        for (String c : COMPLEXITIES) {
            int cap = active.stream().filter(s -> s.complexity.equals(c)).mapToInt(s -> s.eligible.size()).sum();
            if (cap > 0) {
                complexityOrder.add(c);
                caps.put(c, cap);
            }
        }
        Map<String, Integer> quota = allocate(count, complexityOrder, pct, caps);

        Map<String, Integer> slots = new HashMap<>();
        for (String c : complexityOrder) {
            List<Sec> group = active.stream().filter(s -> s.complexity.equals(c)).collect(Collectors.toList());
            int remaining = quota.getOrDefault(c, 0);
            while (remaining > 0) {
                boolean progressed = false;
                for (Sec s : group) {
                    if (remaining == 0) {
                        break;
                    }
                    if (slots.getOrDefault(s.key, 0) < s.eligible.size()) {
                        slots.merge(s.key, 1, Integer::sum);
                        remaining--;
                        progressed = true;
                    }
                }
                if (!progressed) {
                    break;
                }
            }
        }
        return slots;
    }

    /** Returning student: one slot per section when possible, the rest proportional to status weights. */
    private Map<String, Integer> allocateReturning(List<Sec> active, int count) {
        Map<String, Integer> slots = new HashMap<>();
        if (count < active.size()) {
            List<Sec> byPriority = new ArrayList<>(active);
            byPriority.sort(Comparator.comparingInt((Sec s) -> -s.weight).thenComparingInt(s -> s.order));
            for (int i = 0; i < count; i++) {
                slots.put(byPriority.get(i).key, 1);
            }
            return slots;
        }
        for (Sec s : active) {
            slots.put(s.key, 1);
        }
        List<String> order = new ArrayList<>();
        Map<String, Double> weights = new HashMap<>();
        Map<String, Integer> caps = new HashMap<>();
        for (Sec s : active) {
            order.add(s.key);
            weights.put(s.key, (double) s.weight);
            caps.put(s.key, s.eligible.size() - 1);
        }
        Map<String, Integer> extra = allocate(count - active.size(), order, weights, caps);
        extra.forEach((k, v) -> slots.merge(k, v, Integer::sum));
        return slots;
    }

    /**
     * Largest-remainder allocation of {@code slots} across keys by weight, never exceeding caps.
     * Fractional ties are broken by position in {@code order}. Unused slots (caps) are redistributed.
     */
    private Map<String, Integer> allocate(int slots, List<String> order, Map<String, Double> weights,
            Map<String, Integer> caps) {
        Map<String, Integer> result = new LinkedHashMap<>();
        order.forEach(k -> result.put(k, 0));
        int remaining = slots;
        while (remaining > 0) {
            List<String> activeKeys = order.stream()
                    .filter(k -> result.get(k) < caps.getOrDefault(k, 0) && weights.getOrDefault(k, 0.0) > 0)
                    .collect(Collectors.toList());
            if (activeKeys.isEmpty()) {
                break;
            }
            double totalWeight = activeKeys.stream().mapToDouble(weights::get).sum();
            Map<String, Double> fraction = new HashMap<>();
            int given = 0;
            for (String k : activeKeys) {
                double exact = remaining * weights.get(k) / totalWeight;
                int floor = (int) Math.floor(exact);
                int grant = Math.min(floor, caps.get(k) - result.get(k));
                result.merge(k, grant, Integer::sum);
                given += grant;
                fraction.put(k, exact - floor);
            }
            int leftover = remaining - given;
            List<String> ranked = new ArrayList<>(activeKeys);
            ranked.sort((a, b) -> Double.compare(fraction.get(b), fraction.get(a))); // stable: ties keep order
            for (String k : ranked) {
                if (leftover == 0) {
                    break;
                }
                if (result.get(k) < caps.get(k)) {
                    result.merge(k, 1, Integer::sum);
                    leftover--;
                    given++;
                }
            }
            if (given == 0) {
                break;
            }
            remaining -= given;
        }
        return result;
    }

    // ------------------------------------------------------------- difficulty

    private Map<String, List<String>> difficultyTargets(List<Sec> active, Map<String, Integer> slots,
            boolean firstTime, String requested) {
        Map<String, List<String>> targets = new HashMap<>();

        if (requested != null) {
            for (Sec s : active) {
                targets.put(s.key, Collections.nCopies(slots.getOrDefault(s.key, 0), requested));
            }
            return targets;
        }

        if (firstTime) {
            int totalSlots = slots.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> pct = Map.of("EASY", (double) props.getEasyPct(),
                    "MEDIUM", (double) props.getMediumPct(), "HARD", (double) props.getHardPct());
            Map<String, Integer> caps = new HashMap<>();
            for (String d : DIFFICULTIES) {
                caps.put(d, (int) active.stream().flatMap(s -> s.eligible.stream())
                        .filter(q -> difficultyOf(q).equals(d)).count());
            }
            Map<String, Integer> remaining = new HashMap<>(allocate(totalSlots, DIFFICULTIES, pct, caps));

            for (Sec s : active) {
                Map<String, Integer> avail = new HashMap<>();
                for (String d : DIFFICULTIES) {
                    avail.put(d, (int) s.eligible.stream().filter(q -> difficultyOf(q).equals(d)).count());
                }
                List<String> list = new ArrayList<>();
                for (int i = 0; i < slots.getOrDefault(s.key, 0); i++) {
                    String best = null;
                    for (String d : DIFFICULTIES) {
                        if (remaining.getOrDefault(d, 0) > 0 && avail.get(d) > 0
                                && (best == null || remaining.get(d) > remaining.get(best))) {
                            best = d;
                        }
                    }
                    if (best != null) {
                        remaining.merge(best, -1, Integer::sum);
                    } else {
                        for (String d : DIFFICULTIES) { // nothing left in the plan: use what the section has most of
                            if (avail.get(d) > 0 && (best == null || avail.get(d) > avail.get(best))) {
                                best = d;
                            }
                        }
                    }
                    avail.merge(best, -1, Integer::sum);
                    list.add(best);
                }
                targets.put(s.key, list);
            }
            return targets;
        }

        // Returning student: mix depends on the section's status
        for (Sec s : active) {
            int n = slots.getOrDefault(s.key, 0);
            double[] mix = difficultyMix(s.status); // easy, medium, hard
            Map<String, Double> weights = new HashMap<>();
            weights.put("EASY", mix[0]);
            weights.put("MEDIUM", mix[1]);
            weights.put("HARD", mix[2]);
            Map<String, Integer> caps = new HashMap<>();
            for (String d : DIFFICULTIES) {
                caps.put(d, (int) s.eligible.stream().filter(q -> difficultyOf(q).equals(d)).count());
            }
            // tie-break order: medium first, so a strong section's 4 slots become 0/3/1 rather than 1/2/1
            Map<String, Integer> counts = allocate(n, List.of("MEDIUM", "EASY", "HARD"), weights, caps);
            List<String> list = new ArrayList<>();
            for (String d : DIFFICULTIES) {
                list.addAll(Collections.nCopies(counts.getOrDefault(d, 0), d));
            }
            while (list.size() < n) {
                list.add("MEDIUM"); // preferred difficulties exhausted; neighbours are tried when picking
            }
            targets.put(s.key, list);
        }
        return targets;
    }

    private double[] difficultyMix(String status) {
        return switch (status) {
            case "UNSEEN" -> new double[] { 70, 30, 0 };
            case "LEARNING" -> new double[] { 60, 40, 0 };
            case "NEEDS_PRACTICE" -> new double[] { 70, 30, 0 };
            case "DEVELOPING" -> new double[] { 30, 60, 10 };
            default -> new double[] { 10, 60, 30 }; // STRONG
        };
    }

    private List<String> neighbours(String d) {
        return switch (d) {
            case "EASY" -> List.of("MEDIUM", "HARD");
            case "HARD" -> List.of("MEDIUM", "EASY");
            default -> List.of("EASY", "HARD");
        };
    }

    /** Maps stored difficulty text (Basic/Intermediate/Advanced, Easy/Medium/Hard) to EASY/MEDIUM/HARD. */
    private String difficultyOf(Question q) {
        String d = q.getDifficultyLevel() == null ? "" : q.getDifficultyLevel().trim().toLowerCase(Locale.ROOT);
        return switch (d) {
            case "basic", "easy" -> "EASY";
            case "advanced", "hard" -> "HARD";
            default -> "MEDIUM";
        };
    }

    private String parseRequestedDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank() || difficulty.equalsIgnoreCase("AUTO")) {
            return null;
        }
        return switch (difficulty.trim().toLowerCase(Locale.ROOT)) {
            case "easy", "basic" -> "EASY";
            case "medium", "intermediate" -> "MEDIUM";
            case "hard", "advanced" -> "HARD";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "difficulty must be AUTO, EASY, MEDIUM or HARD");
        };
    }

    private String normalizeComplexity(String value) {
        if (value == null) {
            return "INTERMEDIATE";
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        return COMPLEXITIES.contains(v) ? v : "INTERMEDIATE";
    }

    // ----------------------------------------------------------- question pick

    /** Tracks the batch being built and scores candidate questions. */
    private class Picker {
        final Map<Long, QHist> history;
        final int maxRepeats;
        final Map<String, Sec> secByKey = new HashMap<>();
        final Map<Long, Sec> secByQuestion = new HashMap<>();
        final List<Question> chosen = new ArrayList<>();
        final Set<Long> chosenIds = new HashSet<>();
        final Map<String, Integer> styleCounts = new HashMap<>();
        int repeats;

        Picker(Map<Long, QHist> history, int maxRepeats, List<Sec> active) {
            this.history = history;
            this.maxRepeats = maxRepeats;
            for (Sec s : active) {
                secByKey.put(s.key, s);
                for (Question q : s.eligible) {
                    secByQuestion.put(q.getQuestionId(), s);
                }
            }
        }

        /** 1 never attempted, 2 last attempt wrong, 3 correct but not recently, 4 correct recently */
        int bucket(Question q) {
            QHist h = history.get(q.getQuestionId());
            if (h == null) {
                return 1;
            }
            if (!h.lastCorrect) {
                return 2;
            }
            boolean recent = h.lastAt != null && h.lastAt.isAfter(LocalDateTime.now().minusDays(props.getRecentDays()));
            return recent ? 4 : 3;
        }

        int stylePenalty(Question q) {
            String style = q.getQuestionStyle();
            return style == null || style.isBlank() ? 0 : styleCounts.getOrDefault(style, 0);
        }

        /** @param maxBucket highest bucket accepted (2 = only never-attempted or previously wrong questions) */
        Question choose(List<Question> candidates, String difficulty, boolean enforceRepeatCap, int maxBucket) {
            List<Question> usable = new ArrayList<>();
            for (Question q : candidates) {
                if (chosenIds.contains(q.getQuestionId()) || bucket(q) > maxBucket) {
                    continue;
                }
                if (difficulty != null && !difficultyOf(q).equals(difficulty)) {
                    continue;
                }
                if (enforceRepeatCap && repeats >= maxRepeats && bucket(q) == 2) {
                    continue;
                }
                usable.add(q);
            }
            if (usable.isEmpty()) {
                return null;
            }
            Collections.shuffle(usable, random); // random order among equals; min() keeps the first of equals
            return usable.stream().min(Comparator
                    .comparingInt(this::bucket)
                    .thenComparingInt(q -> -secByQuestion.get(q.getQuestionId()).weight)
                    .thenComparingInt(q -> secByQuestion.get(q.getQuestionId()).order)
                    .thenComparingInt(this::stylePenalty)).orElse(null);
        }

        void add(Question q) {
            chosen.add(q);
            chosenIds.add(q.getQuestionId());
            if (bucket(q) == 2) {
                repeats++;
            }
            if (q.getQuestionStyle() != null && !q.getQuestionStyle().isBlank()) {
                styleCounts.merge(q.getQuestionStyle(), 1, Integer::sum);
            }
        }
    }
}
