package dev.deriou.airesume.config;

import dev.deriou.airesume.llm.LlmProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient deepSeekRestClient(LlmProperties properties, RestClient.Builder builder) {
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer(LlmProperties properties) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.timeout());
            requestFactory.setReadTimeout(properties.timeout());
            builder.requestFactory(requestFactory);
        };
    }
}
