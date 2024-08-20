package com.kobot.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.kobot.backend.CacheDto;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EsCacheService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient client;
    private final String indexName = "kobot-user-query-index";
    private final float threshold = 0.8f;
    private final long topK = 1L;

    public CacheDto cache(String query, String answer) {
        // metadata set
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("answer", answer);
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
