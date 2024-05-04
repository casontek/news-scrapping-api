package com.casontek.sportsnewsscrapper.controller;

import com.casontek.sportsnewsscrapper.model.Headline;
import com.casontek.sportsnewsscrapper.model.Highlights;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.result.InsertOneResult;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@RestController
@RequestMapping("api/scraping/news")
public class ScrappingApi {
    String headlineCollection = "crawlednewsheaders";
    String highlights = "newshighlights";
    @GetMapping("bbc/headline")
    public String bbcHeadlines() {
        //connects to bbc site
        String title = "";
        try {
            String baseUrl = "https://www.bbc.com/sport/football";
            Document doc = Jsoup.connect(baseUrl).get();
            title = doc.title();
            //Elements newsItems = doc.select(".ssrcss-a9g9tg-Grid, .ssrcss-141t5cf-Grid, .ssrcss-1sx4aei-Grid");
            Elements newsItems = doc.select(".ssrcss-cmbgq-PromoSwitchLayoutAtBreakpoints, .ssrcss-1m6y3o5-PromoSwitchLayoutAtBreakpoints");
            List<Headline> headlinesList = new ArrayList<>();

            for (Element newsItem : newsItems) {
                Elements inner_elements = newsItem.children();
                for(Element element: inner_elements) {
                    try {
                        String headline = "";
                        String newsLink = "";
                        String imageLink = "";
                        String tag = "";
                        String postedTime = "";

                        try{
                            headline = element.select(".ssrcss-1nzemmm-PromoHeadline").text();
                        }
                        catch (Exception e){
                            System.out.println("================= Headline Error: " + e);
                        }

                        try {
                            newsLink = element.select(".ssrcss-zmz0hi-PromoLink").attr("href");
                        }
                        catch (Exception e) {
                            System.out.println("================= Page Link Error: " + e);
                        }

                        try{
                            imageLink = element.select(".ssrcss-evoj7m-Image").attr("src");
                        }
                        catch (Exception e) {
                            System.out.println("=================== Image Url Error: " + e);
                        }

                        try {
                            tag = element.select(".ssrcss-1if1g9v-MetadataText").text();
                        }
                        catch (Exception e) {
                            System.out.println("===================== Tag Error: " + e);
                        }

                        String formattedTag = "";
                        String formattedTime = "";

                        if(tag != null && !tag.isEmpty()) {
                            String unformattedTags[] = tag.split(" ");
                            StringBuilder builder = new StringBuilder();
                            for (String t: unformattedTags) {
                                if(StringUtils.isNumeric(t)) {
                                    break;
                                }
                                else {
                                    builder.append(" " + t);
                                }
                            }
                            formattedTag = builder.toString().trim();
                        }

                        try {
                            postedTime = element.select(".ssrcss-13nu8ri-GroupChildrenForWrapping").text();
                        }
                        catch (Exception e) {
                            System.out.println("==================== Posted Time Error: " + e);
                        }

                        if(postedTime != null && !postedTime.isEmpty()) {
                            String unformattedTime[] = postedTime.split("ago");
                            if(unformattedTime.length > 1){
                                formattedTime = unformattedTime[1];
                                if(formattedTime.contains("Comments")) {
                                    String[] timeWithoutComment = formattedTime.split(" ");
                                    formattedTime = timeWithoutComment[0];
                                }
                            }
                        }

                        Headline headlines = new Headline();
                        headlines.setHeading(headline);
                        headlines.setPageLink("https://www.bbc.com" + newsLink);
                        headlines.setTags(formattedTag);
                        headlines.setSource("BBC Sports");
                        headlines.setImageUrl(imageLink);
                        headlines.setScrapped(false);
                        headlines.setTimestamp(formattedTime);

                        if((!headlines.getPageLink().isEmpty() && headlines.getPageLink().length() > 10)  &&
                                !headlines.getTimestamp().isEmpty()) {
                            if(formattedTime.contains("h")){
                                try {
                                    long hoursPassed = Long.parseLong(formattedTime.replace("h",""));
                                    LocalDateTime date = LocalDateTime.now();
                                    date = date.minusHours(hoursPassed);

                                    String month;
                                    String day;
                                    String hour;
                                    String minutes;

                                    if(date.getMonthValue() < 10) {
                                        month = "0" + date.getMonthValue();
                                    }
                                    else  {
                                        month = date.getMonthValue() + "";
                                    }

                                    if(date.getDayOfMonth() < 10) {
                                        day = "0" + date.getDayOfMonth();
                                    }
                                    else {
                                        day = date.getDayOfMonth() + "";
                                    }

                                    if(date.getHour() < 10) {
                                        hour = "0" + date.getHour();;
                                    }
                                    else {
                                        hour = date.getHour() + "";
                                    }

                                    if(date.getMinute() < 10) {
                                        minutes = "0" + date.getMinute();
                                    }
                                    else {
                                        minutes = date.getMinute() + "";
                                    }

                                    int year = date.getYear();
                                    formattedTime = day + "/" + month + "/" + year + " " + hour + ":" + minutes;
                                }
                                catch (Exception ignored) {}

                                headlines.setTimestamp(formattedTime);
                                headlinesList.add(headlines);
                            }
                        }
                    }
                    catch (Exception e) {
                        System.out.println("@@@@@@@@@@@@@@@@@@@@@ Error: " + e);
                    }
                }
            }

            System.out.println("Headlines: " + headlinesList.size());
            //performs database operation
            MongoDatabase database = mongoDatabase();
            MongoCollection<org.bson.Document> collection = collection(database, headlineCollection);
            for(Headline h : headlinesList) {
                if(!doesHeadlineExist(collection, h.getHeading())){
                    saveHeadline(collection, h);
                }
                else {
                    System.out.println("####### Headline already exist.");
                }
            }
        }
        catch (IOException e) {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@ Error: " + e);
            e.printStackTrace();
        }

        return title;
    }

