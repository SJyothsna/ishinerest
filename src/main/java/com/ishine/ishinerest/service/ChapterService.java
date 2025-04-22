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
import java.util.Comparator;
import java.util.List;

@Service
public class ChapterService {

    @Autowired
    private ChapterRepository chapterRepository;

    public List<Chapter> getChaptersBySubject(String subjectId) {
        List<Chapter> chapters = chapterRepository.findBySubject_SubjectId(subjectId);
        chapters.sort(Comparator.comparing(Chapter::getChapterName));
        return chapters;    }

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
            Workbook workbook = WorkbookFactory.create(inputStream); // Supports .xls and .xlsx
            Sheet sheet = workbook.getSheet("Chapters");

            if (sheet == null) {
                workbook.close();
                return "Sheet named 'Chapters' not found in the Excel file.";
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell idCell = row.getCell(0);
                Cell subidCell = row.getCell(1);
                Cell nameCell = row.getCell(2);

                if (idCell == null || subidCell == null || nameCell == null) continue;

                String chapterId = idCell.getStringCellValue().trim();
                String subjectId = subidCell.getStringCellValue().trim();
                String chapterName = nameCell.getStringCellValue().trim();

                if (chapterId.isEmpty() || subjectId.isEmpty() || chapterName.isEmpty()) continue;

                if (!chapterRepository.existsById(chapterId)) {
                    Chapter chapter = new Chapter();
                    chapter.setChapterId(chapterId);
                    chapter.setChapterName(chapterName);

                    SubjectEntity subject = new SubjectEntity();
                    subject.setSubjectId(subjectId);
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
