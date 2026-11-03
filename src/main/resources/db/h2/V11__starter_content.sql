ALTER TABLE subject_offerings ADD COLUMN is_starter boolean NOT NULL DEFAULT false;
ALTER TABLE question_placements ADD COLUMN is_starter boolean NOT NULL DEFAULT false;
CREATE TABLE starter_content_runs(seed_key text PRIMARY KEY,applied_at timestamp with time zone NOT NULL DEFAULT current_timestamp);
ALTER TABLE study_notes ADD COLUMN is_starter boolean NOT NULL DEFAULT false;
ALTER TABLE past_papers ADD COLUMN exam_level text;
UPDATE past_papers SET exam_level='Higher Level' WHERE paper_code LIKE 'LC01H:%';
UPDATE past_papers SET exam_level='Ordinary Level' WHERE paper_code LIKE 'LC01O:%';
