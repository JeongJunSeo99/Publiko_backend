package com.kobot.backend.controller;

import com.kobot.backend.service.CrawlingService;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class CrawlingController {

    @Autowired
    private CrawlingService crawlingService;

    @GetMapping("/crawl")
    public String crawl(@RequestParam String url) {
        try {
            crawlingService.startCrawling(url);
            return "Crawling completed!";
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
