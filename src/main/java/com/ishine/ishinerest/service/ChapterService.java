package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.repository.ChapterRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChapterService {

    @Autowired
    private ChapterRepository chapterRepository;

    public List<Chapter> getChaptersBySubject(String subjectId) {
        return chapterRepository.findBySubject_SubjectId(subjectId);
    }

//    public List<Question> getQuestionsByChapterId(Long chapterId) {
//        Chapter chapter = chapterRepository.findById(chapterId)
//                .orElseThrow(() -> new RuntimeException("Chapter not found with ID: " + chapterId));
//
//        return chapter.getQuestions();
//    }
//
//        public List<ExamQuestion> getExamQuestionsByChapter(Long chapterId) {
//        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
//        return chapter != null ? chapter.getExamQuestions() : null;
//    }

    public Chapter saveChapter(Chapter chapter) {
        return chapterRepository.save(chapter);
    }

    
    public List<Chapter> saveChapters(List<Chapter> chapters) {
        return chapterRepository.saveAll(chapters);
    }

        // Delete subject
        public void deleteChapter(String chapterId) {
            chapterRepository.deleteById(chapterId);
        }
    @Transactional
    public void deleteChaptersBySubject(String subjectId) {
        chapterRepository.deleteBySubjectId(subjectId);
    }
    public String uploadChaptersFromExcel(MultipartFile file) {
        try {
            List<Chapter> newChapters = new ArrayList<>();
            InputStream inputStream = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skipping header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell idCell = row.getCell(0);
                Cell subidCell = row.getCell(1);
                Cell nameCell = row.getCell(2);

                if (nameCell == null || nameCell.getCellType() == CellType.BLANK) continue;

                String chapterId = null;
                if (idCell != null && idCell.getCellType() == CellType.NUMERIC) {
                    chapterId = String.valueOf(idCell.getNumericCellValue());
                }

                if (chapterId == null || !chapterRepository.existsById(chapterId)) {
                    Chapter chapter = new Chapter();
                    chapter.setChapterName(nameCell.getStringCellValue());

                    SubjectEntity subject = new SubjectEntity();
                    subject.setSubjectId(String.valueOf(subidCell.getNumericCellValue()));
                    chapter.setSubject(subject);

                    newChapters.add(chapter);
                }
            }

            workbook.close();

            if (!newChapters.isEmpty()) {
                chapterRepository.saveAll(newChapters);
            }

            return "Uploaded successfully. Added " + newChapters.size() + " new chapters.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to upload chapters: " + e.getMessage();
        }
    }
}
