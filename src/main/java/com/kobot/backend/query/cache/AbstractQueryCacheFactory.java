package com.kobot.backend.query.cache;

import org.springframework.ai.embedding.EmbeddingModel;

public abstract class AbstractQueryCacheFactory {

    // TODO Metric은 객체 하나로 묶어서 받기
    public abstract QueryCache create(
        EmbeddingModel embeddingModel,
        DistanceMetric distanceMetric,
        NeighborMetric neighborMetric,
        float threshold);

    // TODO 별도 클래스로 분리
    public enum DistanceMetric {
        DOT_PRODUCT, COSINE
    }

    // TODO 별도 클래스로 분리
    public enum NeighborMetric {
        KNN, ANN
    }
}
