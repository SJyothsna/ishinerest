# iShine REST API

## 📖 API Documentation

All endpoints are fully documented in **Swagger UI** — interactive, always up to date, and directly testable from the browser.

| Resource | URL |
|---|---|
| **Swagger UI** (interactive docs) | `http://localhost:8080/swagger-ui.html` |
| **OpenAPI JSON** (for Postman / code gen) | `http://localhost:8080/api-docs` |

> Swagger UI groups all endpoints by controller tag and lets you try every call directly in the browser.

---

## 🔗 Quick Links

| Resource | URL |
|---|---|
| Home page | `http://localhost:8080/` |
| Admin panel | `http://localhost:8080/admin` |
| H2 Database console | `http://localhost:8080/h2-console` |

**H2 login details**
- JDBC URL: `jdbc:h2:file:./data/istudydb`
- Username: `sa`
- Password: *(leave empty)*

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Running the Application
```bash
# Run with Maven (Linux / macOS)
./mvnw spring-boot:run

# Run with Maven (Windows)
mvnw.cmd spring-boot:run
```

### Default Configuration
- **Server port**: 8080
- **Database**: H2 file-based at `./data/istudydb`
- **Schema**: Auto-updated by Hibernate on startup
- **CORS**: Enabled for `http://localhost:3000` and `http://localhost:5173`

---

## 🔧 Technology Stack
- **Framework**: Spring Boot 3.2
- **Database**: H2 (development) · MySQL (production-ready)
- **ORM**: Hibernate / JPA
- **API docs**: springdoc-openapi (Swagger UI)
- **Auth**: Custom JWT-based auth service

---

## 👨‍💻 Development
Made with ❤️ by the iShine Team
