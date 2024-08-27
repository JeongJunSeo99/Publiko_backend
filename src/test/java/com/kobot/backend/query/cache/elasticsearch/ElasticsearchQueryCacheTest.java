package com.kobot.backend.query.cache.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.kobot.backend.CacheDto;
import com.kobot.backend.KobotBackendApplication;
import com.kobot.backend.query.cache.QueryCache;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = KobotBackendApplication.class)
@Slf4j
class ElasticsearchQueryCacheTest {

    @Autowired
    EmbeddingModel embeddingModel;

    @Autowired
    ElasticsearchClient esClient;

    QueryCache queryCache;

    @BeforeEach
    void before() {
        queryCache = new ElasticsearchCosineKnnQueryCache(embeddingModel, esClient);
    }

    @Test
    void cache() {
        CacheDto result = queryCache.cache("한국의 수도가 어디야?", "서울");
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void getCached() {
        CacheDto cached = queryCache.getCached("한국의 수도는?");
        Assertions.assertThat(cached).isNotNull();
        log.info("{}", cached);
    }

    @Test
    void clear() {
        queryCache.clear(0, System.currentTimeMillis());
    }
}