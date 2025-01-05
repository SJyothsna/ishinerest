package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;

    @Autowired
    private QuestionRepository questionRepository;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> getQuestionsByChapter(Long chapterId) {
        return questionRepository.findByChapter_ChapterId(chapterId);
    }

    public List<Question> getQuestionsBySubject(Long subjectId) {
        return questionRepository.findBySubjectId(subjectId);
    }

    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    public List<Question> saveQuestions(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }


    // Unpracticed questions by subject
    public List<Question> getUnpracticedQuestionsBySubject(Long studentId, Long subjectId) {
        // Get practiced question IDs for the student
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findIncorrectlyAnsweredQuestionIds(studentId);

        // Get unpracticed questions for the subject
        return questionRepository.findUnpracticedQuestionsBySubject(subjectId, practicedQuestionIds);
    }

    // Unpracticed questions by chapter
    public List<Question> getUnpracticedQuestionsByChapter(Long studentId, Long chapterId) {
        // Get answered question IDs for the student and chapter
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        // Get unpracticed questions for the chapter
        return questionRepository.findUnpracticedQuestionsByChapter(chapterId, practicedQuestionIds);
    }
}
