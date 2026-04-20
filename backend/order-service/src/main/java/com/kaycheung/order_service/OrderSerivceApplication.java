package com.kaycheung.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderSerivceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderSerivceApplication.class, args);
	}

}
