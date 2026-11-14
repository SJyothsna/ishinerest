# Run iStudy backend locally

Use the `redesign` branch in both repositories. Start this backend before the [frontend](../istudy/RUNNING.md).

## Requirements

Install a JDK 17 or newer. Maven is supplied through `mvnw`; Docker is only needed for the optional PostgreSQL setup.

## Start with H2 (normal local setup)

```sh
cd /Users/sanjeethreddy/Documents/IdeaProjects/ishinerest
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows PowerShell, quote the `-D` argument; otherwise PowerShell splits it at the dot and Maven fails with `Unknown lifecycle phase ".run.profiles=local"`:

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

On another machine, use the actual checkout path. Run from the repository root so relative database and asset paths resolve correctly. Keep the terminal open. Stop with Ctrl+C.

The backend listens on http://localhost:8080. Check readiness with:

```sh
curl --fail http://localhost:8080/api/v1/catalogue/offerings
```

Then start the frontend on http://localhost:3000. The local backend permits that frontend origin by default.

Alternatively, package and run:

```sh
./mvnw clean package
java -jar target/ishinerest-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Use one startup method at a time; two backends cannot share port 8080 or the same H2 file.

## Inspect the H2 database (local profile)

The `local` profile enables the H2 web console, reachable only while the backend is running. Open http://localhost:8080/h2-console and connect with:

- JDBC URL: `jdbc:h2:file:./data/istudy-redesign-v1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`
- User name: `sa`
- Password: leave empty

Keep the file name exactly as shown; a different name silently creates a new empty database. The console is enabled by `spring.h2.console.enabled=true` in `application-local.properties` and permitted by `LocalH2ConsoleSecurity`, which is active only under the `local` profile. It is not available with the `postgres` profile.

## Database and uploaded content

The current local profile uses `data/istudy-redesign-v1.mv.db`. Flyway creates or upgrades the schema automatically. Preserve this file and the private uploaded assets to retain accounts, the uploaded question bank and progress. They are local runtime data, excluded from Git.

The tracked `data/testdb.mv.db` is the older application's database snapshot, included at the project owner's request. It is **not** the active redesign database and may contain legacy account/application data. Pushing this file does not transfer the current redesign question bank or private textbook files.

For a fresh checkout, normal startup creates an empty redesign database. Register through the frontend; ordinary signup does not grant content administrator privileges. Existing local accounts remain available when using the existing redesign database.

Only when intentionally migrating the legacy snapshot into a fresh redesign database, use:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,import-legacy
```

The importer reads the legacy database without replacing it. Additional legacy paper/image source folders are local files and are not shipped by Git. Use the normal `local` profile afterward. Do not enable import or starter profiles for routine startup; see [architecture notes](ARCHITECTURE.md) for their effects.

## Optional PostgreSQL

H2 needs no container. To use PostgreSQL instead, supply a password through your environment or a private `.env.redesign` file containing `ISTUDY_DB_PASSWORD=your-local-password`:

```sh
docker compose --env-file .env.redesign -f compose.redesign.yml --profile postgres up -d
```

Export the same `ISTUDY_DB_PASSWORD` in the backend terminal, then run:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Defaults: container `istudy-redesign-postgres`, database `istudy_redesign`, user `istudy`, localhost port `55432`. PostgreSQL has its own data; switching profiles does not copy H2 data. Do not commit the password file.

Stop PostgreSQL when required:

```sh
docker compose --env-file .env.redesign -f compose.redesign.yml --profile postgres stop
```

## Checks and troubleshooting

```sh
./mvnw test
```

If port 8080 is occupied, stop the previous backend. If H2 reports a lock, stop other processes using the database; do not delete the database to fix it. Back up H2 only after stopping the backend.

Email sending requires separate SMTP configuration. Firebase and live AI generation remain deferred. For API details see [API_CONTRACT.md](API_CONTRACT.md); production requirements are documented in the frontend's deployment guide.
