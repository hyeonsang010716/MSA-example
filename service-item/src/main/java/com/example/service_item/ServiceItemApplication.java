package com.example.service_item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceItemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceItemApplication.class, args);
	}

}
