package com.attendance.system.config;  // Use this if you place it under /config

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConnectionCheck {

    @Bean
    public CommandLineRunner init(MongoTemplate mongoTemplate) {
        return args -> {
            boolean exists = mongoTemplate.collectionExists("DailyCalendar");
            System.out.println("✅ MongoDB connection test: Collection 'DailyCalendar' exists? " + exists);
        };
    }
}
