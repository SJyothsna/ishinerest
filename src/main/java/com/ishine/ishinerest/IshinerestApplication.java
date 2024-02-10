package com.ishine.ishinerest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.ishine.ishinerest.firebase.FirebaseInitializer.initializeFirebase;

@SpringBootApplication
public class IshinerestApplication {

	public static void main(String[] args) {
//		initializeFirebase();
		SpringApplication.run(IshinerestApplication.class, args);
	}

}
