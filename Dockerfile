FROM openjdk:20
RUN mkdir /app
COPY out/artifacts/java_web_crawler_jar/java-web-crawler.jar /app
WORKDIR /app
ENTRYPOINT ["java","-jar", "java-web-crawler.jar"]