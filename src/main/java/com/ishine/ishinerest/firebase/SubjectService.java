package com.ishine.ishinerest.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.ishine.ishinerest.pojo.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class SubjectService {
    public List<User> getUsersFromFirestore() {
        List<User> users = new ArrayList<>();
        // Reference to the "users" collection
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference usersCollection = firestore.collection("users");

        // Asynchronously retrieve all documents in the "users" collection
        ApiFuture<QuerySnapshot> future = usersCollection.get();
        try {
            QuerySnapshot querySnapshot = future.get();

            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                // Access document data as a map
                System.out.println("User ID: " + document.getId());
                System.out.println("Email: " + document.getString("email"));
                System.out.println("Name: " + document.getString("name"));
                System.out.println("------------");
                users.add(document.toObject(User.class));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return users;
    }
}

