package org.example;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class WebCrawler {
    private static final int MAX_DEPTH = 2;
    private final int THREAD_POOL_SIZE = 4;
    private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrl = ConcurrentHashMap.newKeySet();
    private final List<String> keywords = Arrays.asList("movies");

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final String outputFilePath = "output1.txt";
    private final String robotsTxtPath = "robots.txt";

    private final String seedURL;

    private static final int BASE_DELAY_MS = 10000;
    private static final int MAX_RETRIES = 5;

    public WebCrawler(String seedURL) {
        this.seedURL = seedURL;
        urlQueue.add(seedURL);
        // Download Robots.txt
        downloadRobotsTxt(seedURL);
    }

    private void downloadRobotsTxt(String URL){
        try {
            URL robotsTextUrl = new URL(URL + "/robots.txt");
            try (InputStream in = robotsTextUrl.openStream()) {
                Files.copy(in, Path.of(robotsTxtPath), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("robots.txt downloaded successfully.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    // Not-in-Use
    private WebDriver initWebDriver() {
        // Set the path to the chromedriver executable
        System.setProperty("webdriver.chrome.driver", "/Users/lionelchew/Desktop/java-web-crawler/chromedriver");
        // Create an instance of ChromeOptions
        ChromeOptions options = new ChromeOptions();
        // Add the necessary arguments
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("disable-blink-features=AutomationControlled");
        // Set the user-agent string
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.50 Safari/537.36";
        options.addArguments("user-agent=" + userAgent);

        // Initialize the WebDriver with the ChromeOptions
        return new ChromeDriver(options);
    }
    // Not-in-Use
    private void downloadRobotsTxt_sel(String seedURL) {
        System.out.println(seedURL + "/robots.txt");
        WebDriver driver = initWebDriver();

        // Navigate to a URL to test
        driver.get(seedURL+"/robots.txt");

        // Download
        String robotsTxtContent = driver.getPageSource();

        // Write the content to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(robotsTxtPath))) {
            writer.write(robotsTxtContent);
            System.out.println("robots.txt downloaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Close the driver
        driver.quit();
    }

    public void start() {
        List<Future<Void>> futures = new ArrayList<>();
        RobotRespector robotRespector = new RobotRespector(robotsTxtPath);

        List<String> agents = new ArrayList<>();
        agents.add("IR-crawler");

        while(!urlQueue.isEmpty() || futures.stream().anyMatch(f -> !f.isDone())) {
            while (!urlQueue.isEmpty()) {
                String url = urlQueue.poll();
                if (url != null && !visitedUrl.contains(url) && url.startsWith(seedURL) && robotRespector.isAllowed(agents, url)) {
                    System.out.println(url + " is ALLOWED");
                    visitedUrl.add(url);
                    futures.add(executorService.submit(() -> {
                        crawl(url);
                        return null;
                    }));
                } else if (url != null && !visitedUrl.contains(url) && url.startsWith(seedURL) && !robotRespector.isAllowed(agents, url)) {
                    System.out.println(url + " is DISALLOWED");
                }
            }
        }
        executorService.shutdown();

        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void crawl(String url) {
        try {
            Document doc = Jsoup.connect(url).userAgent("Opera").get();
            String pageText = doc.body().text();

            saveToOutputFile(url, pageText);
            Elements linkTags = doc.select("a[href]");
            linkTags.forEach(link -> {
                String href = link.attr("abs:href");
                if (!visitedUrl.contains(href)) {
                    urlQueue.add(href);
                }
            });

        } catch (IOException e) {
            System.err.println("error processing URL: " + url + e.getMessage());
        }
    }

    // Not-in-Use
     private String crawl_with_sel(String url) {
        WebDriver driver = initWebDriver();
        String pageSrc = null;

        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver driver) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    return js.executeScript("return document.readyState").toString().equals("complete");
                }
            });
            pageSrc = driver.getPageSource();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return pageSrc;
    }
    // Not-in-Use
    private boolean containsKeyword(String pageText) {
        return keywords.stream().anyMatch(pageText::contains);
    }

    private void saveToOutputFile(String url, String pageText) {
        try (FileWriter writer = new FileWriter(outputFilePath, true)) {
            writer.write("URL: " + url + "\n");
            writer.write("Page Text" + pageText + "\n\n");


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
