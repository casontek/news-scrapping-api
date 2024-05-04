package com.casontek.sportsnewsscrapper.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Headline {
    @Getter
    @Setter
    String heading;
    @Getter
    @Setter
    String pageLink;
    @Getter
    @Setter
    String imageUrl;
    @Getter
    @Setter
    String timestamp;
    @Getter
    @Setter
    String source;
    @Getter
    @Setter
    String tags;
    @Getter
    @Setter
    Boolean scrapped;
    @Getter
    @Setter
    String createdAt;
}
