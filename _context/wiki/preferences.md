# Working Preferences & Standards

## Code Review Expectations

Reviews should be **thorough** and cover all of the following dimensions — nothing should be skipped:

1. **Security** — auth gaps, exposed secrets, injection risks, CORS policy, token handling
2. **Performance** — N+1 queries, missing indexes, eager vs lazy loading, unnecessary data fetching
3. **Error handling** — missing exception handling, raw 500s returned to clients, silent failures
4. **Naming** — class names, method names, variable names, endpoint paths (REST conventions)
5. **Code structure** — separation of concerns, fat controllers, logic in the wrong layer
6. **Data integrity** — nullable vs non-nullable, cascade rules, orphan removal correctness
7. **Test coverage** — missing `.http` files, untested edge cases
8. **Documentation** — missing Javadoc or API spec for new endpoints

---

## Coding Style & Standards

- **Language**: Java 17, Spring Boot 3.x conventions
- **Boilerplate**: Use Lombok (`@Getter`, `@Setter`, `@RequiredArgsConstructor`) — do not write verbose constructors/getters manually
- **Entities**: Keep entities lean — no business logic inside entity classes
- **Services**: Business logic lives in `@Service` classes only
- **Controllers**: Controllers should delegate immediately to services — no logic in controllers
- **DTOs / POJOs**: Use dedicated request/response objects — never expose entities directly in API responses
- **Repositories**: Custom queries go in repository interfaces using `@Query` or method naming conventions
- **SQL migrations**: New schema changes require a new migration SQL file in the project root, following the existing `database_migration_*.sql` naming pattern
- **Null safety**: Prefer `Optional<T>` in repository return types; handle empty cases explicitly
- **Transactions**: Mark service methods with `@Transactional` where write operations span multiple repositories

---

## Architecture Principles

- Follow a strict **Controller → Service → Repository** layering — no repository calls from controllers
- **`@CrossOrigin`** annotations should be centralised in `WebConfig`, not scattered per controller
- Keep CORS origins in a config property, not hardcoded strings
- Sensitive config (email credentials, DB passwords, JWT secret) must live in environment variables — never in `application.properties` committed to source control
- One Spring Boot profile per environment: `dev` (H2), `prod` (MySQL)

---

## Security Standards

- JWTs or session tokens must be validated by a Spring Security filter chain — not manually in each controller
- Passwords must be hashed (BCrypt) — never stored or logged in plain text
- Password reset tokens must have expiry and be single-use
- Email verification must be required before full access is granted
- Role-based access control (`@PreAuthorize` or `SecurityContext` checks) must be applied per endpoint
- No secrets in source-controlled config files

---

## Communication Preferences

- Give **detailed explanations** — don't just say "fix this"; explain why it's a problem and what the impact is
- When a review finds issues, group them by severity: **Critical → High → Medium → Low / Suggestion**
- **Ask before making big changes** — propose the approach first, wait for approval, then implement
- For small, clearly safe fixes (typos, formatting, trivial renames) you may proceed directly
- When adding new files or refactoring structure, always describe what you're about to do first
- Prefer inline code references with line numbers when pointing out review findings

---

## How to Work with AI (Bob)

- Act as a **senior code reviewer** on this project — thorough, opinionated, and direct
- Do not skip over issues because they seem minor — surface everything
- When implementing a feature, present a short plan before writing code
- Do not hallucinate API behaviour — always read the relevant source file before commenting on it
- Update the wiki after tasks that yield durable knowledge (new patterns, architecture decisions, etc.) — but always ask for approval first
- Prefer minimal changes — do not refactor unrelated code as a side effect of a task
- Use the existing code style and conventions; do not introduce new patterns without discussion
