package com.funchat.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		System.setProperty("USER_ID", dotenv.get("USER_ID"));
		System.setProperty("USER_PW", dotenv.get("USER_PW"));

		SpringApplication.run(DemoApplication.class, args);
	}

}
