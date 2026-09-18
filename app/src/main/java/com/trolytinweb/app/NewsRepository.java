package com.trolytinweb.app;

import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NewsRepository {
  private NewsRepository(){}

  public static ArrayList<NewsItem> fetchAll(List<String> topics,int rangePos) throws Exception {
    ArrayList<NewsItem> all=new ArrayList<>();
    for(String topic:topics){
      try{all.addAll(fetchGoogle(topic,rangePos));}catch(Exception ignored){}
      try{all.addAll(fetchBing(topic,rangePos));}catch(Exception ignored){}
    }

    long cutoff=System.currentTimeMillis()-days(rangePos)*24L*60L*60L*1000L;
    LinkedHashMap<String,NewsItem> unique=new LinkedHashMap<>();
    all.sort((a,b)->Long.compare(b.publishedAt,a.publishedAt));
    for(NewsItem n:all){
      if(n.publishedAt>0 && n.publishedAt<cutoff) continue;
      String key=normalize(n.title);
      if(key.length()<8) key=n.link;
      unique.putIfAbsent(key,n);
    }
    ArrayList<NewsItem> result=new ArrayList<>(unique.values());
    NewsClusterer.assignGroups(result);
    return result;
  }

  private static List<NewsItem> fetchGoogle(String topic,int rangePos)throws Exception{
    String when=rangePos==0?" when:1d":rangePos==2?" when:30d":" when:7d";
    String q=URLEncoder.encode(topic+when,StandardCharsets.UTF_8.name());
    String url="https://news.google.com/rss/search?q="+q+"&hl=vi&gl=VN&ceid=VN:vi";
    return fetch(url,topic,"Google News");
  }

  private static List<NewsItem> fetchBing(String topic,int rangePos)throws Exception{
    String q=URLEncoder.encode(topic,StandardCharsets.UTF_8.name());
    String url="https://www.bing.com/news/search?q="+q+"&format=rss";
    return fetch(url,topic,"Bing News");
  }

  private static List<NewsItem> fetch(String raw,String topic,String origin)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(raw).openConnection();
    c.setConnectTimeout(12000); c.setReadTimeout(16000);
    c.setInstanceFollowRedirects(true);
    c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) TroLyTinWeb/2.0");
    c.setRequestProperty("Accept","application/rss+xml, application/xml, text/xml, */*");
    int code=c.getResponseCode();
    if(code<200||code>=300){c.disconnect();throw new Exception("HTTP "+code);}
    try(InputStream in=c.getInputStream()){
      return FeedParser.parse(in,topic,origin);
    }finally{c.disconnect();}
  }

  private static int days(int rangePos){return rangePos==0?1:rangePos==2?30:7;}

  private static String normalize(String s){
    return s.toLowerCase(Locale.ROOT)
      .replaceAll("[^\\p{L}\\p{N} ]"," ")
      .replaceAll("\\s+"," ").trim();
  }
}
