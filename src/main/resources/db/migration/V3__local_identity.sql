-- Temporary local identity adapter. Firebase replaces the adapter, not student IDs or history.
ALTER TABLE app_users ADD COLUMN account_role text NOT NULL DEFAULT 'STUDENT' CHECK(account_role IN ('STUDENT','PARENT'));
CREATE TABLE local_credentials(user_id uuid PRIMARY KEY REFERENCES app_users(user_id),password_hash text NOT NULL);
CREATE TABLE auth_sessions(token_hash text PRIMARY KEY,user_id uuid NOT NULL REFERENCES app_users(user_id),expires_at timestamp with time zone NOT NULL);
CREATE INDEX idx_auth_sessions_expiry ON auth_sessions(expires_at);
