CREATE TABLE past_paper_marking_schemes(paper_id uuid PRIMARY KEY REFERENCES past_papers(paper_id),document_id uuid NOT NULL REFERENCES content_documents(document_id));
-- An extracted occurrence may be available even while its original full paper is missing.
ALTER TABLE past_papers ALTER COLUMN document_id DROP NOT NULL;
CREATE TABLE past_paper_question_assets(paper_question_id uuid NOT NULL REFERENCES past_paper_questions(paper_question_id),asset_id uuid NOT NULL REFERENCES media_assets(asset_id),asset_kind text NOT NULL CHECK(asset_kind IN ('QUESTION','MARKING_SCHEME')),sort_order integer NOT NULL,PRIMARY KEY(paper_question_id,asset_id,asset_kind));
