package com.abitmanipulator.url_shortner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
//@Configuration
//@ComponentScan
//@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
public class SpringBootUrlShortnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootUrlShortnerApplication.class, args);
	}

}
