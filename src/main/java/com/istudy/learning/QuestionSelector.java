package com.istudy.learning;

import static com.istudy.learning.LearningModels.*;

import com.istudy.learning.LearningProfileService.Evidence;
import com.istudy.learning.LearningProfileService.Status;

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
        switch (c.difficulty()) {
          case "FOUNDATION" -> 0;
          case "INTERMEDIATE" -> 1;
          default -> 2;
        }
      )
    );
    return selected;
  }

  public List<Candidate> select(
    List<Candidate> pool,
    int count,
    Set<String> selectedFamilies,
    Map<UUID, Evidence> evidence,
    boolean balancedTest
  ) {
    var unique = new LinkedHashMap<UUID, Candidate>();
    pool
      .stream()
      .sorted(
        Comparator.comparing((Candidate c) -> c.familySeen())
          .thenComparingInt(c -> LearningProfileService.priority(
            evidence.get(c.coverageKey())
          ))
          .thenComparingInt(Candidate::attempts)
          .thenComparing(Candidate::questionId)
      )
      .forEach(c -> unique.putIfAbsent(c.questionId(), c));
    var sections = new LinkedHashMap<UUID, List<Candidate>>();
    unique
      .values()
      .forEach(c ->
        sections.computeIfAbsent(c.coverageKey(), key -> new ArrayList<>()).add(c)
      );
    var groups = new ArrayList<>(sections.values());
    if (evidence.isEmpty()) {
      groups.sort(Comparator.<List<Candidate>>comparingDouble(group -> {
        int total = group.stream().mapToInt(Candidate::attempts).sum();
        int correct = group.stream().mapToInt(Candidate::correct).sum();
        return total == 0 ? 0.5 : (double) correct / total;
      }).thenComparingInt(group -> group.stream().mapToInt(Candidate::attempts).sum()));
    } else {
      groups.sort(Comparator.comparingInt(group -> LearningProfileService.priority(
        evidence.get(group.get(0).coverageKey())
      )));
      if (balancedTest) {
        // Take a turn from each kind of evidence before filling from one category.
        var categories = new EnumMap<Status, ArrayDeque<List<Candidate>>>(Status.class);
        for (var group : groups) {
          Evidence item = evidence.get(group.get(0).coverageKey());
          Status status = item == null ? Status.UNASSESSED : item.status();
          categories.computeIfAbsent(status, key -> new ArrayDeque<>()).add(group);
        }
        groups.clear();
        var order = List.of(Status.NEEDS_PRACTICE, Status.STRENGTH, Status.UNASSESSED, Status.DEVELOPING);
        while (categories.values().stream().anyMatch(queue -> !queue.isEmpty())) {
          for (Status status : order) {
            var category = categories.get(status);
            if (category != null && !category.isEmpty()) groups.add(category.removeFirst());
          }
        }
      }
    }
    var queues = new ArrayList<ArrayDeque<Candidate>>();
    for (var group : groups) {
      var levels = new LinkedHashMap<String, ArrayDeque<Candidate>>();
      group.forEach(c ->
        levels
          .computeIfAbsent(
            c.difficulty(),
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
    for (boolean allowRelated : List.of(false, true)) {
      while (result.size() < count) {
        boolean added = false;
        for (var queue : queues) {
          if (result.size() == count) break;
          var choices = queue.iterator();
          while (choices.hasNext()) {
            Candidate candidate = choices.next();
            if (!allowRelated && selectedFamilies.contains(candidate.familyKey())) continue;
            result.add(candidate);
            selectedFamilies.add(candidate.familyKey());
            choices.remove();
            added = true;
            break;
          }
        }
        if (!added) break;
      }
    }
    return result;
  }
}
