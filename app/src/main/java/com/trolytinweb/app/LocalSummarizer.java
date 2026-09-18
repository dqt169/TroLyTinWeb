package com.trolytinweb.app;

import java.util.*;

public final class LocalSummarizer {
  private LocalSummarizer(){}

  public static String summarize(List<NewsItem> items){
    if(items.isEmpty()) return "Chưa có kết quả phù hợp.";
    LinkedHashMap<String,Integer> sources=new LinkedHashMap<>();
    LinkedHashMap<String,Integer> topics=new LinkedHashMap<>();
    LinkedHashMap<String,Integer> groups=new LinkedHashMap<>();
    for(NewsItem n:items){
      sources.put(n.source,sources.getOrDefault(n.source,0)+1);
      topics.put(n.topic,topics.getOrDefault(n.topic,0)+1);
      groups.put(n.cluster,groups.getOrDefault(n.cluster,0)+1);
    }
    String topGroup="";
    int max=0;
    for(Map.Entry<String,Integer> e:groups.entrySet()) if(e.getValue()>max){max=e.getValue();topGroup=e.getKey();}
    String headline=items.get(0).title;
    return "Tổng hợp cục bộ: "+items.size()+" tin từ "+sources.size()+" nguồn, "
      +groups.size()+" nhóm nội dung. Tin mới nhất: “"+headline+"”. "
      +(max>1?topGroup+" có "+max+" bài liên quan. ":"")
      +"Nhấn “AI phân tích nhiều nguồn” để AI đọc và đối chiếu sâu hơn.";
  }
}
