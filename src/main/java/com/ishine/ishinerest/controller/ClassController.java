package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.ClassEntity;
import com.ishine.ishinerest.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping
    public List<ClassEntity> getAllClasses() {
        return classService.getAllClasses();
    }

    @PostMapping
    public ClassEntity saveClass(@RequestBody ClassEntity classEntity) {
        return classService.saveClass(classEntity);
    }
    
    @PutMapping("/{classId}")
    public ClassEntity updateClass(@PathVariable String classId, @RequestBody ClassEntity classEntity) {
    return classService.updateClass(classId, classEntity);
}

    
    @DeleteMapping("/{classId}")
    public void deleteClass(@PathVariable String classId) {
    classService.deleteClassById(classId);
}

}
