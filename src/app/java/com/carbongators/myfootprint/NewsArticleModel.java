package com.carbongators.myfootprint;

public class NewsArticleModel {
    String articleTitle;
    String newsSource;
    int image;


    public NewsArticleModel(String articleTitle, String newsSource, int image) {
        this.articleTitle = articleTitle;
        this.newsSource = newsSource;
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
}
