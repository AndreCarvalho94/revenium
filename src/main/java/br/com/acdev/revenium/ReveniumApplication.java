package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReveniumApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReveniumApplication.class, args);
	}

}
