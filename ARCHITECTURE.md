# iStudy backend redesign

The backend is a modular Spring Boot application with a single `/api/v1` API. There are no legacy HTTP adapters or parallel question-taking engines. The frontend repository owns the React UI; this repository owns persistence, selection, authorisation, grading, and file access.

## Local development

Email/password authentication and H2 remain the current implementation, as requested. Firebase is deferred. Local passwords use BCrypt; only SHA-256 hashes of opaque, expiring bearer tokens are stored. Firebase can later replace the identity adapter while retaining internal user/student UUIDs.

Run `./mvnw spring-boot:run`. The default `local` profile uses `data/istudy-redesign-v1.mv.db`. Flyway manages the schema; Hibernate does not create or modify tables.

To import the existing local application once, run `./mvnw spring-boot:run -Dspring-boot.run.profiles=local,import-legacy`. Stop the previous backend first. The importer opens `data/testdb` read-only. The original file is not replaced. Accounts, catalogue, canonical questions, placements, flags, parent links and notes are copied into the new model. Images are copied into private local object storage. Private import source files under `data/legacy-paper-sources` supply the one-time paper import; `app.import.paper-root` can select another source folder. These files are not shipped in the frontend build. Importers are not API compatibility layers and are not enabled during normal operation.

Historical practice/test rows are retained in `imported_evidence`. Their original question revisions and session boundaries cannot be reconstructed reliably from the source schema. They are therefore preserved for explicit reconciliation and excluded from the new reproducible grading journal and personalised metrics. No historical question snapshots or session dates are invented.

## PostgreSQL

`compose.redesign.yml` defines the separately named `istudy-redesign-postgres` container and `istudy-redesign-postgres-data` volume, bound to localhost port 55432. Supply `ISTUDY_DB_PASSWORD`, run `docker compose -f compose.redesign.yml --profile postgres up -d`, then start Spring with the `postgres` profile. The local development password is stored only in ignored `.env.redesign`; use `docker compose --env-file .env.redesign -f compose.redesign.yml --profile postgres up -d` for this checkout. `ISTUDY_DB_URL` and `ISTUDY_DB_USER` can select a managed instance instead.

PostgreSQL migrations use UUIDs, JSONB, timestamp-with-time-zone columns, foreign keys and immutable-journal triggers. H2 migrations use the same domain relationships with text-backed snapshots and application-enforced journal immutability. The H2 original data file is not a PostgreSQL migration target. Production cutover requires an explicit data transfer and verification.

## Domain boundaries

- `identity`: local identity adapter, enrolment, recovery tokens, parent approval/revocation. A parent request grants no progress access before student approval.
- `catalogue`: curricula, levels, stable subjects, versioned offerings, chapters and structured section IDs.
- `learning`: candidate selection, frozen revisions/placements, drafts, practice checking, final submission, flags, progress and evidence-based recommendations.
- `resources`: personal/general notes, note images, YouTube metadata, full papers and chapter occurrences. Files are accessed through `ObjectStorage`, with a private local adapter for development. An S3 adapter and deployment configuration remain future integration work.
- `content`: textbook source storage, structured bank validation, staging, review/publication and shared Set 2 preparation requests.
- `migration`: opt-in, one-time source import; no runtime dependency on the retired schema.

## Learning rules

Question identity is separate from placement. Sessions de-duplicate canonical question IDs, freeze a revision and effective placement, and never return stored answer keys before an allowed reveal. Immediate checking is restricted to practice. Tests and assessments reveal answers after final submission. Guests receive aggregate results until authenticated claiming; a claim unlocks review and does not silently add guest results to adaptive history.

Submission locks the session and validates its submission token. Retried submissions return the stored result without duplicating attempts. Drafts are resumable but do not count as final evidence. A timed session accepts only drafts saved before expiry when finalised after its deadline; that is the current conservative expiry policy and remains a product review item.

Coverage uses distinct eligible question identities. Accuracy uses evaluated answers, including repeated attempts. Worked examples support explicit self-review, recorded separately from automatic grading and excluded from objective accuracy/recommendations. Summaries can be rebuilt from the journal. Recommendations require at least three distinct questions, six evaluated attempts and two sessions; those conservative thresholds are provisional and need calibration.

The selector balances sections and difficulty buckets, prefers less-exposed questions, and can prioritise sections with more recorded mistakes. It applies explicit scope, mode and difficulty before bounding candidates. It does not use prerequisites, a skill graph or a permanent weakness label. Shortages are returned explicitly; scope/difficulty are not broadened silently.

## Deliberate boundaries

School membership, teacher administration, AI generation, extraction workers, generated audio, skill graphs and a distributed queue are deferred. The schema has extension points where core references require them; no dummy workers or vendor-specific AI APIs are introduced. School selection does not grant permissions.

