package com.project.moduleservicedatacollection.service.TechBlog;

import com.project.moduleservicedatacollection.dto.TechBlog.ArticleDetails;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.tomcat.util.http.FastHttpDateFormat.parseDate;

@Service
public class JsoupService implements TechBlogService {
    public List<String> getSupportedBlogTypes() {
        return List.of(
                "gmarket",
                "hancom",
                "hyperconnect",
                "kakaobank",
                "kakopay",
                "oliveyoung",
                "saramin",
                "socar",
                "spoqa",
                "devocean",
                "danawa",
                "devsisters",
                "woowahan",
                "eleven"
        );
    }

    @Override
    public List<ArticleDetails> fetchArticles(String blogType) {
        if (blogType.equalsIgnoreCase("gmarket")) {
            return fetchGmarketArticles();
        } else if (blogType.equalsIgnoreCase("hancom")) {
            return fetchHancomArticles();
        } else if (blogType.equalsIgnoreCase("hyperconnect")) {
            return fetchHyperconnectArticles();
        } else if (blogType.equalsIgnoreCase("kakaobank")) {
            return fetchKakaobankArticles();
        } else if (blogType.equalsIgnoreCase("kakopay")) {
            return fetchKakopayArticles();
        } else if (blogType.equalsIgnoreCase("oliveyoung")) {
            return fetchOliveyoungArticles();
        } else if (blogType.equalsIgnoreCase("socar")) {
            return fetchSocarArticles();
        } else if (blogType.equalsIgnoreCase("spoqa")) {
            return fetchSpoqaArticles();
        } else if (blogType.equalsIgnoreCase("devocean")) {
            return fetchDevoceanArticles();
        } else if (blogType.equalsIgnoreCase("danawa")) {
            return fetchDanawaArticles();
        } else if (blogType.equalsIgnoreCase("devsisters")) {
            return fetchDevsistersArticles();
        } else if (blogType.equalsIgnoreCase("woowahan")) {
            return fetchWoowahanArticles();
        }else if (blogType.equalsIgnoreCase("eleven")) {
            return fetchElevenArticles();
        } else {
            throw new IllegalArgumentException("Invalid blog type: " + blogType);
        }
    }

