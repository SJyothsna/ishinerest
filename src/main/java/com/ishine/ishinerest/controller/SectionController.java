package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
public class SectionController {

    private final ChapterService chapterService;

    @GetMapping("/chapters/{chapterId}")
    public List<String> getSectionsForChapter(@PathVariable String chapterId) {
        return chapterService.getSectionsForChapter(chapterId);
    }
}

// Made with Bob
