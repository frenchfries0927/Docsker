package com.project.moduleservicedatacollection.service.TechBlog;

import com.project.moduleservicedatacollection.dto.TechBlog.ArticleDetails;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SeleniumService implements TechBlogService {

    public List<String> getSupportedBlogTypes() {
        return List.of(
                "navercloudplatform",
                "cjonstyle",
                "kurly",
                "lgcns",
                "samsung",
                "skplanet",
                "tosstech",
                "kakaomobility",
                "naverd2",
                "tada",
                "daangn"

        );
    }

    @Override
    public List<ArticleDetails> fetchArticles(String blogType) {
        if (blogType.equalsIgnoreCase("navercloudplatform")) {
            return fetchNavercloudplatformArticles();
        } else if (blogType.equalsIgnoreCase("cjonstyle")) {
            return fetchCjonstyleArticles();
        } else if (blogType.equalsIgnoreCase("kurly")) {
            return fetchKurlyArticles();
        } else if (blogType.equalsIgnoreCase("lgcns")) {
            return fetchLgcnsArticles();
        } else if (blogType.equalsIgnoreCase("samsung")) {
            return fetchSamsungArticles();
        } else if (blogType.equalsIgnoreCase("skplanet")) {
            return fetchSkplanetArticles();
        } else if (blogType.equalsIgnoreCase("tosstech")) {
            return fetchTosstechArticles();
        } else if (blogType.equalsIgnoreCase("kakaomobility")) {
            return fetchKakaomobilityArticles();
        } else if (blogType.equalsIgnoreCase("naverd2")) {
            return fetchNaverd2Articles();
        } else if (blogType.equalsIgnoreCase("tada")) {
            return fetchTadaArticles();
        } else if (blogType.equalsIgnoreCase("daangn")) {
            return fetchDaangnArticles();
        }else {
            throw new IllegalArgumentException("Invalid blog type: " + blogType);
        }
    }

    private List<ArticleDetails> fetchNavercloudplatformArticles() {
        String baseUrl = "https://medium.com/naver-cloud-platform";

        List<ArticleDetails> articles = new ArrayList<>();
        WebDriverManager.chromedriver().setup();

        // ✅ 크롬 옵션 설정 (자동화 탐지 우회 + Headless 모드)
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
            // ✅ Medium 블로그 페이지 접속
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // ✅ 모든 게시글을 로드할 때까지 스크롤 다운
            loadNavercloudplatformAllPosts(driver);

            // ✅ 게시글이 로드될 때까지 대기
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.js-trackPostPresentation")));

            // ✅ 페이지 소스를 Jsoup으로 파싱
            Document doc = Jsoup.parse(driver.getPageSource());

            // ✅ 게시글 목록 크롤링
            Elements postElements = doc.select("div.js-trackPostPresentation");

            for (Element post : postElements) {
                // ✅ 제목 크롤링
                Element titleElement = post.selectFirst("h3 div.u-letterSpacingTight");
                if (titleElement == null) {
                    titleElement = post.selectFirst("a");
                }
                String title = (titleElement != null) ? titleElement.text().trim() : "제목 없음";

                // ✅ 링크 크롤링
                Element linkElement = post.selectFirst("a[href]");
                String link = (linkElement != null) ? linkElement.absUrl("href") : "#";

                // ✅ 작성자 및 날짜 크롤링
                Element authorElement = post.selectFirst("div.postMetaInline a");
                Element dateElement = post.selectFirst("time");

                String author = (authorElement != null) ? authorElement.text().trim() : "알 수 없음";
                String dateStr = (dateElement != null) ? dateElement.attr("datetime") : "날짜 없음";
                LocalDate publishDate = parseloadNavercloudplatformAllPostsDate(dateStr);

                // ✅ 요약 크롤링
                Element summaryElement = post.selectFirst("div.u-fontSize18");
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                // ✅ ArticleDetails 객체로 저장
                articles.add(new ArticleDetails("Naver Cloud Platform", title, link, content, new ArrayList<>(), publishDate));

                // ✅ 콘솔 출력
                System.out.printf("✔ 게시글 수집: %s (작성자: %s, 날짜: %s, URL: %s)%n",
                        title, author, publishDate, link);
            }

        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    private void loadNavercloudplatformAllPosts(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int lastHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            try {
                Thread.sleep(3000); // 3초 대기 (서버 부하 방지)
            } catch (InterruptedException ignored) {
            }

            int newHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
            if (newHeight == lastHeight) {
                break; // 더 이상 로드할 게시글이 없음
            }
            lastHeight = newHeight;
        }
    }

    private LocalDate parseloadNavercloudplatformAllPostsDate(String dateText) {
        if (dateText.equals("날짜 없음") || dateText.isEmpty()) {
            return LocalDate.now();
        }

        try {
            return Instant.parse(dateText).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            System.err.println("❌ 날짜 변환 실패: " + dateText);
            return LocalDate.now();
        }
    }

    private List<ArticleDetails> fetchCjonstyleArticles() {
        String baseUrl = "https://medium.com/cj-onstyle";

        List<ArticleDetails> articles = new ArrayList<>();
        WebDriverManager.chromedriver().setup();

        // ✅ 크롬 옵션 설정 (자동화 탐지 우회 + Headless 모드)
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
            // ✅ Medium 블로그 페이지 접속
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // ✅ 모든 게시글을 로드할 때까지 스크롤 다운
            loadCjonstyleAllPosts(driver);

            // ✅ 게시글이 로드될 때까지 대기
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.js-trackPostPresentation")));

            // ✅ 페이지 소스를 Jsoup으로 파싱
            Document doc = Jsoup.parse(driver.getPageSource());

            // ✅ 게시글 목록 크롤링
            Elements postElements = doc.select("div.js-trackPostPresentation");

            for (Element post : postElements) {
                // ✅ 제목 크롤링 (여러 구조 대응)
                Element titleElement = post.selectFirst("h3 div.u-letterSpacingTight, a");
                String title = (titleElement != null) ? titleElement.text().trim() : "제목 없음";

                // ✅ 링크 크롤링
                Element linkElement = post.selectFirst("a[href]");
                String link = (linkElement != null) ? linkElement.absUrl("href") : "#";

                // ✅ 작성자 및 날짜 크롤링
                Element authorElement = post.selectFirst("div.postMetaInline a");
                Element dateElement = post.selectFirst("time");

                String author = (authorElement != null) ? authorElement.text().trim() : "알 수 없음";
                String dateStr = (dateElement != null) ? dateElement.attr("datetime") : "날짜 없음";
                LocalDate publishDate = parseCjonstyleDate(dateStr);

                // ✅ 요약 크롤링
                Element summaryElement = post.selectFirst("div.u-fontSize18");
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                // ✅ ArticleDetails 객체로 저장
                articles.add(new ArticleDetails("CJ OnStyle", title, link, content, new ArrayList<>(), publishDate));

                // ✅ 콘솔 출력
                System.out.printf("✔ 게시글 수집: %s (작성자: %s, 날짜: %s, URL: %s)%n",
                        title, author, publishDate, link);
            }

        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    private void loadCjonstyleAllPosts(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int lastHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            try {
                Thread.sleep(3000); // 3초 대기 (서버 부하 방지)
            } catch (InterruptedException ignored) {
            }

            int newHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
            if (newHeight == lastHeight) {
                break; // 더 이상 로드할 게시글이 없음
            }
            lastHeight = newHeight;
        }
    }

    private LocalDate parseCjonstyleDate(String dateText) {
        if (dateText.equals("날짜 없음") || dateText.isEmpty()) {
            return LocalDate.now();
        }

        try {
            return Instant.parse(dateText).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            System.err.println("❌ 날짜 변환 실패: " + dateText);
            return LocalDate.now();
        }
    }

    private List<ArticleDetails> fetchKurlyArticles() {
        String baseUrl = "https://helloworld.kurly.com";
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd.");

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");  // 최신 크롬 headless 모드
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            // ✅ 페이지 이동 및 대기
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));  // ⏳ 대기 시간 증가
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.post-list li.post-card")));

            // ✅ HTML 가져오기
            Document doc = Jsoup.parse(driver.getPageSource());

            // ✅ 게시글 크롤링
            Elements postElements = doc.select("ul.post-list li.post-card");

            for (Element post : postElements) {
                // ✅ 제목 크롤링 (h3.post-title 내부 텍스트 가져오기)
                Element titleElement = post.selectFirst("h3.post-title");
                String title = (titleElement != null) ? titleElement.text().trim() : "제목 없음";

                // ✅ 링크 크롤링 (a.post-link에서 href 가져오기)
                Element linkElement = post.selectFirst("a.post-link");
                String link = (linkElement != null) ? baseUrl + linkElement.attr("href") : "#";

                // ✅ 내용 크롤링 (p.title-summary에서 텍스트 가져오기)
                Element summaryElement = post.selectFirst("p.title-summary");
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                // ✅ 날짜 크롤링 (span.post-date에서 텍스트 가져오기)
                Element dateElement = post.selectFirst("span.post-date");
                String dateStr = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                // ✅ 날짜 변환 (yyyy.MM.dd. 형식 → LocalDate 변환)
                LocalDate publishDate = null;
                try {
                    if (!dateStr.equals("날짜 없음")) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
                        publishDate = LocalDate.parse(dateStr, formatter);
                    }
                } catch (Exception e) {
                    System.err.println("⚠ 날짜 변환 실패: " + dateStr);
                }

                // ✅ 게시글 저장 (작성자 제거)
                articles.add(new ArticleDetails("Kurly", title, link, content, new ArrayList<>(), publishDate));

                // ✅ 콘솔 출력
                System.out.printf("✔ 게시글 수집: %s (날짜: %s, URL: %s, 요약: %s)%n",
                        title, publishDate, link, content);
            }


        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;

    }

    private List<ArticleDetails> fetchLgcnsArticles() {
        String baseUrl = "https://www.lgcns.com/blog/cns-tech/aws-ambassador/";
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
        Set<String> visitedPages = new HashSet<>();

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        try {
            String currentUrl = baseUrl;
            boolean hasNextPage = true;

            while (hasNextPage) {
                if (visitedPages.contains(currentUrl)) {
                    System.out.println("🔄 이미 방문한 페이지: " + currentUrl);
                    break; // 중복 방문 방지
                }
                visitedPages.add(currentUrl);

                driver.get(currentUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".post-list li a"))); // ✅ 게시글 로딩 대기

                // ✅ HTML 가져오기
                Document doc = Jsoup.parse(driver.getPageSource());

                // ✅ 게시글 크롤링
                Elements postElements = doc.select(".post-list li");

                if (postElements.isEmpty()) {
                    System.out.println("⚠ 게시글이 없습니다. 크롤링을 중단합니다.");
                    break;
                }

                for (Element post : postElements) {
                    Element linkElement = post.selectFirst("a[href]");
                    Element titleElement = post.selectFirst("span.title");
                    Element dateElement = post.selectFirst("span.date");

                    if (linkElement == null || titleElement == null) continue;

                    // ✅ 상대 경로를 절대 경로로 변환
                    String link = linkElement.attr("href");
                    if (!link.startsWith("http")) {
                        link = baseUrl + link;
                    }

                    // ✅ 제목 크롤링
                    String title = titleElement.text().trim();

                    // ✅ 날짜 크롤링
                    String publishDateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";
                    LocalDate publishDate = parseDate(publishDateText); // ✅ 날짜 변환 함수 사용

                    articles.add(new ArticleDetails("LGCNS", title, link, "요약없음", new ArrayList<>(), publishDate));

                    System.out.printf("✔ 게시글 수집: %s (날짜: %s, URL: %s)%n", title,
                            (publishDate != null) ? publishDate : "변환 실패", link);
                }


                // ✅ 다음 페이지 찾기
                Element nextPageElement = doc.selectFirst(".pagination a.next[href]"); // 🔥 선택자 수정됨!
                if (nextPageElement != null) {
                    String nextPageUrl = nextPageElement.absUrl("href"); // ✅ 절대 URL로 변환

                    if (!visitedPages.contains(nextPageUrl)) {
                        currentUrl = nextPageUrl;
                        System.out.println("➡️ 다음 페이지 이동: " + currentUrl);
                    } else {
                        System.out.println("🔄 중복 페이지 감지: " + nextPageUrl + " (탐색 중지)");
                        hasNextPage = false;
                    }
                } else {
                    System.out.println("🛑 마지막 페이지 도달");
                    hasNextPage = false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }
    private LocalDate parseDate(String dateText) {
        if (dateText.equals("날짜 없음")) return null; // ✅ 날짜가 없으면 null 반환

        System.out.println("📅 가져온 날짜 문자열: " + dateText); // 📢 디버깅 로그

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),  // ✅ 기존 패턴에서 마지막 점 제거
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateText, formatter);
            } catch (DateTimeParseException ignored) {}
        }

        System.err.println("⚠ 날짜 변환 실패: " + dateText);
        return null; // ✅ 변환 실패 시 null 반환
    }



    private List<ArticleDetails> fetchSamsungArticles() {
        String baseUrl = "https://techblog.samsung.com";
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd.");

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            int page = 1;
            boolean hasNextPage = true;

            while (hasNextPage) {
                String pageUrl = baseUrl + "?page=" + page;
                driver.get(pageUrl);

                // ✅ 페이지 로딩 대기
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.blog-list li")));

                // ✅ 페이지 소스 가져오기
                Document doc = Jsoup.parse(driver.getPageSource());

                // ✅ 게시글 크롤링
                Elements postElements = doc.select("ul.blog-list li");

                if (postElements.isEmpty()) {
                    System.out.println("⚠ 게시글이 없습니다. 크롤링을 중단합니다.");
                    break;
                }

                for (Element post : postElements) {
                    Element linkElement = post.selectFirst("a[href]");
                    Element titleElement = post.selectFirst("h3");
                    Element dateElement = post.selectFirst("span.date");

                    if (linkElement == null || titleElement == null) continue;

                    String link = baseUrl + linkElement.attr("href");
                    String title = titleElement.text().trim();
                    String publishDateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                    // ✅ 날짜 변환 로직
                    LocalDate publishDate;
                    try {
                        Pattern pattern = Pattern.compile("^(\\w+ \\d{1,2}, \\d{4})");
                        Matcher matcher = pattern.matcher(publishDateText);
                        String dateOnly = matcher.find() ? matcher.group(1) : "날짜 없음";

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
                        publishDate = LocalDate.parse(dateOnly, formatter);
                    } catch (DateTimeParseException e) {
                        System.err.println("❌ 날짜 변환 실패: " + publishDateText + " (기본값: 오늘 날짜 사용)");
                        publishDate = LocalDate.now();
                    }

                    // ✅ 본문 요약 생략 (null 또는 빈 문자열 처리)
                    articles.add(new ArticleDetails("Samsung", title, link, "요약없음", new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }

                // ✅ 다음 페이지 확인
                Element nextPageElement = doc.selectFirst("a[href^='?page=" + (page + 1) + "']");
                if (nextPageElement != null) {
                    page++;  // 다음 페이지
                } else {
                    hasNextPage = false;  // 종료
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }



    private List<ArticleDetails> fetchSkplanetArticles() {
        String baseUrl = "https://techtopic.skplanet.com";

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");  // 최신 크롬 headless 모드
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-blink-features=AutomationControlled");  // 자동화 탐지 우회
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            String currentUrl = baseUrl;
            boolean hasNextPage = true;

            while (hasNextPage) {
                // ✅ 페이지 이동 및 대기
                driver.get(currentUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article.post-list-item")));

                // ✅ 페이지 소스 가져오기
                Document doc = Jsoup.parse(driver.getPageSource());

                // ✅ 게시글 크롤링
                Elements postElements = doc.select("article.post-list-item");

                if (postElements.isEmpty()) {
                    System.out.println("⚠ 게시글이 없습니다. 크롤링을 중단합니다.");
                    break;
                }

                for (Element post : postElements) {
                    Element linkElement = post.selectFirst("h2.title a[itemprop=url]");
                    Element dateElement = post.selectFirst("small");
                    Element summaryElement = post.selectFirst("section p[itemprop=description]");

                    if (linkElement == null) continue;

                    String link = baseUrl + linkElement.attr("href");
                    String title = linkElement.text().trim();
                    String publishDateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";
                    String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                    // 📌 날짜 변환 로직 추가
                    LocalDate publishDate;
                    try {
                        // '|' 기호로 날짜와 작성자명을 분리
                        String[] dateParts = publishDateText.split(" \\| ");
                        String dateOnly = dateParts[0].trim(); // 첫 번째 요소만 날짜 부분으로 사용

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA);
                        publishDate = LocalDate.parse(dateOnly, formatter);
                    } catch (DateTimeParseException e) {
                        System.err.println("❌ 날짜 변환 실패: " + publishDateText + " (기본값: 오늘 날짜 사용)");
                        publishDate = LocalDate.now(); // 변환 실패 시 오늘 날짜 사용
                    }

                    articles.add(new ArticleDetails("SK 플래닛", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }

                // ✅ 다음 페이지 확인
                Element nextPageElement = doc.selectFirst("div.pagination a:contains(›)");
                if (nextPageElement != null) {
                    currentUrl = baseUrl + nextPageElement.attr("href");
                } else {
                    hasNextPage = false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    private List<ArticleDetails> fetchTosstechArticles() {
        String baseUrl = "https://toss.tech/tech";

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // UI 없이 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        try {
            String currentUrl = baseUrl;
            boolean hasNextPage = true;

            while (hasNextPage) {
                driver.get(currentUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.p-pagination__list")));

                // ✅ HTML 가져오기
                Document doc = Jsoup.parse(driver.getPageSource());

                // ✅ 게시글 크롤링
                Elements postElements = doc.select("div.css-132j2b5 li a");

                for (Element post : postElements) {
                    String href = post.attr("href");
                    String link = href.startsWith("http") ? href : "https://toss.tech" + href; // 외부 링크, 내부 링크 모두 처리

                    Element titleElement = post.selectFirst("span.typography--h6");
                    Element summaryElement = post.selectFirst("span.typography--p");
                    Element dateElement = post.selectFirst("span.typography--small");

                    if (titleElement == null) continue; // 필수 조건

                    String title = titleElement.text().trim();
                    String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";
                    String publishDateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                    // 📌 날짜 변환 로직
                    LocalDate publishDate;
                    try {
                        // 날짜만 추출 (예: "2025년 1월 14일 · 송지수" → "2025년 1월 14일")
                        Pattern pattern = Pattern.compile("^(\\d{4}년 \\d{1,2}월 \\d{1,2}일)");
                        Matcher matcher = pattern.matcher(publishDateText);
                        String dateOnly = matcher.find() ? matcher.group(1) : "날짜 없음";

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN);
                        publishDate = LocalDate.parse(dateOnly, formatter);
                    } catch (DateTimeParseException e) {
                        System.err.println("❌ 날짜 변환 실패: " + publishDateText + " (기본값: 오늘 날짜 사용)");
                        publishDate = LocalDate.now();
                    }

                    articles.add(new ArticleDetails("Toss", title, link, content, new ArrayList<>(), publishDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
                }

                // ✅ 페이지네이션 처리
                Element nextPageElement = doc.selectFirst("button[aria-label='next']");
                if (nextPageElement != null) {
                    currentUrl = baseUrl + "?page=" + (extractTosstechCurrentPage(currentUrl) + 1);
                    System.out.println("➡️ 다음 페이지 이동: " + currentUrl);
                } else {
                    System.out.println("🛑 마지막 페이지 도달");
                    hasNextPage = false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }


    private int extractTosstechCurrentPage(String url) {
        Pattern pattern = Pattern.compile(".*[?&]page=(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1; // 기본적으로 첫 페이지로 설정
    }

    private List<ArticleDetails> fetchKakaomobilityArticles() {
        String baseUrl = "https://developers.kakaomobility.com/techblogs";

        List<ArticleDetails> articles = new ArrayList<>();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // UI 없이 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-blink-features=AutomationControlled");  // 자동화 탐지 우회
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        try {
            // ✅ 페이지 이동 및 대기
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.list_tech li")));

            // ✅ 페이지 소스 가져오기
            Document doc = Jsoup.parse(driver.getPageSource());

            // ✅ 게시글 크롤링
            Elements postElements = doc.select("ul.list_tech li");

            for (Element post : postElements) {
                Element linkElement = post.selectFirst("a.link_tech");
                Element titleElement = post.selectFirst("div.wrap_info > strong"); // 제목 요소
                Element summaryElement = post.selectFirst("div.info_tech");  // 📌 본문 요약이 포함된 요소 수정
                Element dateElement = post.selectFirst("span.date_info");

                if (linkElement == null || titleElement == null) continue;

                String link = linkElement.attr("href");
                String title = titleElement.text().trim();
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";
                String publishDateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                // 📌 날짜 변환 로직 수정
                LocalDate publishDate;
                try {
                    // '|' 기호로 날짜와 작성자명을 분리하여 날짜 부분만 사용
                    String[] dateParts = publishDateText.split(" \\| ");
                    String dateOnly = dateParts[0].trim(); // 첫 번째 요소만 날짜로 사용

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA);
                    publishDate = LocalDate.parse(dateOnly, formatter);
                } catch (DateTimeParseException e) {
                    System.err.println("❌ 날짜 변환 실패: " + publishDateText + " (기본값: 오늘 날짜 사용)");
                    publishDate = LocalDate.now();
                }

                articles.add(new ArticleDetails("kakao", title, link, content, new ArrayList<>(), publishDate));
                System.out.println("✔ 게시글 수집: " + title + " (" + publishDate + ")");
            }


        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    private List<ArticleDetails> fetchNaverd2Articles() {
        String baseUrl = "https://d2.naver.com";
        String Url = baseUrl + "/helloworld?page=";

        List<ArticleDetails> articles = new ArrayList<>();

        // ✅ WebDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // GUI 없이 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        WebDriver driver = new ChromeDriver(options);

        try {
            int page = 0;
            while (true) {
                String url = Url + page;
                driver.get(url);

                // ✅ WebDriverWait을 사용하여 페이지가 완전히 로드될 때까지 대기
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".post_article")));

                // ✅ 페이지의 HTML 가져오기
                Document doc = Jsoup.parse(driver.getPageSource());

                // ✅ 게시글 목록 선택
                Elements postElements = doc.select(".post_article");

                if (postElements.isEmpty()) {
                    System.out.println("페이지 " + page + " 에서 더 이상 게시글이 없음. 크롤링 종료.");
                    break;
                }

                for (Element element : postElements) {
                    // ✅ 제목과 링크 가져오기
                    Element titleElement = element.selectFirst("h2 a");
                    String title = titleElement.text();
                    String link = baseUrl + titleElement.attr("href");

                    // ✅ 컨텐츠(요약) 가져오기
                    Element contentElement = element.selectFirst(".post_txt");
                    String content = (contentElement != null) ? contentElement.text() : "";

                    // ✅ 등록일 가져오기 (String → LocalDate 변환)
                    Element dateElement = element.select("dl dt:has(i.xi-time-o) + dd").first();
                    LocalDate publishDate = null;

                    if (dateElement != null) {
                        String dateText = dateElement.text().trim();
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                            publishDate = LocalDate.parse(dateText, formatter);
                        } catch (Exception e) {
                            System.err.println("날짜 변환 오류: " + dateText);
                            e.printStackTrace();
                        }
                    }

                    // ✅ ArticleDetails 객체 생성 후 리스트에 추가
                    articles.add(new ArticleDetails("Naver D2", title, link, content, new ArrayList<>(), publishDate));
                }

                System.out.println("페이지 " + page + " 크롤링 완료, 현재까지 " + articles.size() + "개 수집됨.");
                page++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return articles;

    }

    private List<ArticleDetails> fetchTadaArticles() {
        String baseUrl = "https://blog-tech.tadatada.com";

        List<ArticleDetails> articles = new ArrayList<>();
        Set<String> visitedPages = new HashSet<>();

        // ✅ ChromeDriver 설정
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");  // UI 없이 실행
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        WebDriver driver = new ChromeDriver(options);

        try {
            String currentUrl = baseUrl;
            boolean hasNextPage = true;

            while (hasNextPage) {
                //  중복 방문 방지
                if (visitedPages.contains(currentUrl)) {
                    System.out.println("🔄 이미 방문한 페이지: " + currentUrl);
                    break;
                }
                visitedPages.add(currentUrl);

                //  페이지 이동 및 HTML 가져오기
                driver.get(currentUrl);
                Thread.sleep(2000); // 페이지 로딩 대기
                Document doc = Jsoup.parse(driver.getPageSource());

                //  게시글 크롤링
                Elements postElements = doc.select("ul.css-r8bdad li");

                for (Element post : postElements) {
                    // ✅ 제목 찾기
                    Element titleElement = post.selectFirst("p.css-o27vcl");
                    if (titleElement == null) continue;

                    String title = titleElement.text().trim(); // ✅ title 속성 대신 text() 사용

                    // ✅ 링크 찾기 (a 태그에서 href 가져오기)
                    Element linkElement = post.selectFirst("a");
                    String link = (linkElement != null) ? baseUrl + linkElement.attr("href") : "#";

                    // ✅ 요약 찾기 (정확한 클래스명 필요)
                    Element summaryElement = post.selectFirst("p.css-summary");
                    String content = (summaryElement != null) ? summaryElement.text().trim() : "요약없음";

                    // ✅ 날짜 찾기
                    Element dateElement = post.selectFirst("p.css-17gq9ws");
                    String dateText = (dateElement != null) ? dateElement.text().trim() : "날짜 없음";

                    // ✅ 날짜 변환 로직
                    LocalDate articleDate;
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
                        articleDate = LocalDate.parse(dateText, formatter);
                    } catch (DateTimeParseException e) {
                        System.err.println("❌ 날짜 변환 실패: " + dateText + " (기본값: 오늘 날짜 사용)");
                        articleDate = LocalDate.now(); // 변환 실패 시 오늘 날짜 사용
                    }

                    // ✅ 최종 데이터 저장
                    articles.add(new ArticleDetails("Tada", title, link, content, new ArrayList<>(), articleDate));
                    System.out.println("✔ 게시글 수집: " + title + " (" + articleDate + ")");
                }


                int currentPageNumber = extractTadaPageNumber(currentUrl);
                Element nextPageElement = null;
                Elements pageLinks = doc.select("nav ol li a[href^='/page/']");

                for (Element pageLink : pageLinks) {
                    int pageNumber = extractTadaPageNumber(baseUrl + pageLink.attr("href"));
                    if (pageNumber > currentPageNumber) {
                        nextPageElement = pageLink;
                        break;
                    }
                }

                if (nextPageElement != null) {
                    String nextPageUrl = baseUrl + nextPageElement.attr("href");

                    if (!visitedPages.contains(nextPageUrl)) {
                        currentUrl = nextPageUrl;
                        System.out.println("➡️ 다음 페이지 이동: " + currentUrl);
                    } else {
                        System.out.println("🔄 중복 페이지 감지: " + nextPageUrl + " (탐색 중지)");
                        hasNextPage = false;
                    }
                } else {
                    System.out.println(" 마지막 페이지 도달");
                    hasNextPage = false;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    private int extractTadaPageNumber(String url) {
        Pattern pattern = Pattern.compile(".*/page/(\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1; // 기본적으로 첫 페이지로 설정
    }

    private List<ArticleDetails> fetchDaangnArticles() {
        String baseUrl = "https://medium.com/daangn/development/home";
        List<ArticleDetails> articles = new ArrayList<>();
        WebDriverManager.chromedriver().setup();

        // ✅ 크롬 옵션 설정 (자동화 탐지 우회 + Headless 모드)
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
            // ✅ 당근 블로그 페이지 접속
            driver.get(baseUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // ✅ 모든 게시글을 로드할 때까지 스크롤 다운
            SeleniumService.loadAllMediumPosts(driver);

            // ✅ 게시글이 로드될 때까지 대기
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.col.u-xs-marginBottom10")));

            // ✅ 페이지 소스를 Jsoup으로 파싱
            Document doc = Jsoup.parse(driver.getPageSource());

            // ✅ 게시글 목록 크롤링
            Elements postElements = doc.select("div.col.u-xs-marginBottom10");

            for (Element post : postElements) {
                // ✅ 제목 크롤링 (h3 태그에서 wholeText() 사용 → 직접적인 제목만 가져오기)
                Element titleElement = post.selectFirst("h3");
                String title = (titleElement != null) ? titleElement.wholeText().trim() : "제목 없음";

                // ✅ 링크 크롤링 (a 태그에서 href 가져오기)
                Element linkElement = post.selectFirst("a[href]");
                String link = (linkElement != null) ? linkElement.absUrl("href") : "#";

                // ✅ 작성자 크롤링 (작성자 정보 가져오기)
                Element authorElement = post.selectFirst("div.postMetaInline-authorLockup a");
                String author = (authorElement != null) ? authorElement.text().trim() : "미상";

                // ✅ 날짜 크롤링 (ISO 8601 형식 `datetime` 속성 사용)
                Element dateElement = post.selectFirst("time");
                String dateStr = (dateElement != null) ? dateElement.attr("datetime") : "날짜 없음";

                // ✅ 날짜 변환 (ISO 8601 → LocalDate)
                LocalDate publishDate = null;
                try {
                    if (!dateStr.equals("날짜 없음")) {
                        Instant instant = Instant.parse(dateStr);
                        publishDate = instant.atZone(ZoneId.of("UTC")).toLocalDate();
                    }
                } catch (Exception e) {
                    System.err.println("❌ 날짜 변환 실패: " + dateStr);
                }

                // ✅ 요약 크롤링 (div.u-fontSize18 안의 모든 텍스트 가져오기)
                Element summaryElement = post.selectFirst("div.u-fontSize18, p");
                String content = (summaryElement != null) ? summaryElement.text().trim() : "요약 없음";

                // ✅ ArticleDetails 객체로 저장 (provider 값을 "당근"으로 설정)
                articles.add(new ArticleDetails("당근", title, link, content, new ArrayList<>(), publishDate));

                // ✅ 콘솔 출력
                System.out.printf("✔ 게시글 수집: %s (작성자: %s, 날짜: %s, URL: %s, 요약: %s)%n",
                        title, author, publishDate, link, content);
            }





        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
        } finally {
            driver.quit();
        }

        return articles;
    }

    /**
     * Medium 블로그 게시글이 모두 로드될 때까지 스크롤 다운
     */
    private static void loadAllMediumPosts(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        int lastHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            try {
                Thread.sleep(3000); // 3초 대기 (서버 부하 방지)
            } catch (InterruptedException ignored) {
            }

            int newHeight = ((Number) js.executeScript("return document.body.scrollHeight")).intValue();
            if (newHeight == lastHeight) {
                break; // 더 이상 로드할 게시글이 없음
            }
            lastHeight = newHeight;
        }
    }

    /**
     * Medium 블로그 날짜 변환
     */
    private static LocalDate parseMediumDate(String dateText) {
        if (dateText.equals("날짜 없음") || dateText.isEmpty()) {
            return LocalDate.now();
        }

        try {
            return Instant.parse(dateText).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            System.err.println("❌ 날짜 변환 실패: " + dateText);
            return LocalDate.now();

        }
    }

    }

