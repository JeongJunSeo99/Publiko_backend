package com.kobot.backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    private List<String> disallowedPaths = new ArrayList<>();
    private static final int MAX_DEPTH = 5;

    // 사용자 input url parsing 과정이 중복되어 느려져서 해당 부분 분리
    public void startCrawling(String startUrl) throws IOException, URISyntaxException {

        // input된 url 인코딩 진행
        String encodedUrl = encodeUrl(startUrl);
        URI startUri = new URI(encodedUrl);
        String domain = startUri.getHost();

        // robots.txt 파일에서 Disallow 경로 가져오기
        loadRobotsTxt(startUri);

        crawlPage(encodedUrl, domain, 0);
    }

    public void crawlPage(String url, String domain, int depth) throws IOException, URISyntaxException {
        if (depth > MAX_DEPTH) {
            return;
        }

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
                URI linkUri = null;

                // url to uri 시 에러가 생기면 url 인코딩 후 uri로 변경
                try{
                    linkUri = new URI(absHref);
                } catch (URISyntaxException e) {
                    String encodedUrl = encodeUrl(absHref);
                    linkUri = new URI(encodedUrl);
                }

                // 동일한 도메인에 속하는 URL만 크롤링
                if (linkUri.getHost() != null && linkUri.getHost().equals(domain)) {
                    crawlPage(absHref, domain, depth+1);
                }

            }
        }
    }

    // robots.txt 파일 로드 및 Disallow 경로 파싱
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

    // 주어진 URL이 Disallow 목록에 있는지 확인
    private boolean isDisallowed(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String path = uri.getPath();

        for (String disallowedPath : disallowedPaths) {
            if (path.startsWith(disallowedPath))
                return true;
        }
        return false;
    }

    // url에 있는 공백, 한글 인코딩
    private String encodeUrl(String url) throws IOException {
        try {
            // 전체 URL을 안전하게 인코딩
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
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IOException("Failed to encode URL: " + url, e);
        }
    }

}