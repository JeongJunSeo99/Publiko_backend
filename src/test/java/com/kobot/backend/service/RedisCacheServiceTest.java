package com.kobot.backend.service;

import com.kobot.backend.CacheDto;
import com.kobot.backend.KobotBackendApplication;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = KobotBackendApplication.class)
class RedisCacheServiceTest {

    @Autowired
    RedisCacheService redisCacheService;

    @Test
    void testCache() {
        CacheDto result = redisCacheService.cache("한국의 수도가 어디야?", "서울");
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void testGetCached() {
        CacheDto cached = redisCacheService.getCached("한국의 수도는?");
        Assertions.assertThat(cached).isNotNull();
        log.info("{}", cached);

    }

}