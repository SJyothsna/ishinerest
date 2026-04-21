package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.FlaggedQuestion;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FlaggedQuestionService {

        @Autowired
        private FlaggedQuestionRepository flaggedQuestionRepository;

        @Autowired
        private StudentRepository studentRepository;

        @Autowired
        private QuestionRepository questionRepository;

        @Autowired
        private PracticeSessionDetailRepository practiceSessionDetailRepository;

        /**
         * Flag a question for a student
         */
        @Transactional
        public FlaggedQuestion flagQuestion(Long studentId, Long questionId) {
                // Validate student exists
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found with ID: " + studentId));

                // Validate question exists
                Question question = questionRepository.findById(questionId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Question not found with ID: " + questionId));

                // Check if already flagged
                Optional<FlaggedQuestion> existing = flaggedQuestionRepository
                                .findByStudent_StudentIdAndQuestion_QuestionId(studentId, questionId);

                if (existing.isPresent()) {
                        // Already flagged, return existing
                        return existing.get();
                }

                // Create new flagged question
                FlaggedQuestion flaggedQuestion = new FlaggedQuestion();
                flaggedQuestion.setStudent(student);
                flaggedQuestion.setQuestion(question);

                return flaggedQuestionRepository.save(flaggedQuestion);
        }

        /**
         * Unflag a question for a student
         */
        @Transactional
        public void unflagQuestion(Long studentId, Long questionId) {
                // Validate student exists
                studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found with ID: " + studentId));

                // Validate question exists
                questionRepository.findById(questionId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Question not found with ID: " + questionId));

                // Delete the flagged question
                flaggedQuestionRepository.deleteByStudent_StudentIdAndQuestion_QuestionId(studentId, questionId);
        }

        /**
         * Get all flagged questions for a student in a specific chapter
         */
        public List<FlaggedQuestion> getFlaggedQuestionsByChapter(Long studentId, String chapterId) {
                // Validate student exists
                studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found with ID: " + studentId));

                return flaggedQuestionRepository.findByStudentIdAndChapterId(studentId, chapterId);
        }

        /**
         * Get all flagged questions for a student
         */
        public List<FlaggedQuestion> getAllFlaggedQuestions(Long studentId) {
                // Validate student exists
                studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found with ID: " + studentId));

                return flaggedQuestionRepository.findByStudent_StudentId(studentId);
        }

        /**
         * Get all flagged questions for a student in a specific chapter as
         * QuestionWithFlagDTO
         * (isFlagged will always be true since these are flagged questions)
         */
        public List<QuestionWithFlagDTO> getFlaggedQuestionsByChapterWithFlags(Long studentId, String chapterId) {
                List<FlaggedQuestion> flaggedQuestions = getFlaggedQuestionsByChapter(studentId, chapterId);
                return flaggedQuestions.stream()
                                .map(fq -> new QuestionWithFlagDTO(fq.getQuestion(), true))
                                .collect(Collectors.toList());
        }

        /**
         * Get all flagged questions for a student as QuestionWithFlagDTO
         * (isFlagged will always be true since these are flagged questions)
         */
        public List<QuestionWithFlagDTO> getAllFlaggedQuestionsWithFlags(Long studentId) {
                List<FlaggedQuestion> flaggedQuestions = getAllFlaggedQuestions(studentId);
                return flaggedQuestions.stream()
                                .map(fq -> new QuestionWithFlagDTO(fq.getQuestion(), true))
                                .collect(Collectors.toList());
        }

        /**
         * Get unpracticed flagged questions for a student in a specific chapter
         * Returns only flagged questions that haven't been correctly answered
         */
        public List<QuestionWithFlagDTO> getUnpracticedFlaggedQuestionsByChapter(Long studentId, String chapterId) {
                // Validate student exists
                studentRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found with ID: " + studentId));

                // Get all flagged questions for this student in this chapter
                List<FlaggedQuestion> flaggedQuestions = flaggedQuestionRepository
                                .findByStudentIdAndChapterId(studentId, chapterId);

                if (flaggedQuestions.isEmpty()) {
                        return new ArrayList<>();
                }

                // Get correctly answered question IDs
                List<Long> correctlyAnsweredIds = practiceSessionDetailRepository
                                .findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

                // Filter out correctly answered questions and convert to DTO
                return flaggedQuestions.stream()
                                .filter(fq -> !correctlyAnsweredIds.contains(fq.getQuestion().getQuestionId()))
                                .map(fq -> new QuestionWithFlagDTO(fq.getQuestion(), true))
                                .collect(Collectors.toList());
        }
}

// Made with Bob
