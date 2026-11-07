package com.ishine.ishinerest.questiongeneration.ai;

import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsRequest;
import com.ishine.ishinerest.questiongeneration.dto.PatternSelection;
import org.springframework.stereotype.Component;

/**
 * Builds the text prompts sent to the AI.
 * Keeping prompts here (not in services) means they can be tuned independently
 * of business logic and swapped per provider without touching service code.
 */
@Component
public class QuestionGenerationPromptBuilder {

    // ─── Analyse ──────────────────────────────────────────────────────────────

    public String buildAnalyseSystemPrompt() {
        return """
                You are an expert mathematics curriculum analyst specialising in Junior Cycle and \
                Leaving Certificate Irish mathematics.

                When given images of textbook or reference material:
                1. Identify the distinct question PATTERNS present (e.g. "Fundamental Principle of \
                Counting", "Listing Possible Outcomes", "Two-way Tables").
                2. For each pattern describe it in one sentence.
                3. Suggest a difficulty level using ONLY one of these exact values: Easy, Medium, Hard.
                4. Indicate whether a diagram or visual aid is typically required for that pattern.

                Return ONLY valid JSON — no markdown fences, no prose — matching this schema exactly:
                {
                  "patterns": [
                    {
                      "id": "SNAKE_CASE_ID",
                      "name": "Human readable name",
                      "description": "One sentence description.",
                      "suggestedDifficulty": "Easy|Medium|Hard",
                      "requiresVisual": true|false
                    }
                  ]
                }
                """;
    }

    public String buildAnalyseUserPrompt(String subjectId, String chapterId) {
        return String.format(
                "Analyse these reference pages from subject '%s', chapter '%s'. " +
                "Identify all distinct question patterns present and return JSON as instructed.",
                subjectId != null ? subjectId : "unknown subject",
                chapterId != null ? chapterId : "unknown chapter"
        );
    }

    // ─── Generate batch ───────────────────────────────────────────────────────

    public String buildGenerateSystemPrompt() {
        return """
                You are an expert Irish Junior Cycle mathematics question author.

                RULES — follow all of them:
                • Use the uploaded reference images as source material ONLY — to understand \
                  concepts, solving methods, and question structures.
                • Create NEW, ORIGINAL questions that test the same skills.
                • Do NOT copy source questions verbatim.
                • Do NOT merely replace a single number or name in a copied question.
                • Use different values, contexts, and wording than the source.
                • All questions must be mathematically correct.

                QUESTION TYPE SELECTION — follow these rules strictly:
                • Use questionType 2 (Blanks) when the answer is a plain number or short \
                  numeric value (e.g. 12, 24, 6). These are straightforward counting/calculation \
                  results. Set correctAnswerBlanks to the numeric answer as a string (digits only, \
                  no units — e.g. "40" not "40%").
                • CRITICAL for Blanks: embed the unit or format expectation directly inside \
                  questionText so the student knows exactly what to write. Use a fill-in template \
                  with $\\underline{\\hspace{1.5cm}}$ followed by the unit label. Examples:
                    - percentage answer:  "Express $24$ out of $60$ as a percentage. $\\underline{\\hspace{1.5cm}}$%"
                    - count answer:       "How many outcomes are possible? $\\underline{\\hspace{1.5cm}}$ outcomes"
                    - euro answer:        "Calculate the total cost. €$\\underline{\\hspace{1.5cm}}$"
                  Never ask a bare "Calculate ..." without the unit template in the question text.
                • Use questionType 1 (Choices/MCQ) when the question involves mathematical \
                  expressions, fractions, algebraic notation, or when selecting from plausible \
                  distractors adds educational value. Provide exactly 4 options (A, B, C, D) \
                  with exactly one correct answer and plausible distractors.

                IMAGE RULE:
                • Set requiresImage to true ONLY if the question fundamentally cannot be \
                  understood without a diagram (e.g. spinner, two-way table, tree diagram). \
                  Set visualDescription to a clear plain-English description of the required \
                  diagram. For all other questions set requiresImage to false.

                NOTATION:
                • Use KaTeX notation for ALL mathematical expressions in questionText, \
                  explanation, hint, and options: e.g. $\\frac{3}{4}$, $4 \\times 3 = 12$, \
                  $\\underline{\\hspace{1.5cm}}$ for blanks in fill-in questions.
                • Provide a clear, age-appropriate explanation with KaTeX where needed.
                • The difficulty field must be exactly one of: Easy, Medium, Hard.
                • The usageType field must be exactly one of: Practice, Test, Both.

                Return ONLY valid JSON — no markdown fences, no prose — matching this schema:
                {
                  "questions": [
                    {
                      "questionText": "string",
                      "questionType": 1,
                      "optionA": "string or null",
                      "optionB": "string or null",
                      "optionC": "string or null",
                      "optionD": "string or null",
                      "correctAnswer": "A|B|C|D or null",
                      "correctAnswerBlanks": "string or null",
                      "hint": "string",
                      "explanation": "string",
                      "difficultyLevel": "Easy|Medium|Hard",
                      "usageType": "Practice|Test|Both",
                      "questionSet": "1",
                      "questionPattern": "string",
                      "requiresImage": false,
                      "visualDescription": "string or null"
                    }
                  ]
                }
                """;
    }

    public String buildGenerateUserPrompt(GenerateQuestionsRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate questions for the following patterns using the provided reference images.\n\n");

        for (PatternSelection p : request.getPatterns()) {
            sb.append(String.format(
                    "• Pattern: \"%s\" — generate %d question(s) at difficulty \"%s\".\n",
                    p.getPatternName(), p.getCount(), p.getDifficulty()
            ));
        }

        sb.append(String.format(
                "\nContext: subjectId=%s, chapterId=%s, sectionId=%s, usageType=%s, questionSet=%s.",
                request.getSubjectId(), request.getChapterId(), request.getSectionId(),
                request.getUsageType(), request.getQuestionSet()
        ));

        if (request.getAdditionalInstructions() != null && !request.getAdditionalInstructions().isBlank()) {
            sb.append("\n\nAdditional instructions from admin: ")
              .append(request.getAdditionalInstructions());
        }

        return sb.toString();
    }

    // ─── Regenerate single question ───────────────────────────────────────────

    public String buildRegenerateSystemPrompt() {
        return buildGenerateSystemPrompt(); // same rules apply
    }

    public String buildRegenerateUserPrompt(
            String patternName,
            String difficulty,
            String previousQuestionText,
            GenerateQuestionsRequest context
    ) {
        return String.format(
                "Regenerate ONE question for the pattern \"%s\" at difficulty \"%s\".\n" +
                "Do NOT reproduce this previous question: \"%s\"\n" +
                "Context: subjectId=%s, chapterId=%s, sectionId=%s, usageType=%s, questionSet=%s.\n" +
                "Return a single-element JSON array under the key \"questions\".",
                patternName, difficulty, previousQuestionText,
                context.getSubjectId(), context.getChapterId(), context.getSectionId(),
                context.getUsageType(), context.getQuestionSet()
        );
    }
}
