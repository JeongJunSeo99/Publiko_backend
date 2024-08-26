package com.kobot.backend.controller;

import com.kobot.backend.service.CrawlingService;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/crawl")
public class CrawlingController {

    @Autowired
    private CrawlingService crawlingService;

    @GetMapping()
    public String crawl(@RequestParam String url) throws IOException {
        try {
            crawlingService.crawling(url);
            return "Crawling completed!";
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/auto")
    public String crawlAuto(@RequestParam String url) throws IOException {
        try {
            crawlingService.autoCrawling(url);
            return "Crawling completed!";
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
