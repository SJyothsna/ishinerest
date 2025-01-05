package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PracticeSessionDetailService {

    @Autowired
    private PracticeSessionDetailRepository repository;

    public List<PracticeSessionDetail> getAllSessionDetails() {
        return repository.findAll();
    }

    public Optional<PracticeSessionDetail> getSessionDetailById(Long id) {
        return repository.findById(id);
    }

    public PracticeSessionDetail saveSessionDetail(PracticeSessionDetail sessionDetail) {
        return repository.save(sessionDetail);
    }
    public List<PracticeSessionDetail> saveSessionDetails(List<PracticeSessionDetail> sessionDetails) {
        return repository.saveAll(sessionDetails);
    }
    public void deleteSessionDetail(Long id) {
        repository.deleteById(id);
    }
}