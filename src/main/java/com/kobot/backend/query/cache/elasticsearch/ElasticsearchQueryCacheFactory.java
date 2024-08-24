package com.kobot.backend.query.cache.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.kobot.backend.query.cache.AbstractQueryCacheFactory;
import com.kobot.backend.query.cache.QueryCache;
import org.springframework.ai.embedding.EmbeddingModel;

public class ElasticsearchQueryCacheFactory extends AbstractQueryCacheFactory {

    private final ElasticsearchClient esClient;

    // support metrics
    private final MetricPair cosineKnn = new MetricPair(DistanceMetric.COSINE, NeighborMetric.KNN);


    public ElasticsearchQueryCacheFactory(ElasticsearchClient esClient) {
        if (esClient == null) {
            throw new IllegalArgumentException("'esClient' must not be null.");
        }
        this.esClient = esClient;
    }

    @Override
    public QueryCache create(EmbeddingModel embeddingModel, DistanceMetric distanceMetric,
        NeighborMetric neighborMetric, float threshold) {
        if (embeddingModel == null) {
            throw new IllegalArgumentException("'embeddingMode' must not be null.");
        }

        MetricPair metricPair = new MetricPair(distanceMetric, neighborMetric);

        if (cosineKnn.equals(metricPair)) {
            return new ElasticsearchCosineKnnQueryCache(embeddingModel, esClient);
        }

        throw new IllegalArgumentException(
            "distanceMetric " + distanceMetric + " and neighborMetric " + neighborMetric
                + "are not supported.");
    }

    record MetricPair(DistanceMetric distanceMetric, NeighborMetric neighborMetric) {

    }
}
