ALTER TABLE media_assets ADD COLUMN owner_user_id uuid REFERENCES app_users(user_id);
CREATE TABLE note_attachments(note_id uuid NOT NULL REFERENCES study_notes(note_id) ON DELETE CASCADE,asset_id uuid NOT NULL REFERENCES media_assets(asset_id),sort_order integer NOT NULL DEFAULT 0,PRIMARY KEY(note_id,asset_id));
