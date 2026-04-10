package org.kontrolla;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the Kontrolla backend application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class KontrollaApplication {

	static void main(String[] args) {
		SpringApplication.run(KontrollaApplication.class, args);
	}

}
