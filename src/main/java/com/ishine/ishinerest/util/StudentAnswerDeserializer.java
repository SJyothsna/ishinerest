package com.ishine.ishinerest.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer to handle studentAnswer field that can be either:
 * - A string: "True" or "A" or "B,C,E"
 * - An array: ["Integers (ℤ)", "Rational number (ℚ )"]
 * 
 * Converts arrays to comma-separated strings for storage.
 */
public class StudentAnswerDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node.isArray()) {
            // Convert array to comma-separated string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(node.get(i).asText());
            }
            return sb.toString();
        } else if (node.isTextual()) {
            // Already a string, return as-is
            return node.asText();
        } else if (node.isNull()) {
            return null;
        } else {
            // For any other type, convert to string
            return node.asText();
        }
    }
}

// Made with Bob
