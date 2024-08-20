package com.kobot.backend.service;

import com.kobot.backend.CacheDto;
import com.kobot.backend.KobotBackendApplication;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = KobotBackendApplication.class)
@Slf4j
public class EsCacheServiceTest {

    @Autowired
    EsCacheService cacheService;

    @Test
    void testCache() {
        CacheDto result = cacheService.cache("한국의 수도가 어디야?", "서울");
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void testGetCached() {
        CacheDto cached = cacheService.getCached("한국의 수도는?");
        Assertions.assertThat(cached).isNotNull();
        log.info("{}", cached);

    }
}