    @GetMapping("bbc/detail")
    public String bbcHighlights() {
        //loads available headlines
        MongoDatabase db = mongoDatabase();
        MongoCollection<org.bson.Document> c = collection(db, headlineCollection);
        List<org.bson.Document> headlines = unScrappedHeadlines(c, "BBC Sports");

        MongoCollection<org.bson.Document> highlightsCollection = collection(db, highlights);
        for(org.bson.Document document : headlines) {
            crawlBBCHighlights(document, highlightsCollection);
            try {
                //updates the collection
                Object id = document.get("_id");
                c.findOneAndUpdate(eq("_id", id), set("scrapped", true));
            }
            catch (Exception e) {
                System.out.println("************* collection update failed.");
            }
        }

        return "BBC News Crawling completed.";
    }

    @GetMapping("skySport/headline")
    public String skySportsHeadlines() {
        //connects to bbc site
        String title = "";
        try {
            String baseUrl = "https://www.skysports.com/football/news";
            Document doc = Jsoup.connect(baseUrl).get();
            title = doc.title();

            Elements newsItems = doc.select(".sdc-site-tiles__item");
            List<Headline> headlinesList = new ArrayList<>();

            for(Element element : newsItems) {
                String newsLink = "";
                String headline = "";
                String imageUrl = "";
                String tag = "";

                try {
                    newsLink = element.select(".sdc-site-tile__headline-link").attr("href");
                }
                catch (Exception e) {}

                try {
                    headline = element.select(".sdc-site-tile__headline-text").text();
                }
                catch (Exception e) {}

                try {
                    imageUrl = element.select(".sdc-site-tile__image").attr("src");
                }
                catch (Exception e){}

                try {
                    tag = element.select(".sdc-site-tile__tag").text();
                }
                catch (Exception e){}

                if(!newsLink.isEmpty() && !imageUrl.isEmpty()){
                    Headline headlines = new Headline();
                    headlines.setTags(tag);
                    headlines.setSource("SkySports");
                    headlines.setImageUrl(imageUrl);
                    headlines.setScrapped(false);
                    headlines.setPageLink("https://www.skysports.com" + newsLink);
                    headlines.setHeading(headline);
                    headlinesList.add(headlines);
                }

                System.out.println();
                System.out.println("Title: " + headline);
                System.out.println("Link: " + newsLink);
                System.out.println("Image Url: " + imageUrl);
                System.out.println("Tag: " + tag);
                System.out.println();
            }

            /*
            //performs database operation
            MongoCollection<org.bson.Document> collection = collection(mongoDatabase(), headlineCollection);
            for(Headlines headlines : headlinesList) {
                if(!doesHeadlineExist(collection, headlines.getHeading())){
                    saveHeadline(collection, headlines);
                }
                else {
                    System.out.println("####### Headline already exist.");
                }
            }
             */

        }
        catch (IOException e) {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@ Error: " + e);
        }
        return title;
    }

