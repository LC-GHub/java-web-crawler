package org.example;


import crawlercommons.robots.SimpleRobotRules;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler {
    private static final int MAX_DEPTH = 2;
    private final int THREAD_POOL_SIZE = 4;
    private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrl = ConcurrentHashMap.newKeySet();
    private final List<String> keywords = Arrays.asList("Electric Vehicles", "Sustainable", "Renewable energy", "Elon Musk", "SUV", "Autopilot", "Tesla", "Model S", "Model 3", "Model X", "Gigafactory", "Battery", "Hat", "Logo", "Bins");

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final String outputFilePath = "output1.txt";
    private final String robotsTxtPath = "/Users/lionelchew/Desktop/java-web-crawler/robots.txt";

    private final String seedURL;

    public WebCrawler(String seedURL) {
        this.seedURL = seedURL;
        urlQueue.add(seedURL);

        // Download Robots.txt on init
        try {
            URL robotsTextUrl = new URL(seedURL + "/robots.txt");
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
                } else {
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
            Document doc = Jsoup.connect(url).get();
            String pageText = doc.body().text();

            if (containsKeyword(pageText)) {
                saveToOutputFile(url, pageText);
                Elements linkTags = doc.select("a[href]");
                linkTags.forEach(link -> {
                    String href = link.attr("abs:href");
                    if (!visitedUrl.contains(href)) {
                        urlQueue.add(href);
                    }
                });
            }

        } catch (IOException e) {
            System.err.println("error processing URL: " + url + e.getMessage());
        }
    }

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
