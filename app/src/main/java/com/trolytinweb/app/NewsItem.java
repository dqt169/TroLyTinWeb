package com.trolytinweb.app;

public class NewsItem {
  public final String topic,title,link,source,description,origin;
  public final long publishedAt;
  public String cluster="";

  public NewsItem(String topic,String title,String link,String source,String description,long publishedAt){
    this(topic,title,link,source,description,publishedAt,"Web");
  }

  public NewsItem(String topic,String title,String link,String source,String description,long publishedAt,String origin){
    this.topic=topic; this.title=title; this.link=link; this.source=source;
    this.description=description; this.publishedAt=publishedAt; this.origin=origin;
  }
}