    @GetMapping("skySport/detail")
    public String skySportsHighlights() {
        //loads available headlines
        MongoDatabase database = mongoDatabase();
        MongoCollection<org.bson.Document> tCollection = collection(database, headlineCollection);
        List<org.bson.Document> headlines = unScrappedHeadlines(tCollection, "SkySports");

        MongoCollection<org.bson.Document> hCollection = collection(database, highlights);
        for(org.bson.Document document : headlines) {
            crawlSkySportsHighlights(document, hCollection);
            try {
                //updates the collection
                Object id = document.get("_id");
                tCollection.findOneAndUpdate(eq("_id", id), set("scrapped", true));
            }
            catch (Exception e) {
                System.out.println("************* collection update failed.");
            }
        }

        return "Sky SPorts News Crawling completed.";
    }

    int  getMonthValue(String month) {
        switch (month) {
            case "January" -> {
                return 1;
            }
            case "February" -> {
                return 2;
            }
            case "March" -> {
                return 3;
            }
            case "April" -> {
                return 4;
            }
            case "May" -> {
                return 5;
            }
            case "June" -> {
                return 6;
            }
            case "July" -> {
                return 7;
            }
            case "August" -> {
                return 8;
            }
            case "September" -> {
                return 9;
            }
            case "October" -> {
                return 10;
            }
            case "November" -> {
                return 11;
            }
            case "December" -> {
                return 12;
            }
            default -> {
                LocalDate date = LocalDate.now();
                return date.getMonthValue() + 1;
            }
        }
    }

    MongoDatabase mongoDatabase() {
        String connectionString = "mongodb+srv://Pope:08060158579@varitex-sports.xfko2tl.mongodb.net/varitex-sports-db?retryWrites=true&w=majority";

        MongoClient mongoClient = MongoClients.create(connectionString);
        return  mongoClient.getDatabase("varitex-sports-db");
    }

    MongoCollection<org.bson.Document> collection(MongoDatabase database, String collection) {
        return database.getCollection(collection);
    }

    List<org.bson.Document> unScrappedHeadlines(MongoCollection<org.bson.Document> collection, String source) {
        List<org.bson.Document> documentList = new ArrayList<>();
        MongoCursor<org.bson.Document> cursor =
                collection.find(and(
                        eq("scrapped", false),
                        eq("source", source)
                )).iterator();

        try {
            while (cursor.hasNext()) {
                documentList.add(cursor.next());
            }
        }
        finally {
            cursor.close();
        }

        return documentList;
    }

    Boolean doesHeadlineExist(MongoCollection<org.bson.Document> collection, String title) {
        org.bson.Document doc = collection.find(eq("heading", title)).first();
        if(doc != null) {
            return  true;
        }
        else {
            return false;
        }
    }

    void saveHeadline(MongoCollection<org.bson.Document> collection, Headline headlines) {
        try {
            InsertOneResult result = collection.insertOne(new org.bson.Document()
                    .append("heading", headlines.getHeading())
                    .append("snipets", "")
                    .append("pageLink", headlines.getPageLink())
                    .append("imageUrl", headlines.getImageUrl())
                    .append("timestamp", headlines.getTimestamp())
                    .append("source", headlines.getSource())
                    .append("tags", headlines.getTags())
                    .append("createdDate", getFormattedTodayDate())
                    .append("newsDate", getFormattedTodayDate())
                    .append("scrapped", false));

            System.out.println("Success! Inserted document id: " + result.getInsertedId());
        }
        catch (MongoException e) {
            System.err.println("Unable to insert due to an error: " + e);
        }
    }

    void saveNewsHighlights(MongoCollection<org.bson.Document> collection, Highlights highlights) {
        try {
            InsertOneResult result = collection.insertOne(new org.bson.Document()
                    .append("title", highlights.getTitle())
                    .append("snipets", highlights.getSnipets())
                    .append("subTitle", highlights.getSubTitle())
                    .append("timestamp", highlights.getTimestamp())
                    .append("tags", highlights.getTags())
                    .append("imageUrls", highlights.getImageUrls())
                    .append("highlights", highlights.getHighlights())
                    .append("source", highlights.getSource())
                    .append("imageUrl", highlights.getImageUrl())
                    .append("createdDate", getFormattedTodayDate())
                    .append("newsDate", getFormattedTodayDate())
                    .append("highlightsTwo", highlights.getHighlightsTwo())
            );

            System.out.println("Success! Inserted document id: " + result.getInsertedId());
        }
        catch (MongoException e) {
            System.err.println("Unable to insert due to an error: " + e);
        }
    }

