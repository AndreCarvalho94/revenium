package br.com.acdev.revenium;

import org.springframework.boot.SpringApplication;

public class TestReveniumApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReveniumApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
