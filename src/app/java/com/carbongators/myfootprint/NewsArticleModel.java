package com.carbongators.myfootprint;

public class NewsArticleModel {
    String articleTitle;
    String newsSource;
    String link;
    int image;


    public NewsArticleModel(String articleTitle, String newsSource, int image, String link) {
        this.articleTitle = articleTitle;
        this.newsSource = newsSource;
        this.link = link;
        this.image = image;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public String getNewsSource() {
        return newsSource;
    }

    public int getImage() {
        return image;
    }

    public String getLink() { return link; }
}