    void crawlBBCHighlights(
            org.bson.Document document,
            MongoCollection<org.bson.Document> highlightsCollection
    ) {
        try {
            String postedDate = document.getString("timestamp");
            String tags = document.getString("tags");

            String url = document.getString("pageLink");
            System.out.println("========= Url: " + url);
            Document doc = Jsoup.connect(url).get();

            doc.removeClass(".trc_related_container");
            doc.removeClass(".comments");
            doc.removeClass(".story-body__media");
            // Select all <ul> elements
            Elements ulElements = doc.select("ul");
            // Remove each <ul> element
            for (Element ul : ulElements) {
                ul.remove();
            }

            String headline = "";
            String author = "";
            String source = "";
            String imageUrl = "";
            String imageUrls = "";
            String body1 = "";
            String body2 = "";
            List<String> bodyText = new ArrayList<>();


            try {
                Element headlineElement = doc.selectFirst(".qa-story-headline");
                headline = headlineElement.text();
            }
            catch (Exception e) {
                System.out.println("#################### headline error: " + e);
            }

            try {
                Element authorElement = doc.selectFirst(".qa-contributor-name");
                author = authorElement.text();
            }
            catch (Exception e) {
                System.out.println("#################### Author error: " + e);
            }

            try {
                Element sourceElement = doc.selectFirst(".qa-contributor-title");
                source = sourceElement.text();
            }
            catch (Exception e) {
                source = "BBC Sports";
                System.out.println("#################### Source error: " + e);
            }

            try {
                Element imgElement = doc.selectFirst(".qa-srcset-image");
                imageUrl = imgElement.attr("src");
                imageUrls = imgElement.attr("srcset");
            }
            catch (Exception e) {
                System.out.println("#################### Source error: " + e);
            }

            //remove the previous elements from the body
            try {
                doc.getElementsByTag("header").remove();
                doc.getElementsByTag("figure").remove();
                doc.getElementsByTag("img").remove();
            }
            catch (Exception ignore){}

            try {
                Elements bodyElements = doc.getElementsByTag("p");
                for(Element element: bodyElements) {
                    try {
                        String paragraph = element.removeAttr("data-reactid").toString();
                        bodyText.add(paragraph);
                    }
                    catch (Exception ignored){}
                }
            }
            catch (Exception e) {
                System.out.println("################ Paragraph error: " + e);
            }

            /*
            List<String> imageUrlList = new ArrayList<>();
            try {
                Elements elements = doc.select(".qa-srcset-image");
                for(Element element : elements) {
                    String url = element.attr("src");
                    imageUrlList.add(url);
                }
            }
            catch (Exception e) {
                System.out.println("#################### Image List error: " + e);
            }
             */

            try {
                if (bodyText.size() > 4) {
                    int middle = bodyText.size() / 2;
                    if (bodyText.size() % 2 == 1) {
                        middle += 1;
                    }
                    body1 = formatNewsParagraphBody(bodyText, 0, middle);
                    body2 = formatNewsParagraphBody(bodyText, middle, bodyText.size());
                }
                else {
                    body1 = formatNewsParagraphBody(bodyText, 0, bodyText.size());
                }
            }
            catch (Exception e) {
                System.out.println("############# Body Text Error: " + e);
            }


            if(imageUrl.length() > 0 && headline.length() > 0
                    && bodyText.size() > 0) {
                Highlights highlights = new Highlights();
                highlights.setSource(source);
                highlights.setTags(tags);
                highlights.setImageUrl(imageUrl);
                highlights.setImageUrls(imageUrls);
                highlights.setTimestamp(postedDate);
                highlights.setTitle(headline);
                highlights.setHighlights(body1);
                highlights.setHighlightsTwo(body2);

                //save highlight
                saveNewsHighlights(highlightsCollection, highlights);


            }
        }
        catch (Exception e) {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@ Error: " + e);
            e.printStackTrace();
        }
    }

