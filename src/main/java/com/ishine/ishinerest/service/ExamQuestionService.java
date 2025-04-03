package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.ExamQuestion;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.repository.ExamQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExamQuestionService {

    @Autowired
    private ExamQuestionRepository examQuestionRepository;

    // Create or update an ExamQuestion
    public ExamQuestion saveExamQuestion(ExamQuestion examQuestion) {
        return examQuestionRepository.save(examQuestion);
    }

        public List<ExamQuestion> saveExamQuestions(List<ExamQuestion> examQuestions) {
        return examQuestionRepository.saveAll(examQuestions);
    }


    public List<ExamQuestion> getExamQuestionsByChapter(String chapterId) {
        return examQuestionRepository.findByChapter_ChapterId(chapterId);
    }

    // Get all ExamQuestions
    public List<ExamQuestion> getAllExamQuestions() {
        return examQuestionRepository.findAll();
    }

    // Get a specific ExamQuestion by ID
    public Optional<ExamQuestion> getExamQuestionById(Long id) {
        return examQuestionRepository.findById(id);
    }

    // Delete an ExamQuestion by ID
    public void deleteExamQuestionById(Long id) {
        examQuestionRepository.deleteById(id);
    }
}
