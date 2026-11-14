# Versioned frontend contract

Base URL: `/api/v1`. JSON identifiers are UUID strings. Authenticated requests use `Authorization: Bearer <token>`. Identity is resolved server-side; callers cannot select another student through a body/query `studentId`. Errors expose a human-readable `message` and use normal HTTP status codes.

## Identity and catalogue

- `POST /auth/signup` — `{name,email,password,role: STUDENT|PARENT}`.
- `POST /auth/login` — `{email,password}`. Both responses include `{userId,studentId,name,email,emailVerified,role,token}`.
- `GET /auth/me`, `POST /auth/logout`, `GET /auth/config`.
- `POST /auth/password-reset`, `/auth/password-reset/validate`, `/auth/password-reset/complete`; `POST /auth/email-verification`, `/auth/email-verification/complete`.
- `GET /catalogue/offerings` — published offering, curriculum and level identities and display names.
- `GET /catalogue/offerings/{offeringId}/chapters`; `GET /catalogue/chapters/{chapterId}/sections`.
- `GET|PUT /students/me/subjects` — PUT body is the full selected offering-ID list. Removing an enrolment archives it without deleting evidence.

## One session engine

`POST /practice-sessions`:

```json
{
  "offeringId": "uuid",
  "chapterId": null,
  "sectionIds": [],
  "kind": "PRACTICE",
  "mode": "QUICK",
  "scope": "SUBJECT_ALL",
  "difficulty": "ALL",
  "questionCount": 10,
  "timerSeconds": null
}
```

Kinds: `PRACTICE`, `TEST`, `INITIAL_ASSESSMENT`. Modes: `QUICK`, `FLAGGED`, `MISTAKES`, `PERSONALIZED`. Scopes: `SUBJECT_ALL`, `SUBJECT_STARTED`, `CHAPTER_ALL`, `SELECTED_SECTIONS`. Fixed difficulties: `FOUNDATION`, `INTERMEDIATE`, `ADVANCED`; `ALL` mixes available levels.

Response includes `sessionId`, `kind`, `status`, `submissionToken`, `offeringId`, `chapterId`, `expiresAt`, `requestedCount`, `selectedCount`, `shortage`, and ordered `questions`. Each question includes `sessionQuestionId`, canonical `questionId`, `prompt`, `type`, `options`, optional image asset/draft. Answer keys and explanations are absent until permitted.

Snapshot types: `single`, `multiple`, `text`, `worked`. Choice options are `{id,text}`. Single/text responses are strings, multiple responses are string arrays. Worked-example responses are `{understood:boolean}` and are explicitly self-reported.

- `GET /practice-sessions/{id}` — owned resume/result.
- `PUT /practice-sessions/{id}/drafts` — `{answers:[{sessionQuestionId,response}]}`.
- `POST /practice-sessions/{id}/check` — `{sessionQuestionId,response}`, practice only. Returns correctness/explanation or a worked-example self-review prompt.
- `POST /practice-sessions/{id}/submit` — `{submissionToken,answers:[{sessionQuestionId,response}]}`. Server grades against frozen revisions; no client correctness/totals are accepted.
- `GET /practice-options?offeringId=&chapterId=` — available quick/flagged/mistake counts.
- `PUT|DELETE /questions/{questionId}/flag?offeringId=`.

## Guest samples

`GET /catalogue/offerings/{id}/sample-options` returns the subject’s distinct eligible `questionCount` and `chapters: [{chapterId,title,questionCount}]`. Only published, active, globally available non-worked questions are counted.

`POST /guest-practice/sessions` with `{offeringId,chapterId?}` creates an opaque-cookie-owned sample. Omit/null `chapterId` for all chapters; otherwise only that active chapter in the offering is eligible. A new sample randomly draws up to seven distinct questions, then orders them FOUNDATION → INTERMEDIATE → ADVANCED, retaining random order within each difficulty. There is no fixed difficulty quota or cross-session non-repeat guarantee. A smaller pool returns its available questions and `shortage=true`, without expanding the scope. Refresh/resume keeps the existing frozen session questions. The cookie is HttpOnly, SameSite Strict and expires in one day. Sample get/drafts/submit use `/guest-practice/sessions/{id}` with the same DTOs as ordinary sessions.

`POST /guest-practice/sessions/{id}/claim` requires sign-in and the original guest cookie. Claiming is idempotent for that account and cannot transfer a sample to another account. Anonymous reads remain aggregate-only even after claiming. Guest results do not enter ordinary adaptive history.

