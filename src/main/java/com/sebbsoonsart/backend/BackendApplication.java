package com.sebbsoonsart.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory(System.getProperty("user.dir"))
				.filename(".env")
				.load();
		System.setProperty("GOOGLE_API_KEY", dotenv.get("GOOGLE_API_KEY"));
		System.setProperty("GOOGLE_FOLDER_ID", dotenv.get("GOOGLE_FOLDER_ID"));
		SpringApplication.run(BackendApplication.class, args);
	}

}
