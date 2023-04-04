package com.jds.edgar.cik.download;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableScheduling
@EnableWebMvc
@OpenAPIDefinition
@SpringBootApplication
public class CikConversionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CikConversionApplication.class, args);
	}

}
