package com.cleancoders.postgresqltranscations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General application beans that don't belong to the JPA layer.
 */
@Configuration
public class AppConfig {

    /**
     * Shared, thread-safe {@link ObjectMapper} bean for JSON serialization/deserialization.
     * Required for JSON Patch operations and JSON processing in EmployeeService.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Shared, thread-safe {@link ModelMapper} bean configured once with STRICT strategy.
     * All field names between EmployeeDTO and Employee are identical, so STRICT is safe
     * and avoids accidental mis-mappings introduced by LOOSE matching.
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}

