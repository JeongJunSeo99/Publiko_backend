package com.kobot.backend.controller;

import com.kobot.backend.service.PromptService;
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

    @PostMapping("/chat")
    public String getQueryResult(@RequestBody String query) {
        log.info(query);
        return promptService.getChatResponse(query);
    }
}