Email verification/recovery endpoints are retained. Sending requires `ISTUDY_MAIL_ENABLED=true` and Spring mail settings supplied externally; an unconfigured local environment reports that email is unavailable. No SMTP credentials are committed in application settings.

Content imported from the existing app retains its prior publication intent. New question banks require a source-permission assertion, validation and human review through the content studio. The starter schema is a reference, not an instruction to overwrite an existing database.

## Manual content studio and question-set progression

The latest product decision keeps Set 1 as the textbook foundation and Set 2 as fresh, modestly more challenging content. Quick practice prefers unseen Set 1 questions, then unseen approved Set 2 questions, and uses repeats only as a labelled fallback. Around 80% distinct Set 1 exposure, the backend creates a deduplicated chapter preparation request. This is exposure of served question identities, not a progress/knowledge claim; a claimed sample can contribute to repeat avoidance without entering graded adaptive history.

For now administrators generate banks outside the app. The portal stores authorised textbook PDFs, validates structured chapter/topic JSON, checks planned easy/medium/hard counts, source-page references and answer shapes, and requires human review before publishing. Banks are staged in `question_bank_imports`. Stable external IDs create/reuse canonical questions and append immutable revisions for corrections. Separate placements retain curriculum-relative difficulty. Identical Set 1 prompts/options cannot be repackaged as fresh Set 2 questions.

`QuestionGenerationGateway` is the future provider boundary. There is no active AI worker, no OpenAI request, and no automatic generation/extraction on textbook upload. Manual Set 2 publication fulfils pending preparation requests. The source PDF remains private and is associated with page-level question provenance.

The imported existing administrator is marked `content_admin`; normal signup cannot create administrators. Human review includes checking the actual mathematics and source permissions: format validation does not claim to prove mathematical correctness or complete textbook coverage.

## Verification

The 18 integration tests pass on both H2 and PostgreSQL. The final browser journey uses the actual HTTP backend with isolated test data, including manual question-bank publication, fresh-question progression, private resources and parent approval. The original H2 source is preserved read-only; test content is never imported into the working database.

## Single-course local starter

The opt-in `starter-content` profile runs the versioned Ireland Leaving Certificate Mathematics seed once. It retires previous visible offerings/resources, preserves accounts and historical sessions, enrols existing students into the single offering, and adds five strands, ten topics, thirty original example questions and five starter notes. Existing Mathematics papers/marking schemes and chapter occurrences are re-associated with the course. It never invents attempts or performance. A database snapshot is kept under ignored `data/backups` before applying this local reset.

Starter flags distinguish demonstration placements from uploaded material. The first reviewed Set 1 publication disables all starter placements and removes starter notes atomically. The first real bank can establish its own textbook chapter/topic structure; unused starter-only chapters/topics are retired and reused codes are reactivated with reviewed titles. Frozen historical sessions keep their revisions. Use the normal local profile after seeding; do not run starter/import profiles in production. The production profile includes PostgreSQL and requires an HTTPS frontend origin and private storage path, with secure guest cookies enabled. The frontend deployment example applies account/sample/contact request limits at the ingress.

The starter-content profile can revise active starter questions from the versioned seed resource without resetting data. Changed questions append immutable revisions; repeated startup with unchanged content does not create more revisions, and uploaded non-starter banks are left untouched. Starter questions use four-option single-choice snapshots.

## Irish Leaving Certificate course levels and practice evidence

The catalogue groups Mathematics under Ireland and Leaving Certificate, with Ordinary and Higher as independent offerings. On upgrade the mixed Irish Mathematics bank is archived. Explicit Common content is copied into both courses and level-specific content goes into its matching course. Each copy has its own question identity and revision history, with provenance linking back to the original source. The bank labels indicate provisional applicability. They do not certify either course's syllabus coverage. Paper links use the paper's own level. Existing mixed-course sessions, attempts and enrolment records remain attached to their original course. Returning students choose their level.

Set 1 coverage is planned by subtopic and learning objective. Each mapped question has a primary objective and a family code for closely related variants. The v2 bank format can publish a coverage plan before its questions are ready and can fill gaps in subsequent uploads. The Linear and Simultaneous Equations pilot maps verified existing question families and deliberately exposes uncovered objectives. Unmapped content can still be used for practice. Use a separate source file or textbook per course for new banks.

Content Studio provides a published inventory by course, chapter, subtopic and objective, with counts by set and difficulty and a reviewed correction flow. Corrections append revisions so past session snapshots stay intact.

Each new session freezes the effective difficulty, objective, family and whether the question was previously served in the same course. First checked answers or unassisted test results supply independent evidence. Corrections after feedback and later fresh success are separate signals. Evidence is scoped to the learner's course. Practice prioritises areas that need evidence or review, and personalised tests check a mix of objectives in the chosen scope. Question coverage remains a separate metric from demonstrated understanding.
