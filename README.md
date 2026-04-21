# iShine REST API Documentation

## 🔗 Quick Links

### Admin Panel (Data Insertion UI)
- **URL**: `http://localhost:8080/admin`
- **Description**: Web interface for managing classes, subjects, chapters, questions, and linking data

### H2 Database Console
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/testdb`
- **Username**: `sa`
- **Password**: *(leave empty)*

### Home Page
- **URL**: `http://localhost:8080/`

---

## ⚠️ Important: Database Setup

### First-Time Setup or Subject Creation Issue Fix

If you encounter an error when creating subjects (NULL not allowed for column "CLASS_ID"), you need to run the database migration:

1. **Stop your Spring Boot application**
2. **Open H2 Console**: `http://localhost:8080/h2-console`
3. **Login with**:
   - JDBC URL: `jdbc:h2:file:./data/testdb`
   - Username: `sa`
   - Password: *(leave empty)*
4. **Run the migration script**: Copy and execute the SQL from `database_migration_fix_subjects_table.sql`
5. **Restart your application**

The migration removes the redundant `CLASS_ID` column from the `subjects` table, as subjects are linked to classes through the `class_subjects` join table.

---

## 📚 API Endpoints by Category

### 🔐 Authentication APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/signup` | Register a new student account |
| POST | `/auth/login` | Login with email and password |

---

### 🎓 Class Management APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/classes` | Get all classes |
| POST | `/classes` | Create a new class |
| PUT | `/classes/{classId}` | Update an existing class |
| DELETE | `/classes/{classId}` | Delete a class by ID |

---

### 📖 Subject Management APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/subjects` | Get all subjects |
| GET | `/subjects/class/{classId}` | Get subjects by class ID |
| POST | `/subjects` | Create new subjects (bulk) |
| PUT | `/subjects/{subjectId}` | Update a subject |
| DELETE | `/subjects/{subjectId}` | Delete a subject |

---

### 📑 Chapter Management APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/chapters` | Get all chapters |
| GET | `/chapters?subjectId={id}` | Get chapters by subject ID |
| POST | `/chapters/subject/{subjectId}` | Create chapters for a subject |
| POST | `/chapters/upload` | Upload chapters from Excel file |
| DELETE | `/chapters/{chapterId}` | Delete a chapter by ID |
| DELETE | `/chapters?subjectId={id}` | Delete all chapters for a subject |

---

### ❓ Question Management APIs (Practice Questions)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/questions` | Get all questions |
| GET | `/questions/{id}` | Get question by ID |
| GET | `/questions/chapter/{chapterId}` | Get questions by chapter |
| GET | `/questions/subject/{subjectId}` | Get questions by subject |
| GET | `/questions/unpracticed/subject` | Get unpracticed questions by subject (params: studentId, subjectId, limit) |
| GET | `/questions/unpracticed/chapter` | Get unpracticed questions by chapter (params: studentId, chapterId, limit, level) |
| POST | `/questions` | Create new questions (bulk) |
| POST | `/questions/upload` | Upload questions from Excel file |
| PUT | `/questions/{id}` | Update a question |
| DELETE | `/questions/{id}` | Delete a question |

---

### 📝 Exam Question Management APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/examquestions` | Get all exam questions |
| GET | `/examquestions/{id}` | Get exam question by ID |
| GET | `/examquestions/chapter/{chapterId}` | Get exam questions by chapter |
| POST | `/examquestions` | Create new exam questions (bulk) |
| PUT | `/examquestions/{id}` | Update an exam question |
| DELETE | `/examquestions/{id}` | Delete an exam question |

---

### 📄 Previous Exam Paper APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/prevexam-papers` | Get all previous exam papers |
| GET | `/prevexam-papers/by-subject?subjectId={id}` | Get papers by subject with details |
| POST | `/prevexam-papers` | Create a new exam paper |
| POST | `/prevexam-papers/upload` | Upload exam papers from Excel file |

---

### 📋 Previous Exam Question APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/prevexam-questions/all` | Get all previous exam papers |
| GET | `/prevexam-questions/by-chapter?chapterId={id}` | Get questions by chapter |
| GET | `/prevexam-questions/by-subject?subjectId={id}` | Get questions by subject |
| POST | `/prevexam-questions/upload` | Upload questions from Excel file |

