package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Section;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.entity.YoutubeVideo;
import com.ishine.ishinerest.pojo.VideoDto;
import com.ishine.ishinerest.pojo.VideoRequest;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.SectionRepository;
import com.ishine.ishinerest.repository.SubjectRepository;
import com.ishine.ishinerest.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeVideoService {

    private final YoutubeVideoRepository videoRepository;
    private final SubjectRepository      subjectRepository;
    private final ChapterRepository      chapterRepository;
    private final SectionRepository      sectionRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<VideoDto> getByChapter(String chapterId) {
        return videoRepository.findByChapterId(chapterId)
                .stream().map(v -> toDto(v, false)).toList();
    }

    public List<VideoDto> getBySubject(String subjectId) {
        return videoRepository.findBySubjectId(subjectId)
                .stream().map(v -> toDto(v, true)).toList();
    }

    public List<VideoDto> getBySection(String sectionId) {
        return videoRepository.findBySectionId(sectionId)
                .stream().map(v -> toDto(v, false)).toList();
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public VideoDto create(VideoRequest req) {
        YoutubeVideo video = new YoutubeVideo();
        applyRequest(video, req, true);
        return toDto(videoRepository.save(video), true);
    }

    public VideoDto update(Long videoId, VideoRequest req) {
        YoutubeVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Video not found: " + videoId));
        applyRequest(video, req, false);
        return toDto(videoRepository.save(video), true);
    }

    public void delete(Long videoId) {
        if (!videoRepository.existsById(videoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Video not found: " + videoId);
        }
        videoRepository.deleteById(videoId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Apply fields from a VideoRequest to a YoutubeVideo entity. */
    private void applyRequest(YoutubeVideo video, VideoRequest req, boolean requireFks) {
        if (req.getTitle() != null)          video.setTitle(req.getTitle());
        if (req.getYoutubeUrl() != null)     video.setYoutubeUrl(req.getYoutubeUrl());
        if (req.getYoutubeVideoId() != null) video.setYoutubeVideoId(req.getYoutubeVideoId());
        if (req.getDurationMinutes() != null) video.setDurationMinutes(req.getDurationMinutes());
        if (req.getDisplayOrder() != null)   video.setDisplayOrder(req.getDisplayOrder());

        if (req.getSubjectId() != null || requireFks) {
            String subjectId = req.getSubjectId();
            SubjectEntity subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Subject not found: " + subjectId));
            video.setSubject(subject);
        }

        if (req.getChapterId() != null || requireFks) {
            String chapterId = req.getChapterId();
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Chapter not found: " + chapterId));
            video.setChapter(chapter);
        }

        // Section is always optional; a null sectionId clears the association
        if (req.getSectionId() != null) {
            Section section = sectionRepository.findById(req.getSectionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Section not found: " + req.getSectionId()));
            video.setSection(section);
        } else if (requireFks) {
            video.setSection(null);
        }
    }

    /** Map entity → DTO. Pass includeChapterName=true for the /subject endpoint. */
    private VideoDto toDto(YoutubeVideo v, boolean includeChapterName) {
        VideoDto dto = new VideoDto();
        dto.setVideoId(v.getVideoId());
        dto.setTitle(v.getTitle());
        dto.setYoutubeUrl(v.getYoutubeUrl());
        dto.setYoutubeVideoId(v.getYoutubeVideoId());
        dto.setDurationMinutes(v.getDurationMinutes());
        dto.setDisplayOrder(v.getDisplayOrder());
        if (v.getChapter() != null) {
            dto.setChapterId(v.getChapter().getChapterId());
            if (includeChapterName) dto.setChapterName(v.getChapter().getChapterName());
        }
        if (v.getSection() != null) {
            dto.setSectionId(v.getSection().getSectionId());
            dto.setSectionName(v.getSection().getSectionName());
        }
        return dto;
    }
}
