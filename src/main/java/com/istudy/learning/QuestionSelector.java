package com.istudy.learning;

import static com.istudy.learning.LearningModels.*;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class QuestionSelector {

  public List<Candidate> sample(List<Candidate> pool, int count) {
    var unique = new LinkedHashMap<UUID, Candidate>();
    pool.forEach(c -> unique.putIfAbsent(c.questionId(), c));
    var shuffled = new ArrayList<>(unique.values());
    Collections.shuffle(shuffled);
    var selected = new ArrayList<>(
      shuffled.subList(0, Math.min(count, shuffled.size()))
    );
    selected.sort(
      Comparator.comparingInt(c ->
        switch (c.snapshot().path("difficulty").asText()) {
          case "FOUNDATION" -> 0;
          case "INTERMEDIATE" -> 1;
          default -> 2;
        }
      )
    );
    return selected;
  }

  public List<Candidate> select(List<Candidate> pool, int count) {
    var unique = new LinkedHashMap<UUID, Candidate>();
    pool
      .stream()
      .sorted(
        Comparator.comparingInt(Candidate::attempts).thenComparing(c ->
          c.questionId().toString()
        )
      )
      .forEach(c -> unique.putIfAbsent(c.questionId(), c));
    var sections = new LinkedHashMap<UUID, List<Candidate>>();
    unique
      .values()
      .forEach(c ->
        sections.computeIfAbsent(c.sectionId(), key -> new ArrayList<>()).add(c)
      );
    // Balance sections first. Existing mistakes can raise a section's selection priority,
    // but do not become a persistent label or prerequisite for the learner.
    var groups = new ArrayList<>(sections.values());
    groups.sort(
      Comparator.<List<Candidate>>comparingDouble(group -> {
        int total = group.stream().mapToInt(Candidate::attempts).sum();
        int correct = group.stream().mapToInt(Candidate::correct).sum();
        return total == 0 ? 0.5 : (double) correct / total;
      }).thenComparingInt(group ->
        group.stream().mapToInt(Candidate::attempts).sum()
      )
    );
    var queues = new ArrayList<ArrayDeque<Candidate>>();
    for (var group : groups) {
      var levels = new LinkedHashMap<String, ArrayDeque<Candidate>>();
      group.forEach(c ->
        levels
          .computeIfAbsent(
            c.snapshot().path("difficulty").asText("FOUNDATION"),
            key -> new ArrayDeque<>()
          )
          .add(c)
      );
      var mixed = new ArrayDeque<Candidate>();
      while (
        levels.values().stream().anyMatch(q -> !q.isEmpty())
      ) for (var queue : levels.values()) if (!queue.isEmpty()) mixed.add(
        queue.removeFirst()
      );
      queues.add(mixed);
    }
    var result = new ArrayList<Candidate>();
    while (result.size() < count) {
      boolean added = false;
      for (var queue : queues) if (!queue.isEmpty() && result.size() < count) {
        result.add(queue.removeFirst());
        added = true;
      }
      if (!added) break;
    }
    return result;
  }
}
