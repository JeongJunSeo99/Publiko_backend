package com.kobot.backend.service;

import com.kobot.backend.entity.HostUrl;
import com.kobot.backend.entity.SubUrls;
import com.kobot.backend.repository.HostUrlRepository;
import com.kobot.backend.repository.SubUrlsRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    private final HostUrlRepository hostUrlRepository;
    private final SubUrlsRepository subUrlsRepository;
    private final VectorStore vectorStore;

    private static final int MAX_DEPTH = 5;

    // 사용자 input url parsing 과정이 중복되어 느려져서 해당 부분 분리
    public void startCrawling(String startUrl) throws IOException, URISyntaxException {

        Set<String> visitLinks = new HashSet<>();
        List<String> crawlDatas = new ArrayList<>();

        // input된 url 인코딩 진행
        String encodedUrl = encodeUrl(startUrl);
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();

        // robots.txt 파일에서 Disallow 경로 가져오기
        loadRobotsTxt(startUri, disallowedPaths);

        crawlPage(encodedUrl, domain, 0, disallowedPaths, visitLinks, crawlDatas);

        // DB에 저장
        saveToDatabase(startUrl, visitLinks);

        List<org.springframework.ai.document.Document> documents = convertToDocuments(visitLinks);

        documents.forEach(x -> System.out.println(x.toString()));
        log.info("vectorStroe.add() 실행");
//        vectorStore.add(documents);
    }

    // 기존의 URL과 비교하여 새롭게 발견된 URL만 크롤링
    public void everydayCrawling(HostUrl hostUrl) throws IOException, URISyntaxException {

        Map<String, String> newCrawlSet = new HashMap<>();

        String encodedUrl = encodeUrl(hostUrl.getHostUrl());
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();
        loadRobotsTxt(startUri, disallowedPaths);

        crawlPage(encodedUrl, domain, 0, disallowedPaths, newCrawlSet);

        Set<String> existingSubUrls = new HashSet<>();

        for (SubUrls subUrl : subUrlsRepository.findByHostUrl(hostUrl))
            existingSubUrls.add(subUrl.getSubUrl());

        // 새롭게 발견된 URL들만 필터링
        Set<String> newVisitLinks = new HashSet<>(newCrawlSet.keySet());
        newVisitLinks.removeAll(existingSubUrls);

        if (!newVisitLinks.isEmpty()) {
            saveToDatabase(hostUrl.getHostUrl(), newVisitLinks);

            List<org.springframework.ai.document.Document> documents = convertToDocuments(newCrawlSet);

            documents.forEach(x -> System.out.println(x.toString()));
            log.info("vectorStroe.add() 실행");
//            vectorStore.add(documents);
        }
    }

    public List<String> startDownloadCrawling(String startUrl) throws IOException, URISyntaxException {

        Set<String> visitLinks = new HashSet<>();
        List<String> crawlDatas = new ArrayList<>();

        // input된 url 인코딩 진행
        String encodedUrl = encodeUrl(startUrl);
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();

        // robots.txt 파일에서 Disallow 경로 가져오기
        loadRobotsTxt(startUri, disallowedPaths);

        crawlPage(encodedUrl, domain, 0, disallowedPaths, visitLinks, crawlDatas);

        // DB에 저장
        saveToDatabase(startUrl, visitLinks);

        return crawlDatas;
    }

    public void crawlPage(String url, String domain, int depth, List<String> disallowedPaths
        , Set<String> visitLinks, List<String> crawlDatas) throws IOException, URISyntaxException {

        if (depth > MAX_DEPTH) {
            return;
        }

        if (!visitLinks.contains(url)) {
            visitLinks.add(url);

            if (isDisallowed(url, disallowedPaths)) {
                log.warn("URL is disallowed by robots.txt: {}", url);
                return;
            }

            // 크롤링 시, 웹 콘텐츠가 삭제된 url에 대해 try-catch 구문을 작성해 크롤링 종료 안되게 처리
            try {
                // 크롤링 할 url의 contentType 무시하는 설정
                Connection connection = Jsoup.connect(url).ignoreContentType(true);
                Document document = connection.get();

                String text = document.body().text();
                crawlDatas.add(text);

                Elements links = document.select("a[href]");

                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    URI linkUri = null;

                    try {
                        linkUri = new URI(absHref);
                    } catch (URISyntaxException e) {
                        String encodedUrl = encodeUrl(absHref);
                        linkUri = new URI(encodedUrl);
                    }

                    if (linkUri.getHost() != null && linkUri.getHost().equals(domain))
                        crawlPage(absHref, domain, depth + 1, disallowedPaths, visitLinks, crawlDatas);

                }
            } catch (IOException e) {
                log.error("Failed to crawl the website: URL={}, Error Message={}", url
                    , e.getMessage());
            }
        }
    }

    public void crawlPage(String url, String domain, int depth, List<String> disallowedPaths
        , Map<String, String> newCrawlDatas) throws IOException, URISyntaxException {

        if (depth > MAX_DEPTH) {
            return;
        }

        if (!newCrawlDatas.containsKey(url)) {

            if (isDisallowed(url, disallowedPaths)) {
                log.warn("URL is disallowed by robots.txt: {}", url);
                return;
            }

            // 크롤링 시, 웹 콘텐츠가 삭제된 url에 대해 try-catch 구문을 작성해 크롤링 종료 안되게 처리
            try {
                // 크롤링 할 url의 contentType 무시하는 설정
                Connection connection = Jsoup.connect(url).ignoreContentType(true);
                Document document = connection.get();

                String text = document.body().text();
                newCrawlDatas.put(url, text);

                Elements links = document.select("a[href]");

                for (Element link : links) {
                    String absHref = link.attr("abs:href");
                    URI linkUri = null;

                    try {
                        linkUri = new URI(absHref);
                    } catch (URISyntaxException e) {
                        String encodedUrl = encodeUrl(absHref);
                        linkUri = new URI(encodedUrl);
                    }

                    if (linkUri.getHost() != null && linkUri.getHost().equals(domain))
                        crawlPage(absHref, domain, depth + 1, disallowedPaths, newCrawlDatas);

                }
            } catch (IOException e) {
                log.error("Failed to crawl the website: URL={}, Error Message={}", url
                    , e.getMessage());
            }
        }
    }

    // robots.txt 파일 로드 및 Disallow 경로 파싱
    private void loadRobotsTxt(URI startUri, List<String> disallowedPaths) throws IOException {
        String robotsUrl = startUri.getScheme() + "://" + startUri.getHost() + "/robots.txt";
        String line;

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
            new URI(robotsUrl).toURL().openStream()))) {

            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("Disallow:")) {
                    String disallowPath = line.split(":", 2)[1].trim();
                    disallowedPaths.add(disallowPath);
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.warn("Failed to load robots.txt, assuming no restrictions.");
        }
    }

    // 주어진 URL이 Disallow 목록에 있는지 확인
    private boolean isDisallowed(String url, List<String> disallowedPaths) throws URISyntaxException {
        URI uri = new URI(url);
        String path = uri.getPath();

        for (String disallowedPath : disallowedPaths) {
            if (path.startsWith(disallowedPath))
                return true;
        }

        return false;
    }

    // url에 있는 공백, 한글 인코딩
    private String encodeUrl(String url) throws URISyntaxException, IOException {

        URL urlObj = new URL(url);
        URI uri = new URI(
            urlObj.getProtocol(),
            urlObj.getUserInfo(),
            urlObj.getHost(),
            urlObj.getPort(),
            urlObj.getPath(),
            urlObj.getQuery(),
            urlObj.getRef()
        );

        return uri.toASCIIString();  // 인코딩된 URL 반환
    }

    private void saveToDatabase(String startUrl, Set<String> subLinks) {

        HostUrl hostUrl = hostUrlRepository.findByHostUrl(startUrl);

        if (hostUrl == null) {
            hostUrl = new HostUrl();
            hostUrl.setHostUrl(startUrl);
            hostUrl = hostUrlRepository.save(hostUrl);
        }

        // SubUrls 엔티티 저장
        for (String link : subLinks) {
            if (!link.equals(startUrl)) {
                SubUrls existingSubUrl = subUrlsRepository.findBySubUrl(link);

                if (existingSubUrl == null) {
                    SubUrls subUrl = new SubUrls();
                    subUrl.setSubUrl(link);
                    subUrl.setHostUrl(hostUrl);
                    subUrlsRepository.save(subUrl);
                }
            }
        }
    }

    public List<org.springframework.ai.document.Document> convertToDocuments(Set<String> visitLinks)
    {
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();

        for (String url : visitLinks) {
            org.springframework.ai.document.Document document =
                new org.springframework.ai.document.Document(url);
            documents.add(document);
        }

        return documents;
    }

    private List<org.springframework.ai.document.Document> convertToDocuments(
        Map<String, String> crawlData) {
        List<org.springframework.ai.document.Document> documents = new ArrayList<>();

        for (Map.Entry<String, String> entry : crawlData.entrySet()) {
            org.springframework.ai.document.Document document =
                new org.springframework.ai.document.Document(entry.getValue());
            documents.add(document);
        }

        return documents;
    }

    // 매일 크롤링 수행
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void scheduledCrawling() throws IOException, URISyntaxException {
        List<HostUrl> hostUrls = hostUrlRepository.findAll();

        for (HostUrl hostUrl : hostUrls)
            everydayCrawling(hostUrl);

    }
    
    // 사용자가 원할 때, 새롭게 추가된 웹페이지 내용 크롤링 하는 기능
    public void newContentCrawling() throws IOException, URISyntaxException {
        List<HostUrl> hostUrls = hostUrlRepository.findAll();

        for (HostUrl hostUrl : hostUrls)
            everydayCrawling(hostUrl);

    }
}