package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.ExamQuestion;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.service.ChapterService;
import com.ishine.ishinerest.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/chapters")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public List<Chapter> getChaptersBySubject(@RequestParam String subjectId) {
        return chapterService.getChaptersBySubject(subjectId);
    }

//    @GetMapping("/{chapterId}/questions")
//    public List<Question> getQuestionsByChapterId(@PathVariable Long chapterId) {
//        return chapterService.getQuestionsByChapterId(chapterId);
//    }
//
//    @GetMapping("/{chapterId}/examquestions")
//    public List<ExamQuestion> getExamQuestionsByChapter(@PathVariable Long chapterId) {
//        return chapterService.getExamQuestionsByChapter(chapterId);
//    }

    @PostMapping("/subject/{subjectId}")
    public List<Chapter> saveChapters(@PathVariable String subjectId, @RequestBody List<Chapter> chapters) {
        chapters.forEach(chapter -> {
            chapter.setSubject(new SubjectEntity());
            chapter.getSubject().setSubjectId(subjectId);

        });
        return chapterService.saveChapters(chapters);
    }

    @DeleteMapping("/{chapterId}")
    public String deleteSubject(@PathVariable String chapterId) {
        chapterService.deleteChapter(chapterId);
        return "Chapter with ID " + chapterId + " has been deleted.";
    }

    @DeleteMapping
    public String deleteChaptersBySubject(@RequestParam String subjectId) {
        chapterService.deleteChaptersBySubject(subjectId);
        return "Chapters with SubjectID " + subjectId + " has been deleted.";
    }

    @PostMapping("/upload")
    public String uploadChaptersFromExcel(@RequestParam("file") MultipartFile file) {
        return chapterService.uploadChaptersFromExcel(file);
    }
}

