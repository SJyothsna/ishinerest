-- SQL Queries to view Subjects and Class-Subject Relationships

-- 1. View all subjects with their IDs and names
SELECT subject_id, subject_name 
FROM subjects 
ORDER BY subject_id;

-- 2. View all class-subject relationships
SELECT 
    cs.class_id,
    c.class_name,
    cs.subject_id,
    s.subject_name
FROM class_subjects cs
LEFT JOIN classes c ON cs.class_id = c.class_id
LEFT JOIN subjects s ON cs.subject_id = s.subject_id
ORDER BY cs.class_id, cs.subject_id;

-- 3. View subjects grouped by class
SELECT 
    c.class_id,
    c.class_name,
    c.exam,
    GROUP_CONCAT(s.subject_name ORDER BY s.subject_name SEPARATOR ', ') as subjects
FROM classes c
LEFT JOIN class_subjects cs ON c.class_id = cs.class_id
LEFT JOIN subjects s ON cs.subject_id = s.subject_id
GROUP BY c.class_id, c.class_name, c.exam
ORDER BY c.class_id;

-- 4. View classes grouped by subject
SELECT 
    s.subject_id,
    s.subject_name,
    GROUP_CONCAT(c.class_name ORDER BY c.class_name SEPARATOR ', ') as classes
FROM subjects s
LEFT JOIN class_subjects cs ON s.subject_id = cs.subject_id
LEFT JOIN classes c ON cs.class_id = c.class_id
GROUP BY s.subject_id, s.subject_name
ORDER BY s.subject_id;

-- 5. Count subjects per class
SELECT 
    c.class_id,
    c.class_name,
    COUNT(cs.subject_id) as subject_count
FROM classes c
LEFT JOIN class_subjects cs ON c.class_id = cs.class_id
GROUP BY c.class_id, c.class_name
ORDER BY c.class_id;

-- 6. Count classes per subject
SELECT 
    s.subject_id,
    s.subject_name,
    COUNT(cs.class_id) as class_count
FROM subjects s
LEFT JOIN class_subjects cs ON s.subject_id = cs.subject_id
GROUP BY s.subject_id, s.subject_name
ORDER BY s.subject_id;

-- 7. Find subjects not linked to any class
SELECT subject_id, subject_name
FROM subjects s
WHERE NOT EXISTS (
    SELECT 1 FROM class_subjects cs 
    WHERE cs.subject_id = s.subject_id
);

-- 8. Find classes without any subjects
SELECT class_id, class_name
FROM classes c
WHERE NOT EXISTS (
    SELECT 1 FROM class_subjects cs 
    WHERE cs.class_id = c.class_id
);

-- 9. Insert sample data into class_subjects (if needed)
-- Uncomment and modify as needed:
-- INSERT INTO class_subjects (class_id, subject_id) VALUES (1, 'MATH');
-- INSERT INTO class_subjects (class_id, subject_id) VALUES (1, 'ENG');
-- INSERT INTO class_subjects (class_id, subject_id) VALUES (2, 'MATH');

-- 10. Update/Replace subjects for a specific class
-- Example: Remove all subjects for class 1 and add new ones
-- DELETE FROM class_subjects WHERE class_id = 1;
-- INSERT INTO class_subjects (class_id, subject_id) VALUES 
--     (1, 'MATH'),
--     (1, 'ENG'),
--     (1, 'SCI');

-- 11. View complete database structure
SELECT 
    'Classes' as table_name,
    class_id as id,
    class_name as name,
    exam,
    exam_id,
    NULL as subject_id
FROM classes
UNION ALL
SELECT 
    'Subjects' as table_name,
    NULL as id,
    subject_name as name,
    NULL as exam,
    NULL as exam_id,
    subject_id
FROM subjects
ORDER BY table_name, id;

-- Made with Bob