## Insights and resources

- `GET /students/me/progress?offeringId=&chapterId=` — distinct coverage, objective accuracy, calculation/version metadata.
- `GET /students/me/recommendations?offeringId=&chapterId=` — suggestions based on available fresh questions, coverage, and recent evidence.
- `GET /students/me/resumable-sessions`.
- `GET /notes?offeringId=&chapterId=`, `POST /notes`, `PUT|DELETE /notes/{id}`. Note body: `{offeringId,chapterId,sectionId,title,content,color,attachmentIds}`.
- `GET /notes/{id}/attachments`; `POST /assets` multipart `file`; `GET /assets/{id}/content` with ownership/content permission checks.
- `GET /videos?offeringId=&chapterId=`.
- `GET /past-papers?offeringId=&year=`, `/past-papers/{id}/marking-scheme`.
- `GET /chapter-past-questions?chapterId=`, `/past-questions/{id}/assets`.
- `GET|POST /parent-links`; `PUT /parent-links/{parentId}/approval`; `DELETE /parent-links/{parentId}/{studentId}`.
- Verified parents only: `GET /parent-links/students/{studentId}/subjects`, `/progress?offeringId=`. Private notes are never included.
- `POST /feedback` — `{name,email,type,subject,message}`.

## Manual textbook/question-bank administration

These routes require server-side `content_admin` permission. Login/profile responses include `contentAdmin` for UI navigation; a client-supplied flag grants no permission.

- `GET|POST /content/question-sources` — POST multipart `{file,title,licenceConfirmed,kind}`. `kind=TEXTBOOK` accepts a PDF up to 50 MB; `kind=ORIGINAL` accepts original question JSON up to 5 MB. Both are private. The manifest `sourceType` must match the document (`TEXTBOOK` by default); `ORIGINAL` questions omit textbook pages. No extraction/generation runs. List rows include `sourceType`; bank list rows use `sourceTitle`. Optional question `examLevel=COMMON|ORDINARY|HIGHER` is retained and shown during practice.
- `POST /content/question-banks/validate` — `{offeringId,documentId,manifest}` returns `{valid,issues,warnings,summary}` without publishing.
- `POST /content/question-banks` — same body creates/reuses a review import.
- `GET /content/question-banks`, `GET /content/question-banks/{id}`.
- `POST /content/question-banks/{id}/publish` — `{reviewConfirmed:true}` validates again, publishes revisions/placements atomically, and completes relevant Set 2 preparation requests. Repeated publication is idempotent.
- `GET /content/jobs` — preparation requests, human-readable chapter titles and status.

Manifest format 1.0: `questionSet` (1 or 2), `coverageScope` (`SUBJECT`, `CHAPTER`, `TOPICS`), `chapters[]`, each containing `topics[]`, planned `expectedCounts` for `EASY/MEDIUM/HARD`, and `questions[]`. Each question has a stable `externalId`, source PDF page, type, prompt, explanation, difficulty and type-specific answer fields. `imageAssetId` optionally references an authorised uploaded diagram. Easy/medium/hard map to the existing Foundation/Intermediate/Advanced session filter.

`/practice-options` includes a `progression` summary. Sessions include `repeatedCount`; individual question DTOs include `questionSet`. A claimed sample participates only in exposure/repeat avoidance, not the ordinary graded attempt journal.

## Page-review additions

Offering and enrolment DTOs include `isStarter`; enrolled offerings also return curriculum and level names. Only currently published enrolled offerings are listed. `GET /catalogue/offerings/{id}/preview` is public and exposes limited published chapter/paper metadata for the home page. It never exposes question content or answer keys. Paper DTOs include `examLevel` where known. Session DTOs include `isStarter`, determined from frozen question placements, and each question includes its saved `flagged` status. `auth/config` includes `emailVerificationEnabled`. Verification-link replay is idempotent while the link remains valid.

Publishing reviewed Set 1 retires starter placements and general starter notes in the selected offering. Real bank content is then the only eligible source for new sessions.

After an authorised owned **practice** check, the response also includes `correctAnswer` or `correctAnswers` from the frozen revision. This supports correct-choice highlighting. Initial session reads and test/assessment checks retain their existing answer-reveal restrictions.


