//package com.kobot.backend.config;
//
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.RedisVectorStore;
//import org.springframework.ai.vectorstore.RedisVectorStore.MetadataField;
//import org.springframework.ai.vectorstore.RedisVectorStore.RedisVectorStoreConfig;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import redis.clients.jedis.JedisPooled;
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
//        return template;
//    }
//
//    @Bean
//    public RedisVectorStore redisVectorStore(EmbeddingModel embeddingModel) {
//        RedisVectorStoreConfig config = RedisVectorStoreConfig.builder()
//            .withPrefix("doc:")
//            .withIndexName("spring-ai-index")
//            .withMetadataFields(
//                MetadataField.tag("answer"),
//                MetadataField.numeric("timestamp"))
//            .build();
//
//        JedisPooled jedis = new JedisPooled("localhost", 6379);
//
//        return new RedisVectorStore(config, embeddingModel, jedis,
//            true);
//    }
//}

