package com.kobot.backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
    private List<String> disallowedPaths = new ArrayList<>();

    public void crawl(String url) throws IOException, URISyntaxException {
        URI startUri = new URI(url);
        domain = startUri.getHost();
        loadRobotsTxt(startUri);

        if (!visitedLinks.contains(url)) {
            visitedLinks.add(url);

            // Disallow된 경로인지 확인
            if (isDisallowed(url))
                return;
            
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

    /* robots.txt 파일 로드 및 Disallow 경로 파싱 */
    private void loadRobotsTxt(URI startUri) throws IOException {
        String robotsUrl = startUri.getScheme() + "://" + startUri.getHost() + "/robots.txt";
        String line;

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URI(robotsUrl).toURL().openStream()))) {
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Disallow:")) {
                    String disallowPath = line.split(":", 2)[1].trim();
                    disallowedPaths.add(disallowPath);
                }
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Failed to load robots.txt, assuming no restrictions.");
        }
    }

    /* 주어진 URL이 Disallow 목록에 있는지 확인 */
    private boolean isDisallowed(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String path = uri.getPath();

        for (String disallowedPath : disallowedPaths) {
            if (path.startsWith(disallowedPath))
                return true;
        }
        return false;
    }

}