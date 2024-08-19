package com.kobot.backend.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Service
public class CrawlingService {

    private Set<String> visitedLinks = new HashSet<>();
    private String domain;

    public void crawl(String url) throws IOException, URISyntaxException {
        URI startUri = new URI(url);
        domain = startUri.getHost();

        if (!visitedLinks.contains(url)) {
            visitedLinks.add(url);
            
            // 크롤링 할 url의 contentType 무시하는 설정
            Connection connection = Jsoup.connect(url).ignoreContentType(true);

            Document document = connection.get();

            // 텍스트 데이터 추출
            String text = document.body().text();
            System.out.println("URL : " + url + " : " + text);

            // 연결된 하이퍼링크 추출 후 재귀적으로 크롤링
            Elements links = document.select("a[href]");

            for (Element link : links) {
                String absHref = link.attr("abs:href");

                // 동일한 도메인에 속하는 URL만 크롤링
                URI linkUri = new URI(absHref);
                if (linkUri.getHost().equals(domain)) {
                    crawl(absHref);
                }
            }
        }
    }
}