    private List<ArticleDetails> fetchSpoqaArticles() {
        String baseUrl = "https://spoqa.github.io";
        List<ArticleDetails> articles = new ArrayList<>();
        int currentPage = 1;
        int maxPages = getSpoqaMaxPages(); // 🔹 최대 페이지 동적으로 가져오기

        try {
            while (currentPage <= maxPages) {
                String currentUrl = (currentPage == 1) ? baseUrl : baseUrl + "/page" + currentPage + "/";
                System.out.println("📌 크롤링 중: " + currentUrl);

                // ✅ Jsoup을 이용해 HTML 가져오기
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .ignoreHttpErrors(true) // 🔹 404 오류 무시
                        .get();

                if (doc.location().contains("404") || doc.body().text().contains("404")) {
                    System.out.println("🚫 페이지를 찾을 수 없습니다: " + currentUrl);
                    break;
                }

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("li.post-item");

                if (postElements.isEmpty()) {
                    System.out.println("🚀 모든 페이지 크롤링 완료!");
                    break;
                }

                for (Element post : postElements) {
                    // ✅ 제목 추출
                    Element titleElement = post.selectFirst("span.post-title-words");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                    // ✅ URL 추출 (절대 URL 변환)
                    Element linkElement = post.selectFirst("a[href]");
                    String link = linkElement != null ? baseUrl + linkElement.attr("href").replace("./", "/") : "링크 없음";

                    // ✅ 요약 추출
                    Element summaryElement = post.selectFirst("p.post-description");
                    String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                    // ✅ 게시일 추출
                    Element dateElement = post.selectFirst("span.post-date");
                    LocalDate publishDate = parseSpoqaDate(dateElement != null ? dateElement.text().trim() : "");

                    // ✅ 게시글 저장 (작성자 제거됨)
                    articles.add(new ArticleDetails("Spoqa Blog", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }

                currentPage++; // 다음 페이지로 이동
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private LocalDate parseSpoqaDate(String dateText) {
        if (dateText.isEmpty() || dateText.equals("날짜 없음")) {
            return LocalDate.now();
        }

        // ✅ 공백 제거 및 날짜 형식 정리
        dateText = dateText.replaceAll("\\s+", "").trim();

        // ✅ 날짜 형식 리스트 (한글 형식 포함)
        String[] datePatterns = {
                "yyyy년MM월dd일",   // ✅ 2017년05월22일
                "yyyy.MM.dd.",     // ✅ 2017.05.22.
                "yyyy.MM.dd",      // ✅ 2017.05.22
                "yyyy/MM/dd"       // ✅ 2017/05/22
        };

        // ✅ 여러 날짜 형식 시도
        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.KOREAN);
                return LocalDate.parse(dateText, formatter);
            } catch (Exception ignored) {}
        }

        System.err.println("⚠ 날짜 변환 실패: " + dateText);
        return LocalDate.now(); // 변환 실패 시 현재 날짜 반환
    }

    private int getSpoqaMaxPages() {
        String baseUrl = "https://spoqa.github.io";

        try {
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            // ✅ 페이지네이션에서 마지막 페이지 숫자 가져오기
            Elements pageElements = doc.select("div#post-pagination a[href^='/page']");
            int maxPage = 1;

            for (Element pageElement : pageElements) {
                String pageHref = pageElement.attr("href"); // 예: "/page14/"
                int pageNum = Integer.parseInt(pageHref.replaceAll("[^0-9]", "")); // 숫자만 추출
                maxPage = Math.max(maxPage, pageNum);
            }

            System.out.println("📌 최대 페이지 확인: " + maxPage);
            return maxPage;
        } catch (IOException e) {
            System.err.println("❌ 페이지네이션 정보 가져오기 실패: " + e.getMessage());
        }

        return 1; // 기본적으로 1페이지만 반환 (오류 발생 시)
    }

    private List<ArticleDetails> fetchSocarArticles() {
        String baseUrl = "https://tech.socarcorp.kr";

        List<ArticleDetails> articles = new ArrayList<>();
        Set<String> collectedUrls = new HashSet<>(); // 중복 방지
        String pageUrl = baseUrl + "/posts"; // 첫 번째 페이지

        try {
            while (pageUrl != null) { // "Older" 버튼이 존재하는 한 계속 크롤링
                System.out.println("📌 크롤링 중: " + pageUrl);

                // Jsoup으로 HTML 가져오기
                Document doc = Jsoup.connect(pageUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(5000)
                        .get();

                // 게시글 목록 선택
                Elements postElements = doc.select("article.post-preview");
                for (Element post : postElements) {
                    Element linkElement = post.selectFirst("a[href]");
                    String title = post.selectFirst("h2.post-title") != null ? post.selectFirst("h2.post-title").text().trim() : "제목 없음";
                    String link = linkElement != null ? baseUrl + linkElement.attr("href") : "URL 없음";
                    String content = post.selectFirst("h3.post-subtitle") != null ? post.selectFirst("h3.post-subtitle").text().trim() : "요약 없음";
                    String publishDateText = post.selectFirst("span.date") != null ? post.selectFirst("span.date").text().trim() : "날짜 없음";

                    // 날짜 요소 디버깅 출력 추가
                    if (post.selectFirst("span.date") != null) {
                        System.out.println("📅 날짜 요소 발견: " + post.selectFirst("span.date").outerHtml());
                    } else {
                        System.out.println("⚠ 날짜 요소가 없습니다. 기본값(날짜 없음) 사용");
                    }

                    System.out.println("📅 확인된 날짜: " + publishDateText);

                    // 중복 URL 체크
                    if (collectedUrls.contains(link)) {
                        System.out.println("⚠ 중복된 게시글 건너뜀: " + title);
                        continue;
                    }
                    collectedUrls.add(link);

                    // ✅ 날짜 변환 시도
                    LocalDate publishDate = parseSocarDate(publishDateText);

                    // ✅ 변환 실패 시 null 저장 (오늘 날짜 강제 저장 안 함)
                    if (publishDate == null) {
                        System.err.println("❌ 날짜 변환 실패! null 저장됨. publishDateText: " + publishDateText);
                    }

                    // 게시글 저장
                    articles.add(new ArticleDetails("Socar", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 저장됨: " + title + " (게시일: " + publishDate + ")");
                }

                // ✅ "Older" 버튼(다음 페이지 버튼) 찾기
                Element nextPageElement = doc.selectFirst("a.btn.btn-primary.float-right[href]");
                if (nextPageElement != null) {
                    pageUrl = nextPageElement.absUrl("href"); // 절대 URL 변환
                    System.out.println("📌 다음 페이지 이동: " + pageUrl);
                } else {
                    System.out.println("🚫 더 이상 페이지 없음. 크롤링 종료.");
                    pageUrl = null;
                }
            }
        } catch (IOException e) {
            System.err.println("🚨 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private LocalDate parseSocarDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.equals("날짜 없음")) {
            System.err.println("⚠ 날짜 없음. 변환 불가.");
            return null; // 변환 실패 시 null 반환
        }

        // ✅ 여러 날짜 형식 지원
        String[] datePatterns = {
                "yyyy-MM-dd",    // 2025-02-25
                "yyyy.MM.dd",    // 2025.02.25
                "yyyy/MM/dd"     // 2025/02/25
        };

        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsedDate = LocalDate.parse(dateText, formatter);
                System.out.println("✅ 날짜 변환 성공: " + dateText + " → " + parsedDate);
                return parsedDate;
            } catch (Exception ignored) {}
        }

        // ✅ 모든 패턴에서 변환 실패 시 오류 출력
        System.err.println("❌ 모든 형식에서 날짜 변환 실패: " + dateText);
        return null; // 변환 실패 시 null 반환
    }


    private LocalDate parseSaraminDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.equals("날짜 없음")) {
            System.err.println("⚠ 날짜 없음. 변환 불가.");
            return null; // 변환 실패 시 null 반환
        }

        // ✅ 여러 날짜 형식 지원
        String[] datePatterns = {
                "MMMM d, yyyy",  // June 26, 2024
                "yyyy.MM.dd.",   // 2024.06.26.
                "yyyy.MM.dd",    // 2024.06.26
                "yyyy-MM-dd",    // 2024-06-26
                "yyyy/MM/dd"     // 2024/06/26
        };

        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                LocalDate parsedDate = LocalDate.parse(dateText, formatter);
                System.out.println("✅ 날짜 변환 성공: " + dateText + " → " + parsedDate);
                return parsedDate;
            } catch (Exception ignored) {}
        }

        // ✅ 모든 패턴에서 변환 실패 시 오류 출력
        System.err.println("❌ 모든 형식에서 날짜 변환 실패: " + dateText);
        return null; // 변환 실패 시 null 반환
    }

