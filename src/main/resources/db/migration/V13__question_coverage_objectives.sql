CREATE TABLE learning_objectives (
  objective_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
  section_id uuid NOT NULL REFERENCES sections(section_id),
  code text NOT NULL,
  title text NOT NULL,
  required boolean NOT NULL DEFAULT true,
  UNIQUE (section_id, code),
  UNIQUE (objective_id, section_id)
);

ALTER TABLE question_placements ADD COLUMN objective_id uuid;
ALTER TABLE question_placements ADD COLUMN family_code text;
ALTER TABLE question_placements ADD CONSTRAINT question_placement_objective_scope
  FOREIGN KEY (objective_id, section_id) REFERENCES learning_objectives(objective_id, section_id);
ALTER TABLE question_placements ADD CONSTRAINT question_placement_complete_mapping
  CHECK ((objective_id IS NULL AND family_code IS NULL) OR
         (objective_id IS NOT NULL AND family_code IS NOT NULL AND char_length(family_code) > 0));
CREATE INDEX idx_question_placement_objective ON question_placements(objective_id);

ALTER TABLE content_documents ADD COLUMN bank_offering_id uuid REFERENCES subject_offerings(offering_id);
