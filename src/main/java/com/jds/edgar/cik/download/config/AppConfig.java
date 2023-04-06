package com.jds.edgar.cik.download.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String userAgentName;
    private String userEmail;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor userAgentInterceptor = (request, body, execution) -> {
            request.getHeaders().set("User-Agent", userAgentName + " " + userEmail);
            request.getHeaders().set("Accept-Encoding", "gzip, deflate");
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(Collections.singletonList(userAgentInterceptor));

        return restTemplate;
    }

}
