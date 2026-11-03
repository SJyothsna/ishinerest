package com.istudy.learning;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Grader {

  public boolean correct(JsonNode snapshot, JsonNode response) {
    if (response == null || response.isNull()) return false;
    String type = snapshot.path("type").asText();
    if (type.equals("worked")) return (
      response.isObject() && response.path("understood").asBoolean(false)
    );
    if (type.equals("multiple")) {
      if (!response.isArray()) return false;
      return values(snapshot.path("correctAnswers")).equals(values(response));
    }
    if (
      type.equals("text") &&
      snapshot.path("acceptedAnswers").isArray() &&
      response.isValueNode()
    ) for (var accepted : snapshot.path("acceptedAnswers")) if (
      accepted.asText().trim().equalsIgnoreCase(response.asText().trim())
    ) return true;
    String expected = snapshot.path("correctAnswer").asText("").trim();
    return (
      !expected.isEmpty() &&
      response.isValueNode() &&
      expected.equalsIgnoreCase(response.asText().trim())
    );
  }

  private Set<String> values(JsonNode array) {
    var set = new HashSet<String>();
    array.forEach(v -> set.add(v.asText().trim()));
    return set;
  }
}
