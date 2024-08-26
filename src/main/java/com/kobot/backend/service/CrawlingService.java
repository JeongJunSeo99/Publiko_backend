package com.kobot.backend.service;

import com.kobot.backend.entity.HostUrl;
import com.kobot.backend.entity.SubUrls;
import com.kobot.backend.repository.HostUrlRepository;
import com.kobot.backend.repository.SubUrlsRepository;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static final int MAX_DEPTH = 1;

    // 사용자 input url parsing 과정이 중복되어 느려져서 해당 부분 분리
    public void crawling(String startUrl) throws IOException, URISyntaxException {

        Set<String> visitLinks = new HashSet<>();

        // input된 url 인코딩 진행
        String encodedUrl = encodeUrl(startUrl);
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();

        // robots.txt 파일에서 Disallow 경로 가져오기
        loadRobotsTxt(startUri, disallowedPaths);

        // 현재 시각을 기준으로 폴더 생성
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path folderPath = Paths.get("crawl_results", timestamp);
        Files.createDirectories(folderPath);

        // 크롤링 시작
        crawlPageToFile(encodedUrl, domain, 0, disallowedPaths, visitLinks, folderPath);
    }

    public void autoCrawling(String startUrl) throws IOException, URISyntaxException {

        Set<String> visitLinks = new HashSet<>();

        // input된 url 인코딩 진행
        String encodedUrl = encodeUrl(startUrl);
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();

        // robots.txt 파일에서 Disallow 경로 가져오기
        loadRobotsTxt(startUri, disallowedPaths);

        // 현재 시각을 기준으로 폴더 생성
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path folderPath = Paths.get("crawl_results", timestamp);
        Files.createDirectories(folderPath);

        // 크롤링 시작
        crawlPageToFile(encodedUrl, domain, 0, disallowedPaths, visitLinks, folderPath);

        // DB에 저장
        saveToDatabase(startUrl, visitLinks);
    }

    // 기존의 URL과 비교하여 새롭게 발견된 URL만 크롤링
    public void everydayCrawling(HostUrl hostUrl) throws IOException, URISyntaxException {

        Map<String, String> newCrawlSet = new HashMap<>();

        String encodedUrl = encodeUrl(hostUrl.getHostUrl());
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        List<String> disallowedPaths = new ArrayList<>();
        loadRobotsTxt(startUri, disallowedPaths);

        getCrawlUrl(encodedUrl, domain, 0, disallowedPaths, newCrawlSet);

        Set<String> existingSubUrls = new HashSet<>();

        for (SubUrls subUrl : subUrlsRepository.findByHostUrl(hostUrl))
            existingSubUrls.add(subUrl.getSubUrl());

        // 새롭게 발견된 URL들만 필터링
        Set<String> newVisitLinks = new HashSet<>(newCrawlSet.keySet());
        newVisitLinks.removeAll(existingSubUrls);

        if (!newVisitLinks.isEmpty()){
            // 현재 시각을 기준으로 폴더 생성
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path folderPath = Paths.get("crawl_results", hostUrl.getHostUrl() + timestamp);
            Files.createDirectories(folderPath);

            saveToDatabase(hostUrl.getHostUrl(), newVisitLinks);

            for (String newLink : newVisitLinks) {
                crawlPageToFileOneDepth(newLink, domain, disallowedPaths, folderPath);
            }
        }
    }

    public void crawlPageToFile(String url, String domain, int depth, List<String> disallowedPaths
        , Set<String> visitLinks, Path folderPath) throws IOException, URISyntaxException {

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

                // HTML의 <title> 태그에서 제목 추출
                String title = document.title();
                if (title == null || title.isEmpty()) {
                    title = "no_title";  // 제목이 없을 경우 기본 이름 지정
                }

                String text = document.body().text();

                // 제목을 파일명으로 변환 (파일 시스템에 허용되지 않는 문자는 제거하지 않음)
                String safeFileName = title.replaceAll("[<>:\"/\\|?*]", "_").trim();
                if (safeFileName.length() > 255) {
                    safeFileName = safeFileName.substring(0, 255);  // 파일 이름 길이 제한
                }

                // 파일 이름의 중복을 방지
                Path filePath = getUniqueFilePath(folderPath, safeFileName);

                // 텍스트를 파일로 저장. 만약 파일 이름에 문제가 생기면 log만 찍고 재진행
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                    writer.write(text);
                } catch (IOException e) {
                    log.error("Failed to write to file: URL={}, Error Message={}", url, e.getMessage());
                }

                Elements links = document.select("a[href]");

                for (Element link : links) {
                    String absHref = link.attr("abs:href");

                    // URL이 상대 경로인 경우 base URL을 사용하여 절대 URL로 변환
                    try {
                        URI baseUri = new URI(url);
                        URI linkUri = new URI(absHref);

                        // 상대 경로가 포함된 경우 절대 경로로 변환
                        URI resolvedUri = baseUri.resolve(linkUri);
                        String resolvedUrl = resolvedUri.toString();

                        // 링크의 호스트가 도메인과 일치하는지 확인
                        if (resolvedUri.getHost() != null && resolvedUri.getHost().equals(domain)) {
                            crawlPageToFile(resolvedUrl, domain, depth + 1, disallowedPaths, visitLinks, folderPath);
                        }
                    } catch (URISyntaxException e) {
                        System.err.println("Invalid URL format: " + absHref);
                    }

                }
            } catch (IOException e) {
                log.error("Failed to crawl the website: URL={}, Error Message={}", url
                    , e.getMessage());
            }
        }
    }

    public void crawlPageToFileOneDepth(String url, String domain, List<String> disallowedPaths
        , Path folderPath) throws URISyntaxException {

        if (isDisallowed(url, disallowedPaths)) {
            log.warn("URL is disallowed by robots.txt: {}", url);
            return;
        }

        // 크롤링 시, 웹 콘텐츠가 삭제된 url에 대해 try-catch 구문을 작성해 크롤링 종료 안되게 처리
        try {

            // 크롤링 할 url의 contentType 무시하는 설정
            Connection connection = Jsoup.connect(url).ignoreContentType(true);
            Document document = connection.get();

            // HTML의 <title> 태그에서 제목 추출
            String title = document.title();
            if (title == null || title.isEmpty()) {
                title = "no_title";  // 제목이 없을 경우 기본 이름 지정
            }

            String text = document.body().text();

            // 제목을 파일명으로 변환 (파일 시스템에 허용되지 않는 문자는 제거하지 않음)
            String safeFileName = title.replaceAll("[<>:\"/\\|?*]", "_").trim();
            if (safeFileName.length() > 255) {
                safeFileName = safeFileName.substring(0, 255);  // 파일 이름 길이 제한
            }

            // 파일 이름의 중복을 방지
            Path filePath = getUniqueFilePath(folderPath, safeFileName);

            // 텍스트를 파일로 저장. 만약 파일 이름에 문제가 생기면 log만 찍고 재진행
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
                writer.write(text);
            } catch (IOException e) {
                log.error("Failed to write to file: URL={}, Error Message={}", url, e.getMessage());
            }

        } catch (IOException e) {
            log.error("Failed to crawl the website: URL={}, Error Message={}", url
                , e.getMessage());
        }
    }

    public void getCrawlUrl(String url, String domain, int depth, List<String> disallowedPaths
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

                Elements links = document.select("a[href]");

                for (Element link : links) {

                    String absHref = link.attr("abs:href");

                    // URL이 상대 경로인 경우 base URL을 사용하여 절대 URL로 변환
                    try {
                        URI baseUri = new URI(url);
                        URI linkUri = new URI(absHref);

                        // 상대 경로가 포함된 경우 절대 경로로 변환
                        URI resolvedUri = baseUri.resolve(linkUri);
                        String resolvedUrl = resolvedUri.toString();

                        // 링크의 호스트가 도메인과 일치하는지 확인
                        if (resolvedUri.getHost() != null && resolvedUri.getHost().equals(domain)) {
                            getCrawlUrl(absHref, domain, depth + 1, disallowedPaths, newCrawlDatas);
                        }

                    } catch (URISyntaxException e) {
                        System.err.println("Invalid URL format: " + absHref);
                    }
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

    // 매일 크롤링 수행
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void scheduledCrawling() throws IOException, URISyntaxException {
        List<HostUrl> hostUrls = hostUrlRepository.findAll();

        for (HostUrl hostUrl : hostUrls)
            everydayCrawling(hostUrl);
    }

    private Path getUniqueFilePath(Path folderPath, String fileName) throws IOException {
        Path filePath = folderPath.resolve(fileName + ".txt");
        int counter = 1;

        // 파일 이름 중복 체크 및 고유한 파일 이름 생성
        while (Files.exists(filePath)) {
            String newFileName = fileName + "_" + counter;
            filePath = folderPath.resolve(newFileName + ".txt");
            counter++;
        }

        return filePath;
    }
}