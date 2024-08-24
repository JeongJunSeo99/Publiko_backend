package com.kobot.backend.query.cache;

import org.springframework.ai.embedding.EmbeddingModel;

public abstract class QueryCacheFactory {

    public abstract QueryCache create(
        EmbeddingModel embeddingModel,
        DistanceMetric distanceMetric,
        NeighborMetric neighborMetric,
        float threshold);

    public enum DistanceMetric {
        DOT_PRODUCT, COSINE
    }

    public enum NeighborMetric {
        KNN, ANN
    }
}
