package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PracticeSessionDetailService {

    @Autowired
    private PracticeSessionDetailRepository repository;

    public List<PracticeSessionDetail> getAllSessionDetails() {
        return repository.findAll();
    }

    public List<PracticeSessionDetail> getSessionDetailByStudentId(Long studentId) {
        return repository.findByStudentId(studentId);
    }

    public PracticeSessionDetail saveSessionDetail(PracticeSessionDetail sessionDetail) {
        return repository.save(sessionDetail);
    }
    public List<PracticeSessionDetail> saveSessionDetails(Long studentId, List<PracticeSessionDetail> sessionDetails) {
        List<PracticeSessionDetail> saved = new ArrayList<>();

        for (PracticeSessionDetail detail : sessionDetails) {
            Optional<PracticeSessionDetail> existing = repository.findByStudentIdAndQuestionId(studentId, detail.getQuestionId());

            if (existing.isPresent()) {
                PracticeSessionDetail existingDetail = existing.get();
                existingDetail.setStudentAnswer(detail.getStudentAnswer());
                existingDetail.setIsCorrect(detail.getIsCorrect());
                existingDetail.setAttemptCount(existingDetail.getAttemptCount() + 1);  // increment attempt
                saved.add(repository.save(existingDetail));
            } else {
                detail.setStudentId(studentId);
                detail.setAttemptCount(1);
                saved.add(repository.save(detail));
            }
        }

        return saved;
    }


    public void deleteSessionDetail(Long id) {
        repository.deleteById(id);
    }
}