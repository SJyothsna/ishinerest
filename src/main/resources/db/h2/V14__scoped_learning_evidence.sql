-- Session selection context is immutable. Older sessions have no recorded objective or effective difficulty.
ALTER TABLE session_questions ADD COLUMN objective_id uuid REFERENCES learning_objectives(objective_id);
ALTER TABLE session_questions ADD COLUMN family_code text;
ALTER TABLE session_questions ADD COLUMN effective_difficulty text CHECK(effective_difficulty IN ('FOUNDATION','INTERMEDIATE','ADVANCED'));
ALTER TABLE session_questions ADD COLUMN previously_seen boolean;
ALTER TABLE session_questions ADD COLUMN family_previously_seen boolean;

ALTER TABLE practice_sessions ADD COLUMN revised_mode text
  CHECK(revised_mode IN ('QUICK','FLAGGED','MISTAKES','PAST_QUESTIONS','PERSONALIZED'));
UPDATE practice_sessions SET revised_mode=practice_mode;
ALTER TABLE practice_sessions DROP COLUMN practice_mode;
ALTER TABLE practice_sessions RENAME COLUMN revised_mode TO practice_mode;

CREATE INDEX learning_evidence_session_idx ON practice_sessions(student_id,offering_id,status);
CREATE INDEX learning_evidence_question_idx ON session_questions(question_id);

-- Scope saved flags and accumulated outcomes to the course, even when a canonical question is shared.
ALTER TABLE student_question_flags RENAME TO legacy_unscoped_question_flags;
CREATE TABLE student_question_flags (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  offering_id uuid NOT NULL REFERENCES subject_offerings(offering_id),
  question_id uuid NOT NULL REFERENCES questions(question_id),
  flagged_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,offering_id,question_id)
);
INSERT INTO student_question_flags(student_id,offering_id,question_id,flagged_at)
SELECT DISTINCT f.student_id,c.offering_id,f.question_id,f.flagged_at
FROM legacy_unscoped_question_flags f
JOIN question_placements p ON p.question_id=f.question_id
JOIN sections s ON s.section_id=p.section_id
JOIN chapters c ON c.chapter_id=s.chapter_id
JOIN student_subjects ss ON ss.student_id=f.student_id AND ss.offering_id=c.offering_id;

DROP TABLE student_question_stats;
CREATE TABLE student_question_stats (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  offering_id uuid NOT NULL REFERENCES subject_offerings(offering_id),
  question_id uuid NOT NULL REFERENCES questions(question_id),
  total_attempts integer NOT NULL CHECK(total_attempts>=0),
  correct_attempts integer NOT NULL CHECK(correct_attempts>=0),
  last_attempted_at timestamp with time zone NOT NULL,
  PRIMARY KEY(student_id,offering_id,question_id)
);
INSERT INTO student_question_stats(student_id,offering_id,question_id,total_attempts,correct_attempts,last_attempted_at)
SELECT a.student_id,ps.offering_id,sq.question_id,count(*),sum(case when coalesce(ck.is_correct,a.is_correct) then 1 else 0 end),max(a.submitted_at)
FROM question_attempts a
JOIN session_questions sq ON sq.session_question_id=a.session_question_id
JOIN practice_sessions ps ON ps.session_id=sq.session_id
LEFT JOIN session_answer_checks ck ON ck.session_question_id=sq.session_question_id
WHERE ps.offering_id IS NOT NULL AND a.grading_version<>'self-review-v1'
GROUP BY a.student_id,ps.offering_id,sq.question_id;
