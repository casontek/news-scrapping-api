FROM maven:3.8.3-openjdk-17 AS build
COPY . .
RUN mvn clean install

FROM eclipse-temurin:17-jdk
COPY --from=build /target/sports-news-scrapper.jar sports-news-scrapper.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","sports-news-scrapper.jar"]