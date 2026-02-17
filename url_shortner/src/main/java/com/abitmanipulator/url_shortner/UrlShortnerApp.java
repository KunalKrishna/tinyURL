package com.abitmanipulator.url_shortner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@Configuration
//@ComponentScan
//@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class UrlShortnerApp {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortnerApp.class, args);
	}

}