    void  crawlSkySportsHighlights(
            org.bson.Document document,
            MongoCollection<org.bson.Document> highlightsCollection
    ) {
        try {
            String tags = document.getString("tags");
            String url = document.getString("pageLink");
            System.out.println("========= Url: " + url);

            Document doc = Jsoup.connect(url).get();

            String headline = "";
            String author = "";
            String dateTime = "";
            String imageUrl = "";
            String imageUrls = "";
            String formattedDate = "";
            String part1 = "";
            String part2 = "";
            List<String> bodyText = new ArrayList<>();

            try {
                Element headlineElement = doc.selectFirst(".sdc-article-header__long-title");
                headline = headlineElement.text();
            }
            catch (Exception e) {
                System.out.println("#################### headline error: " + e);
            }

            try {
                Element authorElement = doc.selectFirst(".sdc-article-author__name");
                author = authorElement.text();
            }
            catch (Exception e) {
                System.out.println("#################### Author error: " + e);
            }

            try {
                Element sourceElement = doc.selectFirst(".sdc-article-date__date-time");
                dateTime = sourceElement.text();
            }
            catch (Exception e) {
                System.out.println("#################### Date Time error: " + e);
            }

            try {
                Element imgElement = doc.selectFirst(".sdc-article-image__item");
                imageUrl = imgElement.attr("src");
                imageUrls = imgElement.attr("srcset");
            }
            catch (Exception e) {
                System.out.println("#################### Source error: " + e);
            }

            try {
                doc.getElementsByTag("table").remove();
                doc.getElementsByTag("figure").remove();
                doc.getElementsByTag("ul").remove();
                doc.getElementsByTag("header").remove();
                doc.getElementsByClass("gs-u-display-block story-body__media gs-u-mb-alt+ qa-story-body-media").remove();
                doc.getElementsByClass("view-comments").remove();
                doc.getElementsByClass("bbccom_advert").remove();
                doc.getElementsByClass("qa-introduction gel-pica-bold").remove();
                doc.getElementsByClass("sdc-site-localnav__header").remove();
                doc.getElementsByClass("sdc-article-author__role").remove();
                doc.getElementsByClass("sdc-article-date__date-time").remove();
                doc.getElementsByClass("sdc-site-video__accessibility-message").remove();
                doc.getElementsByAttributeValue("data-role", "bridge-message-text").remove();
            }
            catch (Exception e) {}

            try {
                Elements bodyElements = doc.getElementsByTag("p");//sdc-article-body
                for(Element element: bodyElements) {
                    try {
                        String paragraph = element.toString();
                        bodyText.add(paragraph);
                    }
                    catch (Exception ignored){}
                }
            }
            catch (Exception e) {
                System.out.println("################ Paragraph error: " + e);
            }

            //format sky sports news time i.e. Saturday 30 March 2024 11:24, UK
            try {
                String[] arrayDateObject = dateTime.split(" ");
                int month = getMonthValue(arrayDateObject[2]);
                String year = arrayDateObject[3];
                String day = arrayDateObject[1];
                String time = arrayDateObject[4];
                formattedDate = day + "/" + month + "/" + year + " " + time;
            }
            catch (Exception e){}

            try {
                if (bodyText.size() > 4) {
                    int middle = bodyText.size() / 2;
                    if (bodyText.size() % 2 == 1) {
                        middle += 1;
                    }
                    part1 = formatNewsParagraphBody(bodyText, 0, middle);
                    part2 = formatNewsParagraphBody(bodyText, middle, bodyText.size());
                }
                else {
                    part1 = formatNewsParagraphBody(bodyText, 0, bodyText.size());
                }
            }
            catch (Exception e) {
                System.out.println("############# Body Text Error: " + e);
            }

            if(imageUrl.length() > 0 && headline.length() > 0
                    && bodyText.size() > 0) {
                Highlights highlights = new Highlights();
                highlights.setSource("Sky SPorts");
                highlights.setTags(tags);
                highlights.setImageUrl(imageUrl);
                highlights.setImageUrls(imageUrls);
                highlights.setTimestamp(formattedDate);
                highlights.setTitle(headline);
                highlights.setHighlights(part1);
                highlights.setHighlightsTwo(part2);

                //save highlight
                saveNewsHighlights(highlightsCollection, highlights);


            }

        }
        catch (Exception e) {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@ Error: " + e);
            e.printStackTrace();
        }
    }

    String getFormattedTodayDate() {
        LocalDate today = LocalDate.now();
        String todayDate = today.getDayOfMonth() + "/" + today.getMonthValue() + "/" + today.getYear();
        return todayDate;
    }

    String formatNewsParagraphBody(List<String> items, int start, int end) {
        StringBuilder builder = new StringBuilder();
        for(String item : items.subList(start, end)) {
            builder.append(item);
        }

        return builder.toString();
    }

    @GetMapping("test/{id}")
    public String testApi(@PathVariable("id") int id) {
        List<String> names = Arrays.asList("Chika", "Kingsley", "Agbo","Pope");
        int middle = names.size() / 2;
        if (names.size()%2 == 1) {
            middle += 1;
        }

        if(id == 1) {
            return formatNewsParagraphBody(names, 0 , middle);
        }
        else if(id == 2) {
            return formatNewsParagraphBody(names, middle, names.size());
        }
        else {
            return formatNewsParagraphBody(names, 0, names.size());
        }
    }
}