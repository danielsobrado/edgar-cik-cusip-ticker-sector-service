package com.jds.edgar.cik.download;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CikConversionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CikConversionApplication.class, args);
	}

}
