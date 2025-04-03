package com.ishine.ishinerest.service;


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

    public List<PrevExamQuestionResponse> getQuestionsByChapterAndLevel(String chapterId, String level) {
        List<PrevExamPaper> papers = paperRepo.findByLevel(level);
        Set<Integer> examIds = papers.stream()
                .map(PrevExamPaper::getExamId)
                .collect(Collectors.toSet());

        List<PrevExamQuestion> questions = questionRepo.findByChapterId(chapterId);

        return questions.stream()
                .filter(q -> examIds.contains(q.getExamId()))
                .map(q -> {
                    PrevExamPaper paper = papers.stream()
                            .filter(p -> p.getExamId().equals(q.getExamId()))
                            .findFirst()
                            .orElse(null);
                    if (paper != null) {
                        return new PrevExamQuestionResponse(
                                paper.getExamId(),
                                paper.getExamYear(),
                                paper.getType(),
                                q.getQId(),
                                q.getSection(),
                                q.getQuestion(),
                                q.getMarkingScheme()
                        );
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
