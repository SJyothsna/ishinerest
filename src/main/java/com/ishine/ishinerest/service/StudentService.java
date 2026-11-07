package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.StudentSubject;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.pojo.StudentHomeSummaryDTO;
import com.ishine.ishinerest.pojo.StudentPracticeProgressDTO;
import com.ishine.ishinerest.pojo.StudentSelectedSubjectDTO;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.TestDetailRepository;
import com.ishine.ishinerest.repository.StudentSubjectRepository;
import com.ishine.ishinerest.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ishine.ishinerest.pojo.StudentProfileDTO;
import com.ishine.ishinerest.entity.ClassEntity;
import com.ishine.ishinerest.repository.ClassRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentSubjectRepository studentSubjectRepository;

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;
    @Autowired
    private ClassRepository classRepository;
    @Autowired
    private TestDetailRepository testDetailRepository;
    @Autowired
    private ChapterRepository chapterRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    public StudentPracticeProgressDTO getPracticeProgressByChapter(Long studentId, String chapterId) {
        // Validate student exists
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId));

        // Get Set 1 progress
        List<Object[]> set1Results = practiceSessionDetailRepository.getChapterProgressBySet(studentId, chapterId, "1");
        
        // Get Set 2 progress
        List<Object[]> set2Results = practiceSessionDetailRepository.getChapterProgressBySet(studentId, chapterId, "2");
        
        // Build and return DTO with set-aware logic
        return buildSetAwareProgressDTO(set1Results, set2Results);
    }

    public StudentPracticeProgressDTO getPracticeProgressBySubject(Long studentId, String subjectId) {
        // Validate student exists
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId));

        // Get Set 1 progress
        List<Object[]> set1Results = practiceSessionDetailRepository.getSubjectProgressBySet(studentId, subjectId, "1");
        
        // Get Set 2 progress
        List<Object[]> set2Results = practiceSessionDetailRepository.getSubjectProgressBySet(studentId, subjectId, "2");
        
        // Build and return DTO with set-aware logic
        return buildSetAwareProgressDTO(set1Results, set2Results);
    }

    public StudentPracticeProgressDTO getTestProgressByChapter(Long studentId, String chapterId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId));

        // Get Set 1 progress
        List<Object[]> set1Results = testDetailRepository.getChapterProgressBySet(studentId, chapterId, "1");
        
        // Get Set 2 progress
        List<Object[]> set2Results = testDetailRepository.getChapterProgressBySet(studentId, chapterId, "2");
        
        // Build and return DTO with set-aware logic
        return buildSetAwareProgressDTO(set1Results, set2Results);
    }

    public StudentPracticeProgressDTO getTestProgressBySubject(Long studentId, String subjectId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId));

        // Get Set 1 progress
        List<Object[]> set1Results = testDetailRepository.getSubjectProgressBySet(studentId, subjectId, "1");
        
        // Get Set 2 progress
        List<Object[]> set2Results = testDetailRepository.getSubjectProgressBySet(studentId, subjectId, "2");
        
        // Build and return DTO with set-aware logic
        return buildSetAwareProgressDTO(set1Results, set2Results);
    }

    /**
     * Build a set-aware progress DTO that shows:
     * - Set 1 only until Set 1 is 100% complete
     * - Set 1 + Set 2 combined after Set 1 is complete
     */
    private StudentPracticeProgressDTO buildSetAwareProgressDTO(List<Object[]> set1Results, List<Object[]> set2Results) {
        // Extract Set 1 data
        SetProgressData set1Data = extractSetProgressData(set1Results);
        
        // Extract Set 2 data
        SetProgressData set2Data = extractSetProgressData(set2Results);
        
        // Determine if Set 1 is complete
        boolean set1Complete = isSetComplete(set1Data);
        
        // Create DTO
        StudentPracticeProgressDTO dto = new StudentPracticeProgressDTO();
        
        // Set 1 breakdown (always populated)
        dto.setSet1Total(set1Data.total);
        dto.setSet1Practiced(set1Data.practiced);
        dto.setSet1Correct(set1Data.correct);
        dto.setSet1Incorrect(set1Data.incorrect);
        dto.setSet1NotPracticed(set1Data.total - set1Data.practiced);
        
        // Set 2 breakdown (always populated)
        dto.setSet2Total(set2Data.total);
        dto.setSet2Practiced(set2Data.practiced);
        dto.setSet2Correct(set2Data.correct);
        dto.setSet2Incorrect(set2Data.incorrect);
        dto.setSet2NotPracticed(set2Data.total - set2Data.practiced);
        
        // Set completion flag
        dto.setSet1Complete(set1Complete);
        
        // Main fields logic
        if (set1Complete) {
            // Show combined Set 1 + Set 2 totals
            dto.setTotalQuestions(set1Data.total + set2Data.total);
            dto.setPracticedQuestions(set1Data.practiced + set2Data.practiced);
            dto.setCorrectAnswers(set1Data.correct + set2Data.correct);
            dto.setIncorrectAnswers(set1Data.incorrect + set2Data.incorrect);
            dto.setNotPracticed(
                (set1Data.total + set2Data.total) -
                (set1Data.practiced + set2Data.practiced)
            );
        } else {
            // Show only Set 1 totals
            dto.setTotalQuestions(set1Data.total);
            dto.setPracticedQuestions(set1Data.practiced);
            dto.setCorrectAnswers(set1Data.correct);
            dto.setIncorrectAnswers(set1Data.incorrect);
            dto.setNotPracticed(set1Data.total - set1Data.practiced);
        }
        
        return dto;
    }

    /**
     * Check if a set is complete (all questions practiced)
     */
    private boolean isSetComplete(SetProgressData setData) {
        return setData.total > 0 && setData.practiced >= setData.total;
    }

    /**
     * Extract progress data from query results
     */
    private SetProgressData extractSetProgressData(List<Object[]> results) {
        if (results == null || results.isEmpty()) {
            return new SetProgressData(0L, 0L, 0L, 0L);
        }
        
        Object[] row = results.get(0);
        Long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Long practiced = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        Long correct = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Long incorrect = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        
        return new SetProgressData(total, practiced, correct, incorrect);
    }

    /**
     * Helper class to hold set progress data
     */
    private static class SetProgressData {
        Long total;
        Long practiced;
        Long correct;
        Long incorrect;
        
        SetProgressData(Long total, Long practiced, Long correct, Long incorrect) {
            this.total = total;
            this.practiced = practiced;
            this.correct = correct;
            this.incorrect = incorrect;
        }
    }

    @Deprecated
    private StudentPracticeProgressDTO mapToProgressDTO(Object[] row) {
        Long totalQuestions = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Long practicedQuestions = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        Long correctAnswers = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Long incorrectAnswers = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        Long notPracticed = totalQuestions - practicedQuestions;

        return new StudentPracticeProgressDTO(
                totalQuestions, practicedQuestions, correctAnswers, incorrectAnswers, notPracticed);
    }

    public List<StudentSelectedSubjectDTO> getSubjectsSelectedByStudent(Long studentId) {
        List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(studentId);

        List<String> subjectIds = studentSubjects.stream()
                .map(StudentSubject::getSubjectId)
                .toList();

        List<SubjectEntity> subjects = subjectRepository.findBySubjectIdIn(subjectIds);

        return subjects.stream()
                .map(s -> new StudentSelectedSubjectDTO(s.getSubjectId(), s.getSubjectName()))
                .toList();
    }

    public List<StudentSubject> saveStudentSubjects(Long studentId, List<String> subjectIds) {
        List<StudentSubject> savedSubjects = new ArrayList<>();

        for (String subjectId : subjectIds) {
            boolean alreadyExists = studentSubjectRepository.existsByStudentIdAndSubjectId(studentId, subjectId);
            if (!alreadyExists) {
                StudentSubject studentSubject = new StudentSubject();
                studentSubject.setStudentId(studentId);
                studentSubject.setSubjectId(subjectId);
                studentSubject.setSelectedAt(LocalDateTime.now());
                savedSubjects.add(studentSubjectRepository.save(studentSubject));
            }
        }

        return savedSubjects;
    }

    // new method: build a light profile for onboarding checks
    public StudentProfileDTO getProfile(Long studentId) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        boolean hasClass = s.getClassEntity() != null;
        Integer classId = hasClass ? s.getClassEntity().getClassId() : null; // adjust getter if your id is named
                                                                             // differently
        long subjectCount = studentSubjectRepository.countByStudentId(studentId);
        return new StudentProfileDTO(s.getStudentId(), s.getName(), s.getEmail(), hasClass, classId, subjectCount);
    }

    // new method: set/update the student's class
    public void setStudentClass(Long studentId, Integer classId) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        ClassEntity ce = classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid classId"));
        s.setClassEntity(ce);
        studentRepository.save(s);
    }

    public void replaceStudentSubjects(Long studentId, List<String> subjectIds) {
        studentSubjectRepository.deleteByStudentId(studentId);
        saveStudentSubjects(studentId, subjectIds); // reuse your existing saver
    }

    // -------------------------------------------------------------------------
    // Home summary
    // -------------------------------------------------------------------------

    public StudentHomeSummaryDTO getHomeSummary(Long studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with ID: " + studentId));

        int streak = computeStreak(studentId);
        List<StudentHomeSummaryDTO.RecentActivityItem> recentActivity = buildRecentActivity(studentId);
        StudentHomeSummaryDTO.RecommendedChapter recommended = buildRecommendedNext(studentId);

        return new StudentHomeSummaryDTO(streak, recentActivity, recommended);
    }

    /**
     * Counts consecutive calendar days (including today or ending yesterday)
     * on which the student has at least one practice record.
     *
     * The native query returns java.sql.Date objects (not LocalDate) so we convert
     * each element explicitly and skip nulls.
     */
    private int computeStreak(Long studentId) {
        List<Object> raw = practiceSessionDetailRepository.findDistinctPracticeDatesByStudent(studentId);

        List<LocalDate> dates = raw.stream()
                .filter(o -> o != null)
                .map(o -> {
                    if (o instanceof java.sql.Date d) return d.toLocalDate();
                    if (o instanceof LocalDate ld) return ld;
                    // H2 may return a string like "2025-06-10"
                    return LocalDate.parse(o.toString());
                })
                .collect(java.util.stream.Collectors.toList());

        if (dates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
        // If the student hasn't practised today yet, allow the streak to start from yesterday
        if (!dates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }

        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (date.isBefore(cursor)) {
                // Gap found — stop counting
                break;
            }
        }
        return streak;
    }

    /**
     * Builds up to 3 most recently practised chapters for the recent-activity feed.
     */
    private List<StudentHomeSummaryDTO.RecentActivityItem> buildRecentActivity(Long studentId) {
        List<Object[]> rows = practiceSessionDetailRepository.findRecentChapterActivity(studentId, 3);
        List<StudentHomeSummaryDTO.RecentActivityItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String chapterId       = (String) row[0];
            String chapterName     = (String) row[1];
            String subjectName     = (String) row[2];
            LocalDateTime lastPracticed = row[3] instanceof LocalDateTime ldt ? ldt
                    : row[3] != null ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null;
            long questionsAnswered = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            items.add(new StudentHomeSummaryDTO.RecentActivityItem(
                    chapterId, chapterName, subjectName, lastPracticed, questionsAnswered));
        }
        return items;
    }

    /**
     * Finds the chapter that follows the student's last practised chapter
     * in the same subject. Returns null when no next chapter exists.
     */
    private StudentHomeSummaryDTO.RecommendedChapter buildRecommendedNext(Long studentId) {
        String lastChapterId = practiceSessionDetailRepository.findLastPracticedChapterId(studentId);
        if (lastChapterId == null) {
            return null;
        }
        Optional<Chapter> next = chapterRepository.findNextChapterInSubject(lastChapterId);
        return next.map(c -> new StudentHomeSummaryDTO.RecommendedChapter(
                c.getChapterId(),
                c.getChapterName(),
                c.getSubject() != null ? c.getSubject().getSubjectName() : null
        )).orElse(null);
    }

}
