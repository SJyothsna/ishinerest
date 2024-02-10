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
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final Firestore firestore;

    public QuestionService(FirebaseApp firebaseApp) {
        this.firestore = FirestoreClient.getFirestore(firebaseApp);
    }

    public List<Question> getAllQuestions() throws ExecutionException, InterruptedException {
        // Implement the logic to retrieve questions from Firestore
        // Example: firestore.collection("questions").get().get()
        // Convert Firestore documents to Question objects
        List<Question> questions = new ArrayList<>();
        CollectionReference questionsCollection = firestore.collection("questions");
        ApiFuture<QuerySnapshot> future = questionsCollection.get();
        try {
            QuerySnapshot querySnapshot = future.get();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                Question question = document.toObject(Question.class);
//                question.setId(document.getId());
                questions.add(question);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return questions;
    }
public void addQuestions(List<Question> questions){
    CollectionReference questionsRef = firestore.collection("questions");
  //  Question question = new Question("3","question3",1,"ans3","ch13","ch23","ch33","ch43",2,100102);
    questions.forEach(questionsRef::add);
//    ApiFuture<DocumentReference> result = questionsRef.add(question);
}

    public  List<Question> getQuestionsByTopic( long topicId) {

        CollectionReference questionsRef = firestore.collection("questions");
        Query query = questionsRef.whereEqualTo("topic", topicId);

        List<Question> questions = null;
        try {
            questions = query.get().get().getDocuments().stream()
                    .map(documentSnapshot -> documentSnapshot.toObject(Question.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return questions;
    }

    public  List<Question> getQuestionsBySubject( long subjectId) {

        CollectionReference questionsRef = firestore.collection("questions");
        long lRange = Long.parseLong(String.valueOf(subjectId)+"00");
        long uRange = Long.parseLong(String.valueOf(subjectId)+"99");

        Query query = questionsRef.whereGreaterThan("topic", lRange)
                .whereLessThanOrEqualTo("topic", uRange);
//questionsRef.whereGreaterThanOrEqualTo("topic", 10601).whereLessThan("topic", subjectId. ).get().get().getDocuments()


        List<Question> questions = null;
        try {
            questions = query.get().get().getDocuments().stream()
                    .map(documentSnapshot -> documentSnapshot.toObject(Question.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return questions;
    }

}
