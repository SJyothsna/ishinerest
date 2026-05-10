# How to Run the Database Migration for H2

## Step-by-Step Instructions:

### 1. Access H2 Console
1. Make sure your application is running
2. Open your browser and go to: **http://localhost:8080/h2-console**

### 2. Login to H2 Database
Use these connection details:
- **JDBC URL**: `jdbc:h2:file:./data/testdb`
- **User Name**: `SA`
- **Password**: (leave empty)
- Click **Connect**

### 3. Run the Migration SQL
Copy and paste this SQL command into the SQL statement box:

```sql
ALTER TABLE questions ALTER COLUMN question_text TEXT NULL;
```

Click **Run** button

### 4. Verify the Change
Run this command to verify:

```sql
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'QUESTIONS' AND COLUMN_NAME = 'QUESTION_TEXT';
```

You should see `IS_NULLABLE` = `YES`

### 5. Restart Your Application
After running the migration:
1. Stop your Spring Boot application
2. Start it again
3. Test adding a question with only an image (no text)

## Alternative: Automatic Migration on Startup

If you want Hibernate to automatically update the schema, you can temporarily add this to `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

**WARNING**: This is not recommended for production! Only use for development.

After the schema is updated, change it back to:
```properties
spring.jpa.hibernate.ddl-auto=none
```

## Troubleshooting

If you get "NULL not allowed for column QUESTION_TEXT" error:
- The migration hasn't been applied yet
- Follow the steps above to run the migration manually
- Restart the application after migration