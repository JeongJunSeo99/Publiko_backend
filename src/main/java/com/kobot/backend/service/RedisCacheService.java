//package com.kobot.backend.service;
//
//import com.kobot.backend.CacheDto;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.RedisVectorStore;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RedisCacheService {
//
//    private final EmbeddingModel embeddingModel;
//    private final float threshold = 0.8f;
//    private final int topK = 1;
//    private final RedisVectorStore vectorStore;
//
//    /**
//     * 질의와 답변을 캐시합니다.
//     *
//     * @param query  질의
//     * @param answer 답변
//     * @return 캐시된 객체. 캐시 실패 시, null
//     */
//    public CacheDto cache(String query, String answer) {
//        // metadata set
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("answer", answer);
//        metadata.put("timestamp", System.currentTimeMillis());
//
//        // document set
//        Document document = new Document(query, metadata);
//
//        // embedding
//        document.setEmbedding(embeddingModel.embed(document));
//
//        List <Document> documents = List.of(document);
//        vectorStore.add(documents);
//
//        return convertToCachedDto(document);
//    }
//
//    /**
//     * 질의와 유사한 질답을 캐시에서 꺼냅니다.
//     *
//     * @param query 질의
//     * @return 질답. cache miss 시, null
//     */
//    public CacheDto getCached(String query) {
//        List<Document> documents = vectorStore.similaritySearch(
//            SearchRequest
//                .query(query)
//                .withTopK(topK)
//                .withSimilarityThreshold(threshold)
//        );
//
//        if (documents.isEmpty()){
//            return null;
//        }
//
//        return convertToCachedDto(documents.getFirst());
//    }
//
//    private CacheDto convertToCachedDto(Document doc) {
//        Map<String, Object> metadata = doc.getMetadata();
//        Object distance = metadata.get("distance");
//
//        return CacheDto.builder()
//            .id(doc.getId())
//            .query((String) metadata.get("query"))
//            .answer((String) metadata.get("answer"))
//            .distance(distance == null ? -1f : (float) distance)
//            .build();
//    }
//
//}