Explanation videos: practice `check` responses and completed-session question reviews include `videos`, an array of `{videoId,youtubeVideoId,title}` for up to five active videos associated with the session question’s placed topic. Initial/unsubmitted questions do not expose this assistance. No associated videos returns an empty array. Existing ownership and assessment reveal restrictions apply.

## Shared session screen contract — 1 November 2026

Session responses additionally provide `subjectName`, `levelName`, nullable `chapterName`, `timerSeconds` and `serverTime` (UTC ISO instant). Each question includes topic/chapter display context. `serverTime` is transport timing metadata and changes on reads; frozen questions, stored evidence and submission results remain idempotent.

Both `/practice-sessions/{id}` and `/guest-practice/sessions/{id}` support:

- `PUT /questions/{sessionQuestionId}/flag` with `{flagged:boolean}`. Resolves ownership from the authenticated student or opaque guest cookie; the question must belong to that session. Guest flags persist in the owned session; student flags are saved within the session’s course for flagged practice. Finished-session flags cannot be changed.
- `POST /questions/{sessionQuestionId}/issues` with `{reason: "INCORRECT_ANSWER"|"UNCLEAR_WORDING"|"MISSING_INFORMATION"}`. An identical report for the same frozen session question/reason is idempotent and does not change the answer or personal flag.

Practice `check` records the first checked response once in `session_answer_checks` and returns `firstResponse` with the existing feedback. Repeated checks retain the first evaluation. Owned practice reads restore that question's `feedback`; tests, assessments and samples never expose it during an active session. Final review correctness still grades the final submitted response. Adaptive statistics/progress/recommendations use the first check when present, so correcting after an explanation does not masquerade as an independent correct attempt.

Result summaries additionally include `total`, `unanswered` and `correctedAfterExplanation`. Practice accuracy is explicitly labelled **final answer accuracy** in the UI. Deadline policy is unchanged: editing/checks/draft saves stop at expiry; explicit submission after expiry grades only drafts stored before the deadline. The clock keeps running after leaving. No automatic submission or timer pause was introduced.

Admin-only `GET /content/question-issues` lists the oldest 200 reports awaiting review, with frozen question content, subject/chapter and revision context. `PUT /content/question-issues/{sessionQuestionId}` with `{reason,reviewed:boolean}` updates review state. Content corrections go through the existing question-bank publication workflow.

Migration V12 applies to H2 and PostgreSQL. Hints and live AI remain deferred.

Clearing the final response after checking does not erase that evaluated first attempt: the final review shows it as unanswered, while the first check remains available as practice evidence.

## Course learner profile and personalised sessions

`GET /students/me/learning-profile?offeringId=&chapterId=` requires enrolment in the published course. It returns `offeringId`, `objectiveCount`, `objectivesAttempted`, `strengths`, `recommendations` and `objectives`. A chapter filter limits the profile to that chapter. Each evidence row includes `objectiveId` (nullable for a subtopic without a reviewed objective plan), `sectionId`, `chapterId`, `title`, `status`, `evidenceCount`, `independentCorrect`, `correctionsAfterFeedback`, `freshSuccessAfterCorrection`, `demonstratedDifficulty` (nullable), `availableQuestions`, and `freshQuestions`.

The profile considers up to 12 distinct recent question families per objective from the last 90 days. Strength requires at least three mapped families across two sessions with at least 80% independent accuracy. Other evidence states are `UNASSESSED`, `DEVELOPING`, and `NEEDS_PRACTICE`. These are current practice signals. The first checked answer is separate from a correction after feedback, and later independent success on a different family is recorded separately. Unanswered questions and self-review do not supply independent outcomes. Existing attempts without frozen objective/family metadata stay in their original scope.

Create a `PRACTICE` or `TEST` session with `mode: "PERSONALIZED"` to use this evidence for selection. Practice prioritises areas to revisit and objectives with little evidence. Tests balance areas to revisit, strengths, and objectives with less evidence within the selected scope. Keep the chosen scope and difficulty. For a targeted practice recommendation, send its `objectiveId` with `scope: "SELECTED_SECTIONS"` and its single `sectionId` in `sectionIds`. Omit `objectiveId` for a subtopic recommendation or a broader session. Responses include `trackCode` and preserve effective question difficulty for the frozen session.

`GET /students/me/archived-sessions` lists the most recent reviewable results from the current learner’s archived courses. Use the existing session result route to reopen the frozen review. Archived results stay outside active course profiles.
