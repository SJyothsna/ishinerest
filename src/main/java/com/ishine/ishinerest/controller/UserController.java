package com.ishine.ishinerest.controller;
import com.ishine.ishinerest.firebase.UserService;
import com.ishine.ishinerest.pojo.QuestionIdsRequest;
import com.ishine.ishinerest.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    public UserService userService;

//    public UserController(UserService userService) {
//        this.userService = userService;
//    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getUsersFromFirestore();
    }

    @PostMapping
    public void addSubjectsToUser() throws ExecutionException, InterruptedException {
        // Assuming userId is the ID of the user document
        String userId = "1";
        List<String> subjectIds = List.of("1", "2");

// Add subjects to the user document
        userService.addSubjectsToUser(userId, subjectIds);

// Retrieve the user document with subjects
        User userWithSubjects = userService.getUserWithSubjects(userId);
        System.out.println("User with Subjects: " + userWithSubjects);

    }
    @PostMapping("/{studentId}/practice")
    public void updateStidentProgress(@PathVariable String studentId,
                                                  @RequestBody QuestionIdsRequest questionIdsRequest) throws ExecutionException, InterruptedException {

        userService.updateStudentProgress(studentId,questionIdsRequest.getQuestionIds());
    }
}