---

### 👨‍🎓 Student Management APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/students` | Get all students |
| GET | `/students/{id}` | Get student by ID |
| GET | `/students/{studentId}/profile` | Get student profile for onboarding |
| GET | `/students/{studentId}/subjects` | Get subjects selected by student |
| GET | `/students/{studentId}/practiceProgress/chapter?chapterId={id}` | Get practice progress by chapter |
| GET | `/students/{studentId}/practiceProgress/subject?subjectId={id}` | Get practice progress by subject |
| POST | `/students` | Create a new student |
| POST | `/students/{studentId}/subjects` | Select subjects for student |
| PUT | `/students/{studentId}/class?classId={id}` | Set student's class |
| PUT | `/students/{studentId}/subjects` | Replace student's subjects |
| DELETE | `/students/{id}` | Delete a student |

---

### 📊 Practice Session APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/practice-session-details` | Get all practice session details |
| GET | `/practice-session-details/{studentId}` | Get practice sessions by student ID |
| POST | `/practice-session-details/{studentId}` | Create practice session details for student |
| DELETE | `/practice-session-details/{id}` | Delete a practice session detail |

---

### 📝 Common Notes APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/common-notes/chapter/{chapterId}` | Get all active notes for a chapter (for students) |
| GET | `/common-notes` | Get all common notes (admin) |
| GET | `/common-notes/{id}` | Get a specific note by ID |
| GET | `/common-notes/categories` | Get all distinct note categories |
| POST | `/common-notes` | Create a new common note (admin) |
| PUT | `/common-notes/{id}` | Update an existing note (admin) |
| PATCH | `/common-notes/{id}/deactivate` | Deactivate a note (soft delete) |
| DELETE | `/common-notes/{id}` | Delete a note permanently (admin) |

**Note Categories:**
- Formulas (📐) - Mathematical formulas, equations
- Definitions (📚) - Key terms and their meanings
- Tips & Tricks (💡) - Study tips, shortcuts, mnemonics
- Key Concepts (🔑) - Important concepts to understand
- Examples (📝) - Worked examples and solutions
- Important Points (⭐) - Critical information to remember

---

### 📓 Student Notes APIs (Personal Notes)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/students/{studentId}/notes/{chapterId}` | Get all notes for a student and chapter |
| GET | `/students/{studentId}/notes` | Get all notes for a student (with pagination) |
| GET | `/students/{studentId}/notes/note/{noteId}` | Get a specific note by ID |
| POST | `/students/{studentId}/notes` | Create a new personal note |
| PUT | `/students/{studentId}/notes/{noteId}` | Update an existing note |
| DELETE | `/students/{studentId}/notes/{noteId}` | Delete a note |
| POST | `/students/{studentId}/notes/migrate` | Migrate notes from localStorage (bulk create) |

**Query Parameters for GET all notes:**
- `limit` (default: 50) - Number of notes to return
- `offset` (default: 0) - Pagination offset
- `sortBy` (default: updatedAt) - Sort field (createdAt, updatedAt, title)
- `order` (default: desc) - Sort order (asc, desc)

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Running the Application
```bash
# Navigate to project directory
cd ishinerest

# Run with Maven
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

### Default Configuration
- **Server Port**: 8080
- **Database**: H2 (file-based at `./data/testdb`)
- **JPA**: Auto-update schema enabled
- **SQL Logging**: Enabled with formatting

---

## 📁 Data Files
Excel templates and data files are located in the `/data` directory:
- `1_Maths.xlsx` - Mathematics questions
- `DbData.xlsx` - Database data
- `LCData.xlsx` - Leaving Certificate data
- `Questions list.ods/xls` - Question lists

---

## 🔧 Technology Stack
- **Framework**: Spring Boot
- **Database**: H2 (Development), MySQL (Production ready)
- **ORM**: Hibernate/JPA
- **Authentication**: Custom auth service
- **Frontend**: Thymeleaf templates

---

## 📝 Notes
- All endpoints support JSON request/response format
- File upload endpoints accept Excel files (.xlsx, .xls)
- CORS is enabled for `http://localhost:3000` and `http://localhost:5173`
- H2 console is enabled for development purposes

---

## 👨‍💻 Development
Made with ❤️ by the iShine Team