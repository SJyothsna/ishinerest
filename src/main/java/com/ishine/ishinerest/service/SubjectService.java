package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    public List<SubjectEntity> getSubjectsByClass(String classId) {
        return subjectRepository.findByClassEntity_ClassId(classId);
    }

    public SubjectEntity saveSubject(SubjectEntity subject) {
        return subjectRepository.save(subject);
    }

        public List<SubjectEntity> saveSubjects(List<SubjectEntity> subjects) {
        return subjectRepository.saveAll(subjects);
    }

    // Update subject
    public SubjectEntity updateSubject(String subjectId, SubjectEntity subjectDetails) {
        Optional<SubjectEntity> optionalSubject = subjectRepository.findById(subjectId);

        if (optionalSubject.isPresent()) {
            SubjectEntity subject = optionalSubject.get();
            subject.setSubjectName(subjectDetails.getSubjectName());
            subject.setClassEntity(subjectDetails.getClassEntity());
            return subjectRepository.save(subject);
        } else {
            throw new RuntimeException("Subject not found with ID: " + subjectId);
        }
    }

    public SubjectEntity findById(String subjectId) {
        Optional<SubjectEntity> subjectOptional = subjectRepository.findById(subjectId);
        if (subjectOptional.isPresent()) {
            return subjectOptional.get();
        } else {
            throw new IllegalArgumentException("Subject with ID " + subjectId + " not found");
        }
    }

    // Delete subject
    public void deleteSubject(String subjectId) {
        subjectRepository.deleteById(subjectId);
    }
}
