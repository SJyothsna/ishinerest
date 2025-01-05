package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.service.PracticeSessionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/practice-session-details")
public class PracticeSessionDetailController {

    @Autowired
    private PracticeSessionDetailService service;

    @GetMapping
    public List<PracticeSessionDetail> getAllSessionDetails() {
        return service.getAllSessionDetails();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PracticeSessionDetail> getSessionDetailById(@PathVariable Long id) {
        Optional<PracticeSessionDetail> sessionDetail = service.getSessionDetailById(id);
        return sessionDetail.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public List<PracticeSessionDetail> createSessionDetail(@RequestBody List<PracticeSessionDetail> sessionDetails) {
        return service.saveSessionDetails(sessionDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSessionDetail(@PathVariable Long id) {
        service.deleteSessionDetail(id);
        return ResponseEntity.ok().build();
    }


}