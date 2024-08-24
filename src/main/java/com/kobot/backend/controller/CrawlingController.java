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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class CrawlingController {

    @Autowired
    private CrawlingService crawlingService;

    @GetMapping("/crawl/auto")
    public String crawl(@RequestParam String url) throws IOException {
        try {
            crawlingService.startCrawling(url);
            return "Crawling completed!";
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/crawl/download")
    public ResponseEntity<InputStreamResource> crawlAndDownload(@RequestParam String url) {
        try {
            List<String> subLinks = crawlingService.startDownloadCrawling(url);

            StringBuilder sb = new StringBuilder();

            for (String data : subLinks)
                sb.append("Content:\n").append(data).append("\n\n");

            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(bytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subLinks.txt");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));

            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.OK);
        } catch (IOException | URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
