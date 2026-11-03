-- Fresh PostgreSQL database only. Derived from the supplied architecture; no legacy schema changes.
CREATE TABLE curricula (
  curriculum_id uuid DEFAULT random_uuid() PRIMARY KEY,
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  country_code char(2),
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE academic_levels (
  level_id uuid DEFAULT random_uuid() PRIMARY KEY,
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  sort_order integer NOT NULL DEFAULT 0
);

CREATE TABLE subjects (
  subject_id uuid DEFAULT random_uuid() PRIMARY KEY,
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE subject_offerings (
  offering_id uuid DEFAULT random_uuid() PRIMARY KEY,
  curriculum_id uuid NOT NULL REFERENCES curricula(curriculum_id),
  level_id uuid NOT NULL REFERENCES academic_levels(level_id),
  subject_id uuid NOT NULL REFERENCES subjects(subject_id),
  track_code text NOT NULL DEFAULT 'STANDARD',
  syllabus_version text NOT NULL,
  status text NOT NULL DEFAULT 'DRAFT'
    CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
  effective_from date,
  effective_to date,
  UNIQUE(curriculum_id, level_id, subject_id, track_code, syllabus_version),
  CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE TABLE chapters (
  chapter_id uuid DEFAULT random_uuid() PRIMARY KEY,
  offering_id uuid NOT NULL REFERENCES subject_offerings(offering_id),
  code text NOT NULL,
  title text NOT NULL,
  description text,
  sort_order integer NOT NULL DEFAULT 0,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
  UNIQUE(offering_id,code),
  UNIQUE(chapter_id,offering_id)
);

CREATE TABLE sections (
  section_id uuid DEFAULT random_uuid() PRIMARY KEY,
  chapter_id uuid NOT NULL REFERENCES chapters(chapter_id),
  parent_section_id uuid REFERENCES sections(section_id),
  code text NOT NULL,
  title text NOT NULL,
  sort_order integer NOT NULL DEFAULT 0,
  selection_priority smallint NOT NULL DEFAULT 3 CHECK(selection_priority BETWEEN 1 AND 5),
  status text NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE')),
  UNIQUE(chapter_id,code),
  UNIQUE(section_id,chapter_id)
);

CREATE TABLE exam_programmes (
  exam_id uuid DEFAULT random_uuid() PRIMARY KEY,
  code text NOT NULL UNIQUE,
  name text NOT NULL,
  organisation text,
  region text,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE exam_syllabus_versions (
  exam_syllabus_version_id uuid DEFAULT random_uuid() PRIMARY KEY,
  exam_id uuid NOT NULL REFERENCES exam_programmes(exam_id),
  version_code text NOT NULL,
  effective_from date,
  effective_to date,
  status text NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),
  UNIQUE(exam_id,version_code),
  CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE TABLE exam_syllabus_sections (
  exam_syllabus_version_id uuid NOT NULL REFERENCES exam_syllabus_versions(exam_syllabus_version_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  weight numeric(6,3),
  PRIMARY KEY(exam_syllabus_version_id,section_id),
  CHECK(weight IS NULL OR weight >= 0)
);

CREATE TABLE app_users (
  user_id uuid DEFAULT random_uuid() PRIMARY KEY,
  email text,
  email_verified boolean NOT NULL DEFAULT false,
  display_name text,
  status text NOT NULL DEFAULT 'ACTIVE'
    CHECK(status IN ('ACTIVE','SUSPENDED','DELETED')),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE auth_identities (
  identity_id uuid DEFAULT random_uuid() PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES app_users(user_id),
  provider text NOT NULL,
  provider_subject text NOT NULL,
  linked_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE(provider,provider_subject)
);

CREATE TABLE student_profiles (
  student_id uuid PRIMARY KEY REFERENCES app_users(user_id),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  learning_preferences text NOT NULL DEFAULT '{}'
);

CREATE TABLE institutions (
  institution_id uuid DEFAULT random_uuid() PRIMARY KEY,
  institution_type text NOT NULL CHECK(institution_type IN ('SCHOOL','COLLEGE','OTHER')),
  name text NOT NULL,
  country_code char(2),
  status text NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE institution_memberships (
  membership_id uuid DEFAULT random_uuid() PRIMARY KEY,
  institution_id uuid NOT NULL REFERENCES institutions(institution_id),
  user_id uuid NOT NULL REFERENCES app_users(user_id),
  role text NOT NULL CHECK(role IN ('STUDENT','TEACHER','ADMIN')),
  status text NOT NULL DEFAULT 'PENDING'
    CHECK(status IN ('PENDING','VERIFIED','REVOKED')),
  verified_at timestamp with time zone,
  verified_by uuid REFERENCES app_users(user_id),
  UNIQUE(institution_id,user_id,role),
  CHECK(status <> 'VERIFIED' OR verified_at IS NOT NULL)
);

CREATE TABLE parent_student_links (
  parent_user_id uuid NOT NULL REFERENCES app_users(user_id),
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  status text NOT NULL DEFAULT 'PENDING'
    CHECK(status IN ('PENDING','VERIFIED','REVOKED')),
  verified_at timestamp with time zone,
  PRIMARY KEY(parent_user_id,student_id),
  CHECK(parent_user_id <> student_id)
);

CREATE TABLE student_subjects (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  offering_id uuid NOT NULL REFERENCES subject_offerings(offering_id),
  status text NOT NULL DEFAULT 'ENROLLED'
    CHECK(status IN ('ENROLLED','ARCHIVED')),
  enrolled_at timestamp with time zone NOT NULL DEFAULT now(),
  started_at timestamp with time zone,
  last_activity_at timestamp with time zone,
  last_chapter_id uuid REFERENCES chapters(chapter_id),
  PRIMARY KEY(student_id,offering_id)
);

CREATE TABLE media_assets (
  asset_id uuid DEFAULT random_uuid() PRIMARY KEY,
  storage_provider text NOT NULL DEFAULT 'S3',
  bucket_name text NOT NULL,
  object_key text NOT NULL,
  object_version text,
  content_type text NOT NULL,
  byte_size bigint CHECK(byte_size IS NULL OR byte_size >= 0),
  sha256 text,
  access_level text NOT NULL DEFAULT 'PRIVATE'
    CHECK(access_level IN ('PRIVATE','CONTROLLED_PUBLIC')),
  scan_status text NOT NULL DEFAULT 'PENDING'
    CHECK(scan_status IN ('PENDING','CLEAN','REJECTED')),
  licence_note text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE(storage_provider,bucket_name,object_key)
);

CREATE TABLE content_documents (
  document_id uuid DEFAULT random_uuid() PRIMARY KEY,
  asset_id uuid NOT NULL REFERENCES media_assets(asset_id),
  document_type text NOT NULL
    CHECK(document_type IN ('TEXTBOOK','EXAM_PAPER','MARK_SCHEME','OTHER')),
  title text NOT NULL,
  owner_institution_id uuid REFERENCES institutions(institution_id),
  licence_status text NOT NULL DEFAULT 'UNREVIEWED'
    CHECK(licence_status IN ('UNREVIEWED','PERMITTED','RESTRICTED','REJECTED')),
  processing_status text NOT NULL DEFAULT 'UPLOADED'
    CHECK(processing_status IN ('UPLOADED','EXTRACTING','REVIEW','READY','FAILED')),
  content_version integer NOT NULL DEFAULT 1 CHECK(content_version>0),
  created_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE document_chunks (
  chunk_id uuid DEFAULT random_uuid() PRIMARY KEY,
  document_id uuid NOT NULL REFERENCES content_documents(document_id),
  chunk_ordinal integer NOT NULL CHECK(chunk_ordinal>=0),
  page_start integer,
  page_end integer,
  extracted_text text NOT NULL,
  extraction_version text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE(document_id,chunk_ordinal),
  CHECK(page_end IS NULL OR page_start IS NULL OR page_end>=page_start)
);

CREATE TABLE document_section_mappings (
  mapping_id uuid DEFAULT random_uuid() PRIMARY KEY,
  document_id uuid NOT NULL REFERENCES content_documents(document_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  page_start integer,
  page_end integer,
  review_status text NOT NULL DEFAULT 'PENDING'
    CHECK(review_status IN ('PENDING','APPROVED','REJECTED')),
  UNIQUE(document_id,section_id,page_start)
);

CREATE TABLE content_jobs (
  job_id uuid DEFAULT random_uuid() PRIMARY KEY,
  document_id uuid REFERENCES content_documents(document_id),
  operation text NOT NULL
    CHECK(operation IN ('EXTRACT','CHUNK','MAP_TOPICS','GENERATE_QUESTIONS','GENERATE_AUDIO','EMBED')),
  status text NOT NULL DEFAULT 'QUEUED'
    CHECK(status IN ('QUEUED','RUNNING','REVIEW','COMPLETED','FAILED')),
  input_parameters text NOT NULL DEFAULT '{}',
  model_name text,
  prompt_version text,
  attempt_count smallint NOT NULL DEFAULT 0,
  error_code text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  started_at timestamp with time zone,
  completed_at timestamp with time zone
);

CREATE TABLE questions (
  question_id uuid DEFAULT random_uuid() PRIMARY KEY,
  question_text text NOT NULL,
  option_a text,
  option_b text,
  option_c text,
  option_d text,
  option_e text,
  option_f text,
  correct_answer text,
  correct_answers text,
  question_type text NOT NULL DEFAULT 'SINGLE_CHOICE',
  explanation text,
  hint text,
  difficulty_level text NOT NULL
    CHECK(difficulty_level IN ('FOUNDATION','INTERMEDIATE','ADVANCED')),
  tags text NOT NULL DEFAULT '[]',
  question_image_asset_id uuid REFERENCES media_assets(asset_id),
  creator_id uuid REFERENCES app_users(user_id),
  owner_institution_id uuid REFERENCES institutions(institution_id),
  visibility text NOT NULL DEFAULT 'GLOBAL'
    CHECK(visibility IN ('GLOBAL','INSTITUTION','PRIVATE')),
  approval_status text NOT NULL DEFAULT 'DRAFT'
    CHECK(approval_status IN ('DRAFT','IN_REVIEW','PUBLISHED','REJECTED','ARCHIVED')),
  current_revision integer NOT NULL DEFAULT 1 CHECK(current_revision>0),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE question_placements (
  placement_id uuid DEFAULT random_uuid() PRIMARY KEY,
  question_id uuid NOT NULL REFERENCES questions(question_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  usage_type text NOT NULL DEFAULT 'PRACTICE',
  question_set integer NOT NULL DEFAULT 1,
  active boolean NOT NULL DEFAULT true,
  difficulty_override text CHECK(difficulty_override IN ('FOUNDATION','INTERMEDIATE','ADVANCED')),
  UNIQUE(question_id,section_id,usage_type,question_set),
  UNIQUE(placement_id,question_id)
);

CREATE TABLE question_revisions (
  revision_id uuid DEFAULT random_uuid() PRIMARY KEY,
  question_id uuid NOT NULL REFERENCES questions(question_id),
  revision_number integer NOT NULL CHECK(revision_number>0),
  content_snapshot text NOT NULL,
  reviewed_by uuid REFERENCES app_users(user_id),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  UNIQUE(question_id,revision_number),
  UNIQUE(revision_id,question_id)
);

CREATE TABLE question_sources (
  source_id uuid DEFAULT random_uuid() PRIMARY KEY,
  question_id uuid NOT NULL REFERENCES questions(question_id),
  document_id uuid REFERENCES content_documents(document_id),
  chunk_id uuid REFERENCES document_chunks(chunk_id),
  page_number integer,
  generation_job_id uuid REFERENCES content_jobs(job_id),
  source_note text,
  CHECK(document_id IS NOT NULL OR generation_job_id IS NOT NULL OR source_note IS NOT NULL)
);

CREATE TABLE question_reviews (
  review_id uuid DEFAULT random_uuid() PRIMARY KEY,
  question_id uuid NOT NULL REFERENCES questions(question_id),
  revision_id uuid NOT NULL,
  reviewer_id uuid NOT NULL REFERENCES app_users(user_id),
  decision text NOT NULL CHECK(decision IN ('APPROVED','CHANGES_REQUESTED','REJECTED')),
  review_note text,
  reviewed_at timestamp with time zone NOT NULL DEFAULT now(),
  FOREIGN KEY(revision_id,question_id) REFERENCES question_revisions(revision_id,question_id)
);

CREATE TABLE past_papers (
  paper_id uuid DEFAULT random_uuid() PRIMARY KEY,
  exam_id uuid NOT NULL REFERENCES exam_programmes(exam_id),
  exam_syllabus_version_id uuid REFERENCES exam_syllabus_versions(exam_syllabus_version_id),
  document_id uuid NOT NULL REFERENCES content_documents(document_id),
  exam_year integer NOT NULL CHECK(exam_year BETWEEN 1900 AND 2200),
  exam_session text NOT NULL DEFAULT '',
  paper_code text NOT NULL,
  approval_status text NOT NULL DEFAULT 'DRAFT'
    CHECK(approval_status IN ('DRAFT','IN_REVIEW','PUBLISHED','ARCHIVED')),
  UNIQUE(exam_id,exam_year,exam_session,paper_code)
);

CREATE TABLE past_paper_offerings (
  paper_id uuid NOT NULL REFERENCES past_papers(paper_id),
  offering_id uuid NOT NULL REFERENCES subject_offerings(offering_id),
  PRIMARY KEY(paper_id,offering_id)
);

CREATE TABLE past_paper_questions (
  paper_question_id uuid DEFAULT random_uuid() PRIMARY KEY,
  paper_id uuid NOT NULL REFERENCES past_papers(paper_id),
  question_number text NOT NULL,
  part_label text NOT NULL DEFAULT '',
  question_id uuid REFERENCES questions(question_id),
  extracted_text text,
  page_number integer,
  marks numeric(7,2),
  review_status text NOT NULL DEFAULT 'PENDING'
    CHECK(review_status IN ('PENDING','APPROVED','REJECTED')),
  UNIQUE(paper_id,question_number,part_label)
);

CREATE TABLE past_paper_question_sections (
  paper_question_id uuid NOT NULL REFERENCES past_paper_questions(paper_question_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  PRIMARY KEY(paper_question_id,section_id)
);

CREATE TABLE practice_sessions (
  session_id uuid DEFAULT random_uuid() PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  offering_id uuid REFERENCES subject_offerings(offering_id),
  exam_syllabus_version_id uuid REFERENCES exam_syllabus_versions(exam_syllabus_version_id),
  paper_id uuid REFERENCES past_papers(paper_id),
  chapter_id uuid REFERENCES chapters(chapter_id),
  session_kind text NOT NULL
    CONSTRAINT practice_sessions_session_kind_check CHECK(session_kind IN ('PRACTICE','TEST','INITIAL_ASSESSMENT','PAST_PAPER')),
  practice_mode text CHECK(practice_mode IN ('QUICK','FLAGGED','MISTAKES','PAST_QUESTIONS')),
  scope_type text NOT NULL
    CHECK(scope_type IN ('SUBJECT_ALL','SUBJECT_STARTED','CHAPTER_ALL','SELECTED_SECTIONS','PAPER','EXAM')),
  difficulty_filter text CHECK(difficulty_filter IN ('ALL','FOUNDATION','INTERMEDIATE','ADVANCED')),
  requested_count integer NOT NULL CHECK(requested_count>0),
  selected_count integer NOT NULL DEFAULT 0 CHECK(selected_count>=0),
  timer_seconds integer CHECK(timer_seconds>0),
  status text NOT NULL DEFAULT 'CREATED'
    CHECK(status IN ('CREATED','IN_PROGRESS','SUBMITTED','EXPIRED','ABANDONED')),
  selection_version text NOT NULL DEFAULT 'v1',
  grading_version text NOT NULL DEFAULT 'v1',
  started_at timestamp with time zone,
  submitted_at timestamp with time zone,
  expires_at timestamp with time zone,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  submission_token uuid UNIQUE,
  CHECK(offering_id IS NOT NULL OR exam_syllabus_version_id IS NOT NULL OR paper_id IS NOT NULL)
);

CREATE TABLE practice_session_sections (
  session_id uuid NOT NULL REFERENCES practice_sessions(session_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  PRIMARY KEY(session_id,section_id)
);

CREATE TABLE session_questions (
  session_question_id uuid DEFAULT random_uuid() PRIMARY KEY,
  session_id uuid NOT NULL REFERENCES practice_sessions(session_id),
  question_id uuid NOT NULL REFERENCES questions(question_id),
  revision_id uuid NOT NULL,
  placement_id uuid,
  past_paper_question_id uuid REFERENCES past_paper_questions(paper_question_id),
  question_position integer NOT NULL CHECK(question_position>0),
  UNIQUE(session_id,question_position),
  UNIQUE(session_id,question_id),
  FOREIGN KEY(revision_id,question_id) REFERENCES question_revisions(revision_id,question_id),
  FOREIGN KEY(placement_id,question_id) REFERENCES question_placements(placement_id,question_id)
);

CREATE TABLE session_answer_drafts (
  session_question_id uuid PRIMARY KEY REFERENCES session_questions(session_question_id),
  response_payload text NOT NULL,
  saved_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE question_attempts (
  attempt_id uuid DEFAULT random_uuid() PRIMARY KEY,
  session_question_id uuid NOT NULL UNIQUE REFERENCES session_questions(session_question_id),
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  response_payload text NOT NULL,
  is_correct boolean NOT NULL,
  score numeric(8,3),
  time_spent_seconds integer CHECK(time_spent_seconds IS NULL OR time_spent_seconds>=0),
  grading_version text NOT NULL DEFAULT 'v1',
  submitted_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE student_question_flags (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  question_id uuid NOT NULL REFERENCES questions(question_id),
  flagged_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,question_id)
);

CREATE TABLE student_question_stats (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  question_id uuid NOT NULL REFERENCES questions(question_id),
  total_attempts integer NOT NULL DEFAULT 0 CHECK(total_attempts>=0),
  correct_attempts integer NOT NULL DEFAULT 0 CHECK(correct_attempts>=0),
  last_correct boolean,
  last_attempted_at timestamp with time zone,
  stats_version text NOT NULL DEFAULT 'v1',
  calculated_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,question_id)
);

CREATE TABLE student_section_stats (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  unique_questions_attempted integer NOT NULL DEFAULT 0 CHECK(unique_questions_attempted>=0),
  total_attempts integer NOT NULL DEFAULT 0 CHECK(total_attempts>=0),
  correct_attempts integer NOT NULL DEFAULT 0 CHECK(correct_attempts>=0),
  recent_attempts integer NOT NULL DEFAULT 0 CHECK(recent_attempts>=0),
  recent_correct integer NOT NULL DEFAULT 0 CHECK(recent_correct>=0),
  evidence_level text NOT NULL DEFAULT 'INSUFFICIENT'
    CHECK(evidence_level IN ('INSUFFICIENT','EMERGING','SUFFICIENT')),
  proficiency_band text NOT NULL DEFAULT 'UNKNOWN'
    CHECK(proficiency_band IN ('UNKNOWN','NEEDS_PRACTICE','DEVELOPING','ON_TRACK','STRONG')),
  last_practised_at timestamp with time zone,
  stats_version text NOT NULL DEFAULT 'v1',
  calculated_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,section_id)
);

CREATE TABLE student_chapter_stats (
  student_id uuid NOT NULL REFERENCES student_profiles(student_id),
  chapter_id uuid NOT NULL REFERENCES chapters(chapter_id),
  distinct_questions_attempted integer NOT NULL DEFAULT 0,
  total_attempts integer NOT NULL DEFAULT 0,
  correct_attempts integer NOT NULL DEFAULT 0,
  coverage_percent numeric(5,2),
  stats_version text NOT NULL DEFAULT 'v1',
  calculated_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,chapter_id),
  CHECK(coverage_percent IS NULL OR coverage_percent BETWEEN 0 AND 100)
);

CREATE TABLE student_subject_stats (
  student_id uuid NOT NULL,
  offering_id uuid NOT NULL,
  distinct_questions_attempted integer NOT NULL DEFAULT 0,
  eligible_question_count integer NOT NULL DEFAULT 0,
  coverage_percent numeric(5,2),
  total_attempts integer NOT NULL DEFAULT 0,
  correct_attempts integer NOT NULL DEFAULT 0,
  stats_version text NOT NULL DEFAULT 'v1',
  calculated_at timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY(student_id,offering_id),
  FOREIGN KEY(student_id,offering_id) REFERENCES student_subjects(student_id,offering_id),
  CHECK(coverage_percent IS NULL OR coverage_percent BETWEEN 0 AND 100)
);

CREATE TABLE study_notes (
  note_id uuid DEFAULT random_uuid() PRIMARY KEY,
  offering_id uuid REFERENCES subject_offerings(offering_id),
  chapter_id uuid REFERENCES chapters(chapter_id),
  section_id uuid REFERENCES sections(section_id),
  owner_user_id uuid REFERENCES app_users(user_id),
  note_type text NOT NULL CHECK(note_type IN ('GENERAL','STUDENT')),
  title text NOT NULL,
  content_text text,
  attachment_asset_id uuid REFERENCES media_assets(asset_id),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CHECK((note_type='STUDENT' AND owner_user_id IS NOT NULL)
      OR (note_type='GENERAL' AND owner_user_id IS NULL))
);

CREATE TABLE learning_videos (
  video_id uuid DEFAULT random_uuid() PRIMARY KEY,
  youtube_video_id text NOT NULL UNIQUE,
  title text NOT NULL,
  duration_seconds integer,
  status text NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','HIDDEN')),
  created_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE video_sections (
  video_id uuid NOT NULL REFERENCES learning_videos(video_id),
  section_id uuid NOT NULL REFERENCES sections(section_id),
  PRIMARY KEY(video_id,section_id)
);

CREATE TABLE outbox_events (
  event_id uuid DEFAULT random_uuid() PRIMARY KEY,
  aggregate_type text NOT NULL,
  aggregate_id uuid NOT NULL,
  event_type text NOT NULL,
  event_version integer NOT NULL DEFAULT 1,
  payload text NOT NULL DEFAULT '{}',
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  published_at timestamp with time zone,
  attempt_count integer NOT NULL DEFAULT 0 CHECK(attempt_count>=0)
);

CREATE UNIQUE INDEX uq_app_users_email ON app_users(email);

CREATE INDEX idx_chapters_offering_order ON chapters(offering_id,sort_order);

CREATE INDEX idx_sections_chapter_order ON sections(chapter_id,sort_order);

CREATE INDEX idx_questions_published ON questions(approval_status,difficulty_level);

CREATE INDEX idx_placements_section_select ON question_placements(section_id,usage_type,question_set);

CREATE INDEX idx_placements_question ON question_placements(question_id);

CREATE INDEX idx_papers_exam_year ON past_papers(exam_id,exam_year DESC);

CREATE INDEX idx_paper_questions_question ON past_paper_questions(question_id);

CREATE INDEX idx_sessions_student_recent ON practice_sessions(student_id,created_at DESC);

CREATE INDEX idx_sessions_student_active ON practice_sessions(student_id,status);

CREATE INDEX idx_session_questions_session ON session_questions(session_id,question_position);

CREATE INDEX idx_attempts_student_recent ON question_attempts(student_id,submitted_at DESC);

CREATE INDEX idx_attempts_question_ref ON question_attempts(session_question_id);

CREATE INDEX idx_notes_owner_chapter ON study_notes(owner_user_id,chapter_id);

CREATE INDEX idx_chunks_document_order ON document_chunks(document_id,chunk_ordinal);

CREATE INDEX idx_content_jobs_pending ON content_jobs(status,created_at);

CREATE INDEX idx_memberships_verified ON institution_memberships(institution_id,status,role);

CREATE INDEX idx_outbox_pending ON outbox_events(created_at);
