package com.trolytinweb.app;

public class NewsItem {
  public final String topic,title,link,source,description;
  public final long publishedAt;
  public NewsItem(String topic,String title,String link,String source,String description,long publishedAt){
    this.topic=topic; this.title=title; this.link=link; this.source=source;
    this.description=description; this.publishedAt=publishedAt;
  }
}
