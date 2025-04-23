package com.ishine.ishinerest.service;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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

    public void saveQuestionsFromExcel(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet("Prev_Exam_Questions");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'Prev_Exam_Questions' not found.");
            }

            List<PrevExamQuestion> questions = new ArrayList<>();
            int rowNum = 0;
            for (Row row : sheet) {
                // Skip header
                if (rowNum++ == 0) continue;

                PrevExamQuestion q = new PrevExamQuestion();
                q.setExamId(getStringValue(row.getCell(0)));
                q.setChapterId(getStringValue(row.getCell(1)));
                q.setQId(getStringValue(row.getCell(2)));
                q.setPaperName(getStringValue(row.getCell(3)));
                q.setQuestion(getStringValue(row.getCell(4)));
                q.setMarkingScheme(getStringValue(row.getCell(5)));

                questions.add(q);
            }

            prevExamQuestionRepository.saveAll(questions);
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    public List<PrevExamQuestionResponse> getPaperDetailsBySubjectId(String subjectId) {
        return prevExamQuestionRepository.findQuestionsBySubjectId(subjectId);
    }

}
