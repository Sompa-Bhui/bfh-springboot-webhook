package com.example.bfh;

import com.example.bfh.config.AppProperties;
import com.example.bfh.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(20_000);
        return new RestTemplate(factory);
    }

    @Bean
    public CommandLineRunner run(WebhookService service, AppProperties props) {
        return args -> {
            log.info("Starting BFH Spring Boot webhook flow...");
            log.info("Configured user: name='{}', regNo='{}', email='{}'", props.getName(), props.getRegNo(), props.getEmail());
            if (props.getFinalQuery() == null || props.getFinalQuery().isBlank()) {
                log.warn("No finalQuery configured. Set 'bfh.final-query' in application.yml or environment to your SQL answer.");
            }
            service.executeFlow();
            log.info("Flow finished.");
        };
    }
}
