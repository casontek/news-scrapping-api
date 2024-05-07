FROM openjdk
COPY target/sports-news-scrapper.jar sports-news-scrapper.jar
ENTRYPOINT ["java", "-jar", "/sports-news-scrapper.jar"]