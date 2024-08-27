package com.kobot.backend.controller;

import com.kobot.backend.CacheDto;
import com.kobot.backend.service.PromptService;
import com.kobot.backend.service.RedisCacheService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final RedisCacheService redisCacheService;

    @PostMapping("/chat")
    public String getQueryResult(@RequestBody String query) {
        log.info(query);
        return promptService.getChatResponse(query);
    }

    @PostMapping("/chat-cache")
    public String getCacheQueryResult(@RequestBody String query) {
        CacheDto cacheDto = Optional.ofNullable(redisCacheService.getCached(query))
            .orElseGet(() -> redisCacheService.cache(query, promptService.getChatResponse(query)));

        return cacheDto.getAnswer();
    }
}
