package com.cleancoders.postgresqltranscations;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PostgresqltranscationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostgresqltranscationsApplication.class, args);
	}

	@Bean
	public OpenAPI metadata() {
		return new OpenAPI().info(
				new Info().title("Employee Service Documentation")
						.description("API documentation for Employee Service")
						.version("1.0")
		);
	}
}


