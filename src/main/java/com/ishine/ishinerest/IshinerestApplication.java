package com.ishine.ishinerest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class IshinerestApplication {

	public static void main(String[] args) {
//		initializeFirebase();
		SpringApplication.run(IshinerestApplication.class, args);
	}

}
