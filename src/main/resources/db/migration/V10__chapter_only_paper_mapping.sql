-- Preserve chapter-level provenance without inventing a topic mapping for old paper images.
CREATE TABLE past_paper_question_chapters(paper_question_id uuid NOT NULL REFERENCES past_paper_questions(paper_question_id),chapter_id uuid NOT NULL REFERENCES chapters(chapter_id),PRIMARY KEY(paper_question_id,chapter_id));
INSERT INTO past_paper_question_chapters(paper_question_id,chapter_id) SELECT DISTINCT pq.paper_question_id,s.chapter_id FROM past_paper_question_sections pq JOIN sections s ON s.section_id=pq.section_id WHERE s.code='past-questions' AND NOT EXISTS(SELECT 1 FROM question_placements qp WHERE qp.section_id=s.section_id);
DELETE FROM past_paper_question_sections WHERE section_id IN(SELECT s.section_id FROM sections s WHERE s.code='past-questions' AND NOT EXISTS(SELECT 1 FROM question_placements qp WHERE qp.section_id=s.section_id));
DELETE FROM sections WHERE code='past-questions' AND NOT EXISTS(SELECT 1 FROM question_placements qp WHERE qp.section_id=sections.section_id);
