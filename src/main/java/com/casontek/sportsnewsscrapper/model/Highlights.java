package com.casontek.sportsnewsscrapper.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Highlights {
    @Getter
    @Setter
    String title;
    @Getter
    @Setter
    String snipets;
    @Getter
    @Setter
    String subTitle;
    @Getter
    @Setter
    String timestamp;
    @Getter
    @Setter
    String tags;
    @Getter
    @Setter
    String imageUrls;
    @Getter
    @Setter
    String highlights;
    @Getter
    @Setter
    String source;
    @Getter
    @Setter
    String imageUrl;
    @Getter
    @Setter
    String highlightsTwo;
    @Getter
    @Setter
    String createdAt;
}
