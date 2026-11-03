ALTER TABLE app_users ADD COLUMN content_admin boolean NOT NULL DEFAULT false;
ALTER TABLE practice_sessions ADD COLUMN repeated_count integer NOT NULL DEFAULT 0 CHECK(repeated_count>=0);
ALTER TABLE content_jobs ADD COLUMN generation_key text;
ALTER TABLE content_jobs ADD COLUMN chapter_id uuid REFERENCES chapters(chapter_id);
ALTER TABLE content_jobs ADD COLUMN next_attempt_at timestamp with time zone;
CREATE UNIQUE INDEX uq_generation_job_key ON content_jobs(generation_key);
