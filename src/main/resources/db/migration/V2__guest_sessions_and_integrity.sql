-- Guests use the same frozen-question engine; ownership is an opaque cookie, never an IP address.
CREATE TABLE guest_principals (
 guest_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 secret_hash text NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL,
 claimed_by uuid REFERENCES student_profiles(student_id),
 claimed_at timestamptz
);
ALTER TABLE practice_sessions ALTER COLUMN student_id DROP NOT NULL;
ALTER TABLE practice_sessions ADD COLUMN guest_id uuid REFERENCES guest_principals(guest_id);
ALTER TABLE practice_sessions ADD CONSTRAINT session_owner CHECK ((student_id IS NOT NULL) <> (guest_id IS NOT NULL));
ALTER TABLE practice_sessions ADD COLUMN result_payload jsonb;
ALTER TABLE practice_sessions ADD COLUMN claimed_by uuid REFERENCES student_profiles(student_id);
ALTER TABLE practice_sessions DROP CONSTRAINT practice_sessions_session_kind_check;
ALTER TABLE practice_sessions ADD CONSTRAINT session_kind CHECK(session_kind IN ('PRACTICE','TEST','INITIAL_ASSESSMENT','PAST_PAPER','SAMPLE'));
ALTER TABLE study_notes ADD COLUMN color text NOT NULL DEFAULT '#FFF9C4';
CREATE TABLE feedback_messages (
 feedback_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 user_id uuid REFERENCES app_users(user_id),
 name text NOT NULL,email text NOT NULL,kind text NOT NULL,subject text NOT NULL,message text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
-- Enforce append-only evidence even when application code changes.
CREATE FUNCTION reject_immutable_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION '% is immutable', TG_TABLE_NAME; END; $$;
CREATE TRIGGER attempts_immutable BEFORE UPDATE OR DELETE ON question_attempts FOR EACH ROW EXECUTE FUNCTION reject_immutable_change();
CREATE TRIGGER revisions_immutable BEFORE UPDATE OR DELETE ON question_revisions FOR EACH ROW EXECUTE FUNCTION reject_immutable_change();
