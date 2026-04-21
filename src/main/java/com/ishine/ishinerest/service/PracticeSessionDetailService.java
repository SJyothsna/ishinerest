package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PracticeSessionDetailService {

    @Autowired
    private PracticeSessionDetailRepository repository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private FlaggedQuestionRepository flaggedQuestionRepository;

    public List<PracticeSessionDetail> getAllSessionDetails() {
        return repository.findAll();
    }

    public List<PracticeSessionDetail> getSessionDetailByStudentId(Long studentId) {
        return repository.findByStudentId(studentId);
    }

    public PracticeSessionDetail saveSessionDetail(PracticeSessionDetail sessionDetail) {
        return repository.save(sessionDetail);
    }

    public List<PracticeSessionDetail> saveSessionDetails(Long studentId, List<PracticeSessionDetail> sessionDetails) {
        List<PracticeSessionDetail> saved = new ArrayList<>();

        // Load student entity once
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        for (PracticeSessionDetail detail : sessionDetails) {
            Long questionId = detail.getQuestionId();

            // Load question entity
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

            Optional<PracticeSessionDetail> existing = repository.findByStudentIdAndQuestionId(studentId, questionId);

            if (existing.isPresent()) {
                PracticeSessionDetail existingDetail = existing.get();
                existingDetail.setStudentAnswer(detail.getStudentAnswer());
                existingDetail.setIsCorrect(detail.getIsCorrect());
                existingDetail.setAttemptCount(existingDetail.getAttemptCount() + 1); // increment attempt
                saved.add(repository.save(existingDetail));
            } else {
                detail.setStudent(student);
                detail.setQuestion(question);
                detail.setAttemptCount(1);
                saved.add(repository.save(detail));
            }
        }

        return saved;
    }

    public void deleteSessionDetail(Long id) {
        repository.deleteById(id);
    }

    public void resetChapterProgress(Long studentId, String chapterId) {
        repository.deleteByStudentIdAndChapterId(studentId, chapterId);
    }

    /**
     * Get wrong answers (incorrectly answered questions) for a student in a chapter
     * with flag status
     */
    public List<QuestionWithFlagDTO> getWrongAnswersByChapterWithFlags(Long studentId, String chapterId) {
        // Get incorrectly answered question IDs
        List<Long> wrongQuestionIds = repository.findIncorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (wrongQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get the questions
        List<Question> wrongQuestions = questionRepository.findAllById(wrongQuestionIds);

        // Get flagged question IDs for this student
        Set<Long> flaggedQuestionIds = flaggedQuestionRepository
                .findByStudent_StudentId(studentId)
                .stream()
                .map(fq -> fq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        // Convert to DTOs with flag status
        return wrongQuestions.stream()
                .map(q -> new QuestionWithFlagDTO(q, flaggedQuestionIds.contains(q.getQuestionId())))
                .collect(Collectors.toList());
    }
}