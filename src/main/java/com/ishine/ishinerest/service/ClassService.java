package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.ClassEntity;
import com.ishine.ishinerest.repository.ClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {

    @Autowired
    private ClassRepository classRepository;

    public List<ClassEntity> getAllClasses() {
        return classRepository.findAll();
    }

    public ClassEntity saveClass(ClassEntity classEntity) {
        return classRepository.save(classEntity);
    }

    // Delete a class by ID
    public void deleteClassById(String classId) {
        classRepository.deleteById(classId);
    }

    // Update a class by ID
    public ClassEntity updateClass(String classId, ClassEntity classEntity) {
        // Find the existing class
        ClassEntity existingClass = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + classId));

        // Update the existing class with new details
        existingClass.setClassName(classEntity.getClassName());

        // Save the updated class
        return classRepository.save(existingClass);
    }
}
