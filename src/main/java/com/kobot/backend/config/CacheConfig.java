package com.kobot.backend.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.kobot.backend.query.cache.QueryCache;
import com.kobot.backend.query.cache.QueryCacheFactory;
import com.kobot.backend.query.cache.QueryCacheFactory.DistanceMetric;
import com.kobot.backend.query.cache.QueryCacheFactory.NeighborMetric;
import com.kobot.backend.query.cache.elasticsearch.ElasticsearchQueryCacheFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Value("${com.kobot.query.cache.threshold}")
    private float threshold;

    @Bean
    public QueryCache getQueryCache(ElasticsearchClient esClient, EmbeddingModel embeddingModel) {
        QueryCacheFactory elasticsearchQueryCacheFactory = new ElasticsearchQueryCacheFactory(
            esClient);
        
        return elasticsearchQueryCacheFactory.create(embeddingModel, DistanceMetric.COSINE,
            NeighborMetric.KNN,
            threshold);
    }
}
