package dev.deriou.airesume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiResumeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeApplication.class, args);
    }
}
