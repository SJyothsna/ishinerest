-- Session review flags, immutable first checks, and question-specific issue reports.
ALTER TABLE session_questions ADD COLUMN review_flag boolean NOT NULL DEFAULT false;
CREATE TABLE session_answer_checks (
 session_question_id uuid PRIMARY KEY REFERENCES session_questions(session_question_id),
 first_response_payload text NOT NULL,
 is_correct boolean NOT NULL,
 checked_at timestamp with time zone NOT NULL DEFAULT current_timestamp
);
CREATE TABLE question_issue_reports (
 reviewed boolean NOT NULL DEFAULT false,
 session_question_id uuid NOT NULL REFERENCES session_questions(session_question_id),
 reason text NOT NULL CHECK(reason IN ('INCORRECT_ANSWER','UNCLEAR_WORDING','MISSING_INFORMATION')),
 created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,
 PRIMARY KEY(session_question_id,reason)
);