    private List<ArticleDetails> fetchOliveyoungArticles() {
        String baseUrl = "https://oliveyoung.tech/";
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<ArticleDetails> articles = new ArrayList<>();
        Set<String> visitedPages = new HashSet<>(); // 중복된 페이지 방문 방지
        String currentUrl = baseUrl; // 첫 번째 페이지에서 시작

        try {
            while (currentUrl != null) {
                if (visitedPages.contains(currentUrl)) {
                    System.out.println("⚠ 중복 페이지 건너뛰기: " + currentUrl);
                    break;
                }
                visitedPages.add(currentUrl);

                // ✅ 페이지 요청 및 파싱
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(5000)
                        .get();

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("ul[class*=PostList-module--container] > li");

                for (Element post : postElements) {
                    // ✅ 제목 크롤링
                    Element titleElement = post.selectFirst("h1, h2, h3");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                    // ✅ URL 크롤링
                    Element linkElement = post.selectFirst("a[href]");
                    String relativeUrl = linkElement != null ? linkElement.attr("href").replaceAll("/$", "") : "#";
                    String link = relativeUrl.startsWith("http") ? relativeUrl : baseUrl.replaceAll("/$", "") + "/" + relativeUrl.replaceAll("^/", "");

                    // ✅ 요약 크롤링
                    Element summaryElement = post.selectFirst("p[class^=PostList-module--sub], p:not([class])");
                    String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                    // ✅ 날짜 크롤링
                    Element dateElement = post.selectFirst("p[class*=PostList-module--date], time");
                    String publishDateText = dateElement != null ? dateElement.text().trim() : "날짜 없음";
                    LocalDate publishDate = LocalDate.now(); // 기본값

                    // ✅ 날짜 변환 시도
                    if (!publishDateText.equals("날짜 없음")) {
                        try {
                            publishDate = LocalDate.parse(publishDateText, DATE_FORMATTER);
                        } catch (Exception e) {
                            System.err.println("⚠ 날짜 변환 실패: " + publishDateText);
                        }
                    }

                    // ✅ 게시글 저장
                    articles.add(new ArticleDetails("OliveYoung", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDateText + ")");
                }

                // ✅ 다음 페이지 버튼 찾기
                Element nextPageElement = doc.selectFirst("a[class*=Pagination-module--page-btn][href]:contains(Next)");

                if (nextPageElement != null) {
                    currentUrl = nextPageElement.absUrl("href");
                    System.out.println("📌 다음 페이지 이동: " + currentUrl);
                } else {
                    System.out.println("🚫 더 이상 페이지 없음. 크롤링 종료.");
                    currentUrl = null;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private LocalDate parsePublishDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.equals("날짜 없음")) {
            return LocalDate.now(); // 기본값 (오늘 날짜)
        }

        // ✅ 마지막 마침표(.) 제거 & 앞뒤 공백 제거
        dateText = dateText.replaceAll("\\.$", "").trim();

        // ✅ 변환 가능한 날짜 형식 목록
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 2023-08-30
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),  // 2023.08.30
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),  // 2023/08/30
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") // 2023년 08월 30일
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateText, formatter);
            } catch (Exception ignored) {
            }
        }

        System.err.println("⚠ 날짜 변환 실패 (미지원 형식): " + dateText);
        return LocalDate.now(); // 변환 실패 시 기본값 (오늘 날짜) 반환
    }

    private List<ArticleDetails> fetchKakopayArticles() {
        String baseUrl = "https://tech.kakaopay.com";

        List<ArticleDetails> articles = new ArrayList<>();
        int currentPage = 1;
        int maxPages = getKakopayMaxPages(); // 🔹 최대 페이지 동적으로 가져오기

        try {
            while (currentPage <= maxPages) {
                String currentUrl = baseUrl + "/page/" + currentPage;
                System.out.println("📌 크롤링 중: " + currentUrl);

                // ✅ Jsoup을 이용해 HTML 가져오기 (재시도 로직 추가)
                Document doc = fetchDocumentWithRetry(currentUrl);
                if (doc == null) break;

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("li[class*='_postListItem_']");

                if (postElements.isEmpty()) {
                    System.out.println("🚀 모든 페이지 크롤링 완료!");
                    break;
                }

                for (Element post : postElements) {
                    try {
                        // ✅ 제목 추출
                        Element titleElement = post.selectFirst("strong");
                        String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                        // ✅ URL 추출 (절대 URL 변환 로직 추가)
                        Element linkElement = post.selectFirst("a[href]");
                        String link = linkElement != null ? formatUrl(baseUrl, linkElement.attr("href")) : "링크 없음";

                        // ✅ 요약 추출
                        Element summaryElement = post.selectFirst("p");
                        String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                        // ✅ 게시일 추출 (여러 날짜 형식 지원)
                        Element dateElement = post.selectFirst("time");
                        LocalDate publishDate = parseKakopayDate(dateElement != null ? dateElement.text().trim() : "");

                        // ✅ 게시글 저장 (provider 수정됨)
                        articles.add(new ArticleDetails("KakaoPay", title, link, content, new ArrayList<>(), publishDate));
                        System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                    } catch (Exception e) {
                        System.err.println("⚠ 게시글 처리 중 오류 발생: " + e.getMessage());
                    }
                }

                currentPage++; // 다음 페이지로 이동
            }
        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        }

        return articles;
    }

    private LocalDate parseKakopayDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.equals("날짜 없음")) {
            return null; // 변환 실패 시 null 반환
        }

        // ✅ 입력값에서 연속된 공백 제거
        dateText = dateText.replaceAll("\\s+", "");  // 모든 공백 제거

        // ✅ 여러 날짜 형식 지원
        String[] datePatterns = {
                "yyyy-MM-dd",    // 2025-02-25
                "yyyy.MM.dd",    // 2025.02.25
                "yyyy/MM/dd",    // 2025/02/25
                "yyyy.M.d",      // 2025.1.24 (월, 일이 한 자리일 경우)
                "yyyy년M월d일"   // 2025년 1월 24일 (한국어 형식)
        };

        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsedDate = LocalDate.parse(dateText, formatter);
                System.out.println("✅ 날짜 변환 성공: " + dateText + " → " + parsedDate);
                return parsedDate;
            } catch (Exception ignored) {}
        }

        // ✅ 모든 패턴에서 변환 실패 시 오류 출력
        System.err.println("❌ 모든 형식에서 날짜 변환 실패: " + dateText);
        return null; // 변환 실패 시 null 반환
    }

    private int getKakopayMaxPages() {
        String baseUrl = "https://tech.kakaopay.com";
        try {
            Document doc = fetchDocumentWithRetry(baseUrl + "/page/1");
            if (doc == null) return 1;

            // ✅ 페이지네이션에서 마지막 페이지 숫자 가져오기
            Elements pageElements = doc.select("a[href^='/page/']");
            int maxPage = 1;

            for (Element pageElement : pageElements) {
                try {
                    String pageHref = pageElement.attr("href"); // 예: "/page/26"
                    int pageNum = Integer.parseInt(pageHref.replaceAll("[^0-9]", ""));
                    maxPage = Math.max(maxPage, pageNum);
                } catch (NumberFormatException ignored) {}
            }

            return maxPage; // ✅ 가장 큰 페이지 숫자 반환
        } catch (Exception e) {
            System.err.println("❌ 페이지네이션 정보 가져오기 실패: " + e.getMessage());
        }

        return 1; // 기본적으로 1페이지만 반환 (오류 발생 시)
    }

    private String formatUrl(String baseUrl, String link) {
        return link.startsWith("http") ? link : baseUrl + link;
    }

    private Document fetchDocumentWithRetry(String url) {
        int maxRetries = 3; // 🔹 최대 3회 재시도
        for (int i = 0; i < maxRetries; i++) {
            try {
                return Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .get();
            } catch (IOException e) {
                System.err.println("⚠ 크롤링 실패: " + url + " (재시도 " + (i + 1) + "/" + maxRetries + ")");
                try {
                    Thread.sleep(2000); // 2초 대기 후 재시도
                } catch (InterruptedException ignored) {}
            }
        }
        System.err.println("❌ 크롤링 포기: " + url);
        return null;
    }

    private List<ArticleDetails> fetchKakaobankArticles() {
        String baseUrl = "https://tech.kakaobank.com";

        List<ArticleDetails> articles = new ArrayList<>();
        String currentUrl = baseUrl;

        try {
            while (currentUrl != null) {
                System.out.println("🔍 크롤링 중: " + currentUrl);

                // Jsoup을 이용하여 HTML 가져오기
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(5000)
                        .get();

                // 게시글 목록 선택
                Elements postElements = doc.select("div.post");

                for (Element post : postElements) {
                    // 제목과 URL 추출
                    Element titleElement = post.selectFirst("h2.post-item.post-title a");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";
                    String link = titleElement != null ? titleElement.absUrl("href") : "#";

                    // 요약 추출
                    Element summaryElement = post.selectFirst("div.post-item.post-summary");
                    String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                    // 게시일 추출
                    // ✅ 날짜 크롤링
                    // ✅ 날짜 크롤링
                    Element dateElement = post.selectFirst("div.date");
                    String publishedAt = dateElement != null ? dateElement.text().trim() : "날짜 없음";
                    LocalDate publishDate = parsePublishDate(publishedAt);
                    if (publishDate == null) {
                        System.err.println("⚠ 날짜 변환 실패. 기본값(LocalDate.now()) 사용: " + publishedAt);
                        publishDate = LocalDate.now();
                    }

                    // 크롤링한 게시글 저장
                    articles.add(new ArticleDetails("KakaoBank", title, link, content, new ArrayList<>(), publishDate));

                    System.out.println("✔ 게시글 수집: " + title + " (" + publishedAt + ")");
                }

                // "다음 페이지" 버튼 찾기
                Element nextPageElement = doc.selectFirst("div.pag-next a");
                if (nextPageElement != null) {
                    currentUrl = nextPageElement.absUrl("href"); // 절대 URL 변환
                } else {
                    System.out.println("✅ 다음 페이지 없음. 크롤링 종료.");
                    currentUrl = null;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private List<ArticleDetails> fetchHyperconnectArticles() {
        String baseUrl = "https://hyperconnect.github.io";
        List<ArticleDetails> articles = new ArrayList<>();

        try {
            System.out.println("📌 크롤링 중: " + baseUrl);

            // ✅ Jsoup을 이용해 HTML 가져오기
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            // ✅ 게시글 목록 가져오기
            Elements postElements = doc.select("li");

            if (postElements.isEmpty()) {
                System.out.println("🚀 게시글이 없습니다.");
                return articles;
            }

            for (Element post : postElements) {
                // ✅ 제목 추출
                Element titleElement = post.selectFirst("h3 a.post-link");
                String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                // ✅ URL 추출 (절대 URL 변환)
                String link = titleElement != null ? baseUrl + titleElement.attr("href") : "링크 없음";

                // ✅ 요약 추출
                Element summaryElement = post.selectFirst("div.post-excerpt");
                String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                // ✅ 게시일 추출
                Element dateElement = post.selectFirst("time");
                String publishedAt = dateElement != null ? dateElement.text().trim() : "날짜 없음";

                // ✅ 크롤링된 원본 날짜 출력
                System.out.println("📅 크롤링한 원본 날짜: " + publishedAt);

                // ✅ 날짜 변환 시도
                LocalDate publishDate = parseHyperconnectDate(publishedAt);

                // ✅ 변환 실패 시 null 저장 (오늘 날짜 강제 저장 안 함)
                if (publishDate == null) {
                    System.err.println("❌ 날짜 변환 실패! null 저장됨. publishedAt: " + publishedAt);
                }

                // ✅ 게시글 저장
                articles.add(new ArticleDetails("Hyperconnect Blog", title, link, content, new ArrayList<>(), publishDate));
                System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
            }

        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private LocalDate parseHyperconnectDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.equals("날짜 없음")) {
            System.err.println("⚠ 날짜 없음. 변환 불가.");
            return null; // 변환 실패 시 null 반환
        }

        // ✅ 여러 날짜 형식 지원
        String[] datePatterns = {
                "yyyy-MM-dd",    // 2024-09-25
                "yyyy.MM.dd.",   // 2024.09.25.
                "yyyy.MM.dd",    // 2024.09.25
                "yyyy/MM/dd"     // 2024/09/25
        };

        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsedDate = LocalDate.parse(dateText, formatter);
                System.out.println("✅ 날짜 변환 성공: " + dateText + " → " + parsedDate);
                return parsedDate;
            } catch (Exception ignored) {}
        }

        // ✅ 모든 패턴에서 변환 실패 시 오류 출력
        System.err.println("❌ 모든 형식에서 날짜 변환 실패: " + dateText);
        return null; // 변환 실패 시 null 반환
    }

    private List<ArticleDetails> fetchHancomArticles() {
        String baseUrl = "https://tech.hancom.com/blog/";
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        List<ArticleDetails> articles = new ArrayList<>();
        String currentUrl = baseUrl; // 첫 번째 페이지 URL

        try {
            while (currentUrl != null) {
                System.out.println("📌 페이지 크롤링: " + currentUrl);

                // ✅ 페이지 요청 및 파싱
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .timeout(5000)
                        .get();

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("div.uc_post_list_content");

                if (postElements.isEmpty()) {
                    System.out.println("🚫 게시글이 없습니다. 크롤링 종료.");
                    break;
                }

                for (Element post : postElements) {
                    // ✅ 제목 크롤링
                    Element titleElement = post.selectFirst("h4.uc_post_list_title a");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";
                    String link = titleElement != null ? titleElement.absUrl("href") : "#";

                    // ✅ 날짜 크롤링
                    Element dateElement = post.selectFirst("div.ue-grid-item-meta-data:has(i.fas.fa-calendar-week)");
                    String publishDateText = "날짜 없음";
                    LocalDate publishDate = LocalDate.of(1970, 1, 1); // 기본값 설정

                    if (dateElement == null) {
                        System.out.println("⚠ 날짜 요소를 찾을 수 없음!");
                    } else {
                        publishDateText = dateElement.text().trim(); // 날짜 값 가져오기
                        System.out.println("📅 크롤링한 원본 날짜: " + publishDateText);

                        // ✅ 날짜 변환 시도
                        if (!publishDateText.equals("날짜 없음")) {
                            publishDate = LocalDate.parse(publishDateText, DATE_FORMATTER);
                        }
                    }

                    // ✅ 요약 크롤링
                    Element contentElement = post.selectFirst("div.uc_post_content");
                    String content = contentElement != null ? contentElement.text().trim() : "요약 없음";

                    // ✅ 게시글 저장
                    articles.add(new ArticleDetails("Hancom", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }


                // ✅ 다음 페이지 찾기
                Element nextPageElement = doc.selectFirst("a.page-numbers[href]:contains(" + (getCurrentPageNumber(currentUrl) + 1) + ")");

                if (nextPageElement != null) {
                    currentUrl = nextPageElement.absUrl("href");
                    System.out.println("📌 다음 페이지 이동: " + currentUrl);
                } else {
                    System.out.println("🚫 다음 페이지 없음. 크롤링 종료.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        }
        return articles;
    }

    private int getCurrentPageNumber(String url) {
        String baseUrl = "https://tech.hancom.com/blog/";
        String[] parts = url.replace(baseUrl, "").split("/");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return 1; // 기본적으로 첫 번째 페이지로 처리
        }
    }

    private List<ArticleDetails> fetchGmarketArticles() {
        String baseUrl = "https://ebay-korea.tistory.com/category/Backend?page=";
        List<ArticleDetails> articles = new ArrayList<>();
        int currentPage = 1;
        int maxPages = getGmarketMaxPages(); // 🔹 최대 페이지 동적으로 가져오기

        try {
            while (currentPage <= maxPages) {
                String currentUrl = baseUrl + currentPage;
                System.out.println("📌 크롤링 중: " + currentUrl);

                // ✅ Jsoup을 이용해 HTML 가져오기
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .get();

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("div.list_content");

                if (postElements.isEmpty()) {
                    System.out.println("🚀 모든 페이지 크롤링 완료!");
                    break;
                }

                for (Element post : postElements) {
                    // ✅ 제목 추출
                    Element titleElement = post.selectFirst("strong.tit_post");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                    // ✅ URL 추출
                    Element linkElement = post.selectFirst("a.link_post");
                    String link = linkElement != null ? "https://ebay-korea.tistory.com" + linkElement.attr("href") : "링크 없음";

                    // ✅ 요약 추출
                    Element summaryElement = post.selectFirst("p.txt_post");
                    String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                    // ✅ 게시일 추출
                    Element dateElement = post.selectFirst("script[type='text/javascript']");
                    LocalDate publishDate = extractDateFromScript(dateElement);
                    // ✅ 게시글 저장
                    articles.add(new ArticleDetails("Gmarket Tech Blog", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }

                currentPage++; // ✅ 다음 페이지로 이동
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }

    private int getGmarketMaxPages() {
        String baseUrl = "https://ebay-korea.tistory.com/category/Backend?page=";
        try {
            Document doc = Jsoup.connect(baseUrl + "1")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            // ✅ 페이지네이션에서 마지막 페이지 숫자 가져오기
            Elements pageElements = doc.select("div.area_paging a");
            int maxPage = 1;

            for (Element pageElement : pageElements) {
                String pageText = pageElement.text().trim();
                if (pageText.matches("\\d+")) { // 숫자만 찾기
                    int pageNum = Integer.parseInt(pageText);
                    maxPage = Math.max(maxPage, pageNum);
                }
            }

            return maxPage; // ✅ 가장 큰 페이지 숫자 반환
        } catch (IOException e) {
            System.err.println("❌ 페이지네이션 정보 가져오기 실패: " + e.getMessage());
        }

        return 1; // 기본적으로 1페이지만 반환 (오류 발생 시)
    }

    private LocalDate extractDateFromScript(Element scriptElement) {
        if (scriptElement == null) {
            return null; // 날짜를 찾지 못하면 null 반환
        }

        // 🔹 script 태그의 전체 내용 가져오기
        String scriptText = scriptElement.html();

        // 🔹 정규식 패턴: 'YYYY. MM. DD.' 형식 추출
        Pattern pattern = Pattern.compile("(\\d{4})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})");
        Matcher matcher = pattern.matcher(scriptText);

        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));  // YYYY
            int month = Integer.parseInt(matcher.group(2)); // MM
            int day = Integer.parseInt(matcher.group(3));   // DD

            return LocalDate.of(year, month, day);
        }

        return null; // 날짜를 찾지 못하면 null 반환
    }

    private List<ArticleDetails> fetchDevoceanArticles() {
        String baseUrl = "https://devocean.sk.com/blog/index.do?page=";
        String articleBaseUrl = "https://devocean.sk.com/blog/techBoardDetail.do?ID=";
        List<ArticleDetails> articles = new ArrayList<>();
        int currentPage = 1;
        int maxPages = getDevoceanMaxPages(); // 🔹 최대 페이지 동적으로 가져오기

        try {
            while (currentPage <= maxPages) {
                String currentUrl = baseUrl + currentPage;
                System.out.println("📌 크롤링 중: " + currentUrl);

                // ✅ Jsoup을 이용해 HTML 가져오기
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .timeout(10000)
                        .get();

                // ✅ 게시글 목록 가져오기
                Elements postElements = doc.select("div.sec-cont");

                if (postElements.isEmpty()) {
                    System.out.println("🚀 모든 페이지 크롤링 완료!");
                    break;
                }

                for (Element post : postElements) {
                    // ✅ 제목 추출
                    Element titleElement = post.selectFirst("h3.title");
                    String title = titleElement != null ? titleElement.text().trim() : "제목 없음";

                    // ✅ 게시글 ID 추출
                    String onclickAttr = titleElement != null ? titleElement.attr("onclick") : null;
                    String postId = null;
                    if (onclickAttr != null && !onclickAttr.isEmpty()) {
                        Pattern pattern = Pattern.compile("'(\\d+)'");
                        Matcher matcher = pattern.matcher(onclickAttr);
                        if (matcher.find()) {
                            postId = matcher.group(1);
                        }
                    }

                    // ✅ URL 생성
                    String link = (postId != null) ? articleBaseUrl + postId + "&boardType=techBlog" : "링크 없음";

                    // ✅ 날짜 추출 (String → LocalDate 변환)
                    Element dateElement = post.selectFirst("span.date");
                    String dateText = dateElement != null ? dateElement.text().trim() : "날짜 없음";
                    LocalDate publishDate = parseDevoceanDate(dateText);

                    // ✅ 요약 내용(컨텐츠) 추출
                    Element descElement = post.selectFirst("p.desc");
                    String content = descElement != null ? descElement.text().trim() : "요약 없음";

                    // ✅ 게시글 저장
                    articles.add(new ArticleDetails("Devocean Blog", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " | " + publishDate + " | " + link);
                }

                currentPage++; // ✅ 다음 페이지로 이동
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        return articles;
    }



    private int getDevoceanMaxPages() {
        try {
            Document doc = Jsoup.connect("https://devocean.sk.com/blog/index.do?page=1")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/110.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            Elements pageElements = doc.select("div.sec-area-paging a");
            int maxPage = 1;
            for (Element pageElement : pageElements) {
                String pageText = pageElement.text().trim();
                if (pageText.matches("\\d+")) {
                    int pageNum = Integer.parseInt(pageText);
                    maxPage = Math.max(maxPage, pageNum);
                }
            }
            return maxPage;
        } catch (IOException e) {
            System.err.println("❌ 페이지네이션 정보 가져오기 실패: " + e.getMessage());
        }
        return 1;
    }

    private LocalDate parseDevoceanDate(String dateText) {
        if (dateText.isEmpty() || dateText.equals("날짜 없음")) {
            return LocalDate.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd");
            return LocalDate.parse(dateText, formatter);
        } catch (Exception e) {
            System.err.println("⚠ Devocean 날짜 변환 실패: " + dateText);
            return LocalDate.now();
        }
    }

    private List<ArticleDetails> fetchDanawaArticles() {
        List<ArticleDetails> articles = new ArrayList<>();
        try {
            String baseUrl = "https://danawalab.github.io";
            System.out.println("📌 크롤링 중: " + baseUrl);

            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/110.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            Elements postElements = doc.select("div.content__post");
            if (postElements.isEmpty()) {
                System.out.println("🚀 게시글이 없습니다.");
                return articles;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
            for (Element post : postElements) {
                Element titleElement = post.selectFirst("h3.content__h3");
                String title = (titleElement != null) ? titleElement.text().trim() : "제목 없음";

                Element linkElement = post.selectFirst("a.content__link");
                String link = (linkElement != null) ? baseUrl + linkElement.attr("href") : "링크 없음";

                Element summaryElement = post.selectFirst("p.content__p");
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                Element dateElement = post.selectFirst("span.date");
                LocalDate publishDate = parseDanawaDate((dateElement != null) ? dateElement.text().trim() : "");

                articles.add(new ArticleDetails("Danawa Lab Blog", title, link, content, new ArrayList<>(), publishDate));
                System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
            }
        } catch (IOException e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        return articles;
    }

    private LocalDate parseDanawaDate(String dateText) {
        if (dateText.isEmpty() || dateText.equals("날짜 없음")) {
            return LocalDate.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
            return LocalDate.parse(dateText, formatter);
        } catch (Exception e) {
            System.err.println("⚠ 날짜 변환 실패: " + dateText);
            return LocalDate.now();
        }
    }

    private List<ArticleDetails> fetchDevsistersArticles() {
        List<ArticleDetails> articles = new ArrayList<>();
        int MAX_PAGES = 4;
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // UI 없이 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        WebDriver driver = new ChromeDriver(options);

        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                String url = "https://tech.devsisters.com?page=" + page;
                System.out.println("📌 현재 페이지: " + url);
                driver.get(url);

                // JavaScript 실행 시간 대기
                Thread.sleep(3000);

                // Jsoup으로 Selenium에서 가져온 HTML 파싱
                Document doc = Jsoup.parse(driver.getPageSource());

                Elements postElements = doc.select("li[class^=group]");
                if (postElements.isEmpty()) {
                    System.out.println("🚫 게시글 없음. 종료.");
                    break;
                }

                for (Element post : postElements) {
                    Element titleElement = post.selectFirst("h3");
                    Element linkElement = post.selectFirst("a[href]");
                    Element summaryElement = post.selectFirst("p");
                    Element dateElement = post.selectFirst("time");

                    if (titleElement == null || linkElement == null) continue;

                    String title = titleElement.text().trim();
                    String relativeUrl = linkElement.attr("href");
                    String link = relativeUrl.startsWith("http") ? relativeUrl : "https://tech.devsisters.com" + relativeUrl;
                    String content = summaryElement != null ? summaryElement.text().trim() : "요약 없음";

                    LocalDate publishDate = LocalDate.now();
                    if (dateElement != null) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            publishDate = LocalDate.parse(dateElement.text().trim(), formatter);
                        } catch (Exception e) {
                            System.err.println("⚠ 날짜 파싱 실패: " + dateElement.text().trim());
                        }
                    }

                    articles.add(new ArticleDetails("Devsisters", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 수집 완료: " + title);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return articles;
    }


    private List<ArticleDetails> fetchWoowahanArticles() {
        String baseUrl = "https://techblog.woowahan.com/category/backend/";
        List<ArticleDetails> articles = new ArrayList<>();
        Set<String> collectedUrls = new HashSet<>();

        int maxPage = getWoowahanMaxPages();
        System.out.println("📌 최대 페이지: " + maxPage);

        for (int page = 1; page <= maxPage; page++) {
            String currentUrl = (page == 1) ? baseUrl : baseUrl + "page/" + page + "/";
            System.out.println("📌 크롤링 중: " + currentUrl);

            try {
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                        .header("Accept-Language", "ko-KR,ko;q=0.9")
                        .timeout(5000)
                        .get();

                Elements postElements = doc.select("div.posts div.item");

                if (postElements.isEmpty()) {
                    System.out.println("🚫 게시글이 없습니다. 크롤링 종료.");
                    break;
                }

                for (Element post : postElements) {
                    try {
                        Element titleElement = post.selectFirst("h2.post-title");
                        Element linkElement = post.selectFirst("a[href]");
                        Element summaryElement = post.selectFirst("p.post-excerpt");
                        Element dateElement = post.selectFirst("time.post-author-date");

                        String title = (titleElement != null) ? titleElement.text().trim() : "제목 없음";
                        String link = (linkElement != null) ? linkElement.absUrl("href") : "#";
                        String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";
                        String dateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                        if (title.contains("{{") || link.equals("#")) {
                            System.out.println("⚠️ 템플릿 데이터 감지, 스킵: " + title);
                            continue;
                        }

                        LocalDate publishDate = parseWoowahanDate(dateText);

                        // 중복 방지
                        if (!collectedUrls.contains(link)) {
                            collectedUrls.add(link);
                            articles.add(new ArticleDetails("Woowahan", title, link, content, new ArrayList<>(), publishDate));
                            System.out.printf("✔ [Woowahan] 게시글 수집 완료: %s (작성일: %s, URL: %s)%n",
                                    title, publishDate, link);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 게시글 파싱 중 오류 발생: " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            }
        }
        return articles;
    }


    private int getWoowahanMaxPages() {
        int maxPage = 1;
        String baseUrl = "https://techblog.woowahan.com/category/backend/";

        try {
            Document doc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(5000)
                    .get();

            Element lastPageElement = doc.selectFirst("div.wp-pagenavi a.last");
            if (lastPageElement != null) {
                String lastPageHref = lastPageElement.attr("href");

                Matcher matcher = Pattern.compile("/page/(\\d+)/").matcher(lastPageHref);
                if (matcher.find()) {
                    maxPage = Integer.parseInt(matcher.group(1));
                    System.out.println("📌 Jsoup 최대 페이지 확인: " + maxPage);
                } else {
                    System.out.println("⚠️ 페이지 숫자 추출 실패: " + lastPageHref);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Jsoup으로 최대 페이지 가져오기 실패: " + e.getMessage());
        }

        return maxPage;
    }


    private LocalDate parseWoowahanDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return LocalDate.now(); // 기본값

        // 패턴: "Feb.25.2025"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM.dd.yyyy", Locale.ENGLISH);

        try {
            return LocalDate.parse(rawDate, formatter);
        } catch (Exception e) {
            System.err.println("⚠ 날짜 변환 실패 (미지원 형식): " + rawDate);
            return LocalDate.now(); // 실패 시 현재 날짜 반환
        }
    }
    private List<ArticleDetails> fetchElevenArticles() {
        String BASE_URL = "https://11st-tech.github.io";
        List<ArticleDetails> articles = new ArrayList<>();
        Set<String> visitedLinks = new HashSet<>();
        String currentUrl = BASE_URL;

        while (currentUrl != null) {
            try {
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(5000)
                        .get();

                Elements postElements = doc.select("li.post-item.post");

                for (Element post : postElements) {
                    String title = getText(post, "h3.post-title");
                    String link = getAbsoluteLink(BASE_URL, post, "a[href]");
                    String content = getText(post, "p.post-excerpt");
                    String publishDateText = getText(post, "div.post-meta p.post-date");
                    LocalDate publishDate = parseDate(publishDateText);

                    if (link != null && visitedLinks.add(link)) {  // ✅ 중복 URL 방지
                        articles.add(new ArticleDetails("ElevenST Tech Blog", title, link, content, new ArrayList<>(), publishDate));
                        System.out.println("✔ 게시글 수집 완료: " + title + " (" + publishDate + ")");
                    }
                }

                // ✅ 다음 페이지 확인
                Element nextPageElement = doc.selectFirst("li#page-next a[href]");
                if (nextPageElement != null) {
                    String nextUrl = getAbsoluteLink(BASE_URL, nextPageElement, "a");
                    if (nextUrl != null) {
                        currentUrl = nextUrl;
                        System.out.println("📌 다음 페이지 이동: " + currentUrl);
                    } else {
                        System.out.println("🚫 다음 페이지 없음. 크롤링 종료.");
                        break;
                    }
                } else {
                    System.out.println("🚫 마지막 페이지 도달");
                    break;
                }

            } catch (IOException e) {
                System.err.println("❌ 네트워크 오류: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("⚠ 크롤링 중 오류 발생: " + e.getMessage());
                break;
            }
        }
        return articles;
    }

    /**
     * ✅ 요소에서 텍스트 가져오는 메서드
     */
    private String getText(Element element, String selector) {
        Element found = element.selectFirst(selector);
        return (found != null) ? found.text().trim() : "정보 없음";
    }

    /**
     * ✅ 상대 경로를 절대 경로로 변환하는 메서드
     */
    private String getAbsoluteLink(String baseUrl, Element element, String selector) {
        Element found = element.selectFirst(selector);
        if (found != null) {
            String href = found.attr("href").trim();
            if (!href.isEmpty()) {
                if (href.startsWith("http")) {
                    return href; // ✅ 이미 절대 URL이면 그대로 반환
                } else {
                    return baseUrl + (href.startsWith("/") ? href : "/" + href); // ✅ 상대경로 보정
                }
            }
        }
        return null; // ✅ 잘못된 URL 방지
    }

    /**
     * ✅ 게시글 날짜 변환 메서드
     */
    private LocalDate parseDate(String dateText) {
        if (dateText.isEmpty() || dateText.equals("정보 없음")) {
            return LocalDate.now(); // 날짜 정보가 없을 경우, 현재 날짜를 저장
        }

        // 지원하는 날짜 형식
        String[] datePatterns = {
                "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/MM/dd", "MMM dd, yyyy"
        };

        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateText, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        System.err.println("⚠ 날짜 변환 실패: " + dateText);
        return LocalDate.now(); // 변환 실패 시 현재 날짜 저장 (안정성 유지)
    }
}

