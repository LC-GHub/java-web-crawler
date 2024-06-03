package org.example;

public class Main {
    public static void main(String[] args) {
        WebCrawler wc = new WebCrawler("https://www.imdb.com");
        wc.start();
    }
}