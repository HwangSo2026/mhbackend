package com.hwangso.mhbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Redis Hash Value(JSON) 직렬화/역직렬화를 위해 전역 ObjectMapper Bean 제공
// - Repository 계층에서 직접 주입받아 사용
// - LocalDate 등 Java Time 타입 대응
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 Date/Time (LocalDate, LocalDateTime 등) 지원
        mapper.registerModule(new JavaTimeModule());

        // timestamp 대신 ISO-8601 문자열로
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}