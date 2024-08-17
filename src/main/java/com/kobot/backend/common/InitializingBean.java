package com.kobot.backend.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitializingBean {

    private final ElasticsearchClient client;
    private final String indexName = "spring-ai-document-index";

    @PostConstruct
    public void init() {
        try {
            boolean indexExists = client.indices().exists(b -> b.index(indexName)).value();
            if (!indexExists) {
                client.indices().create(c -> c.index(indexName));
                log.info("Index {} created.", indexName);
            } else {
                log.info("Index {} already exists.", indexName);
            }
        } catch (IOException e) {
            log.error("Error checking or creating index: {}", e.getMessage(), e);
        }
    }
}
