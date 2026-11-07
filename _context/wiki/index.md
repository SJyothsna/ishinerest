# iShine REST API — Wiki Index

This wiki documents the iShine backend project for use by AI agents and developers.
Read only the sections relevant to your current task — do not read the whole wiki upfront.

## Files in this wiki

| File | Contents |
|------|----------|
| [project.md](project.md) | Project overview, goals, user roles, DB structure, key modules and API surface |
| [preferences.md](preferences.md) | Working standards, coding style, review expectations, how to work with AI |

## Quick orientation

- **What**: Spring Boot 3 REST API backend for the iShine study app (Java 17, H2/MySQL, JPA, Swagger)
- **Who**: Students, Teachers, Parents, Admins
- **Core flows**: Auth → Practice questions → Tests (teacher-created + assigned) → Progress tracking
- **DB**: H2 locally (file-based), MySQL-ready for production
- **Auth**: Session token returned on login (no Spring Security JWT filter yet — see preferences for review notes)

## When to read more

| Task type | Relevant file |
|-----------|--------------|
| New feature, new endpoint, architecture decision | [project.md](project.md) |
| Code review, naming, style, error handling, security | [preferences.md](preferences.md) |
| Both | Read both |
