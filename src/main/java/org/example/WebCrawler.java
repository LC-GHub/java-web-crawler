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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler {
    private static final int MAX_DEPTH = 2;
    private final int THREAD_POOL_SIZE = 4;
    private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> visitedUrl = ConcurrentHashMap.newKeySet();
    private final List<String> keywords = Arrays.asList("Electric Vehicles", "Sustainable", "Renewable energy", "Elon Musk", "SUV", "Autopilot", "Tesla", "Model S", "Model 3", "Model X", "Gigafactory", "Battery", "Hat", "Logo", "Bins");

    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final String outputFilePath = "output.txt";

    public WebCrawler(String seedURL) {
        urlQueue.add(seedURL);
    }

    public void start() {
        List<Future<Void>> futures = new ArrayList<>();

        while(!urlQueue.isEmpty() || futures.stream().anyMatch(f -> !f.isDone())) {
            while (!urlQueue.isEmpty()) {
                String url = urlQueue.poll();
                if (url != null && !visitedUrl.contains(url)) {
                    visitedUrl.add(url);
                    futures.add(executorService.submit(() -> {
                        crawl(url);
                        return null;
                    }));
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
