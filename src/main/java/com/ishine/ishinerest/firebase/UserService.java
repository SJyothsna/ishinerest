package com.ishine.ishinerest.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import com.ishine.ishinerest.pojo.Question;
import com.ishine.ishinerest.pojo.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private final Firestore firestore;

    public UserService(FirebaseApp firebaseApp) {
        this.firestore = FirestoreClient.getFirestore(firebaseApp);
    }
    public List<User> getUsersFromFirestore() {
        List<User> users = new ArrayList<>();
        // Reference to the "users" collection
        CollectionReference usersCollection = firestore.collection("users");

        // Asynchronously retrieve all documents in the "users" collection
        ApiFuture<QuerySnapshot> future = usersCollection.get();
        try {
            QuerySnapshot querySnapshot = future.get();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                // Access document data as a map
                User user = document.toObject(User.class);
            //    user.setId(document.getId());
                users.add(user);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return users;
    }

    public void addSubjectsToUser(String userId, List<String> subjectIds) {
        // Reference to the "users" collection

        CollectionReference usersCollection = firestore.collection("users");

        // Reference to the specific user document
        DocumentReference userDocument = usersCollection.document(userId);

        // Update the "subjects" field in the user document
        userDocument.update("subjects", subjectIds);
    }

    public User getUserWithSubjects(String userId) throws ExecutionException, InterruptedException {
        CollectionReference usersCollection = firestore.collection("users");
        DocumentReference userDocument = usersCollection.document(userId);
        DocumentSnapshot document = userDocument.get().get();

        if (document.exists()) {
            User user = document.toObject(User.class);
            assert user != null;
            return user;
        } else {
            System.out.println("No such document");
            return null;
        }
    }

    private List<String> getUserQuestions(String userId) throws ExecutionException, InterruptedException {
        CollectionReference usersCollection = firestore.collection("users");
        DocumentReference userDocument = usersCollection.document(userId);
        DocumentSnapshot document = userDocument.get().get();
        if (document.exists()) {
            User user = document.toObject(User.class);
            assert user != null;
            return user.getQuestions();
        } else {
            System.out.println("No such document");
            return null;
        }
    }
    public void updateStudentProgress(String studentId, List<String> questionIds) throws ExecutionException, InterruptedException {
        CollectionReference usersCollection = firestore.collection("users");
        DocumentReference userDocument = usersCollection.document(studentId);
        List<String> existingQuestionIds = getUserQuestions(studentId);
        existingQuestionIds.addAll(questionIds);
        userDocument.update("questions", existingQuestionIds);


        //  Question question = new Question("3","question3",1,"ans3","ch13","ch23","ch33","ch43",2,100102);
//        questions.forEach(questionsRef::add);
//    ApiFuture<DocumentReference> result = questionsRef.add(question);
    }
}

