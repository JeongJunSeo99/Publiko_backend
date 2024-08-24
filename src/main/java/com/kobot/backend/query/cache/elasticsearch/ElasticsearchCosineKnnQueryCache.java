package com.kobot.backend.query.cache.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.kobot.backend.CacheDto;
import com.kobot.backend.query.cache.QueryCache;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.beans.factory.annotation.Value;


public class ElasticsearchCosineKnnQueryCache implements QueryCache {


    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient client;

    private final Logger log = LoggerFactory.getLogger(ElasticsearchCosineKnnQueryCache.class);

    private final float threshold = 0.8f;

    @Value("com.kobot.query.cache.elasticsearch.index-name")
    private String indexName = "kobot-user-query-index"; // FIXME pojo로

    private final long topK = 1L;

    public ElasticsearchCosineKnnQueryCache(EmbeddingModel embeddingModel,
        ElasticsearchClient client) {
        this.embeddingModel = embeddingModel;
        this.client = client;
    }

    public ElasticsearchCosineKnnQueryCache(EmbeddingModel embeddingModel,
        ElasticsearchClient client,
        String indexName) {
        this.embeddingModel = embeddingModel;
        this.client = client;
        this.indexName = indexName;
    }

    @Override
    public CacheDto cache(String query, String answer) {
        // metadata set
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("answer", answer); // 필드 이름은 상수로 빼기
        metadata.put("timestamp", System.currentTimeMillis());

        // document set
        Document document = new Document(query, metadata);

        // embedding
        document.setEmbedding(embeddingModel.embed(document));

        IndexRequest<Document> indexRequest = new IndexRequest.Builder<Document>()
            .index(indexName)
            .id(UUID.randomUUID().toString())
            .document(document)
            .build();

        // insert
        try {
            client.index(indexRequest);
        } catch (ElasticsearchException | IOException e) {
            log.error("", e);
            return null;
        }

        return convertToCachedDto(document);
    }

    @Override
    public CacheDto getCached(String query) {
        float[] vectors = embeddingModel.embed(query);

        SearchResponse<Document> res;
        try {
            res = client.search(
                sr -> sr.index(indexName)
                    .knn(knn -> knn.queryVector(EmbeddingUtils.toList(vectors))
                        .similarity(threshold)
                        .k(topK)
                        .field("embedding")
                        .numCandidates((long) (1.5 * topK))
                    ),
                Document.class);
        } catch (IOException e) {
            log.error("", e);
            return null;
        }

        log.debug("Score: {}", res.maxScore());

        // TODO 아래 공통화
        List<CacheDto> cacheDtos = res.hits().hits().stream().map(r -> {
            Document source = r.source();
            if (source == null) {
                log.debug("'source' field is null from {}", r);
                return null;
            }

            source.getMetadata().put("distance", r.score().floatValue());
            return convertToCachedDto(source);
        }).toList();
        if (cacheDtos.isEmpty()) {
            return null;
        }

        return cacheDtos.getFirst();
    }

    @Override
    public void clear(long from, long to) {

        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest.Builder()
            .index(indexName)
            .query(
                RangeQuery.of(
                        rq -> rq.gte(JsonData.of(from)).lte(JsonData.of(to))
                            .field("metadata.timestamp"))
                    ._toQuery())
            .build();

        try {
            DeleteByQueryResponse response = client.deleteByQuery(deleteByQueryRequest);
            log.info("Cached data which created between {} and {} is deleted.", from, to);
            log.debug("Deleted data: {}", response);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private CacheDto convertToCachedDto(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        Object distance = metadata.get("distance");

        return CacheDto.builder()
            .id(doc.getId())
            .query((String) metadata.get("query"))
            .answer((String) metadata.get("answer"))
            .distance(distance == null ? -1f : (float) distance)
            .build();
    }
}
