package com.ishine.ishinerest.service;


import com.ishine.ishinerest.entity.ClassEntity;
import com.ishine.ishinerest.pojo.PrevExamQuestionResponse;
import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.entity.PrevExamQuestion;
import com.ishine.ishinerest.repository.PrevExamPaperRepository;
import com.ishine.ishinerest.repository.PrevExamQuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrevExamQuestionService {

    @Autowired
    private PrevExamPaperRepository paperRepo;

    @Autowired
    private PrevExamQuestionRepository questionRepo;
    private final PrevExamQuestionRepository prevExamQuestionRepository;


    public PrevExamQuestionService(PrevExamQuestionRepository prevExamQuestionRepository) {
        this.prevExamQuestionRepository = prevExamQuestionRepository;
    }

    public List<PrevExamQuestionResponse> getQuestionsByChapterId(String chapterId) {
        return prevExamQuestionRepository.findQuestionsByChapterId(chapterId);
    }
    public List<PrevExamPaper> getAllPrevExamPapers() {
        return paperRepo.findAll();
    }
}
