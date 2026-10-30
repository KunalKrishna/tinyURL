package com.abitmanipulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
//@Configuration
//@ComponentScan
//@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class UrlShortnerApp {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortnerApp.class, args);
	}

}
