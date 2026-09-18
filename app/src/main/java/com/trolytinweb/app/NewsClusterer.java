package com.trolytinweb.app;

import java.util.*;

public final class NewsClusterer {
  private static final Set<String> STOP=new HashSet<>(Arrays.asList(
    "của","và","cho","trong","với","một","những","được","tại","theo","khi","về","các",
    "the","and","for","with","from","this","that","news","mới","nhất","đang"
  ));
  private NewsClusterer(){}

  public static void assignGroups(List<NewsItem> items){
    ArrayList<Set<String>> reps=new ArrayList<>();
    int next=1;
    for(NewsItem n:items){
      Set<String> words=tokens(n.title);
      int best=-1; double score=0;
      for(int i=0;i<reps.size();i++){
        double s=jaccard(words,reps.get(i));
        if(s>score){score=s;best=i;}
      }
      if(best>=0 && score>=0.34){
        n.cluster="Nhóm "+(best+1);
        reps.get(best).addAll(words);
      }else{
        reps.add(new HashSet<>(words));
        n.cluster="Nhóm "+next++;
      }
    }
  }

  public static int countGroups(List<NewsItem> items){
    HashSet<String> g=new HashSet<>();
    for(NewsItem n:items) if(!n.cluster.isEmpty()) g.add(n.cluster);
    return g.size();
  }

  private static Set<String> tokens(String s){
    HashSet<String> out=new HashSet<>();
    String[] a=s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]"," ").split("\\s+");
    for(String x:a) if(x.length()>=3&&!STOP.contains(x)) out.add(x);
    return out;
  }

  private static double jaccard(Set<String>a,Set<String>b){
    if(a.isEmpty()||b.isEmpty())return 0;
    int inter=0; for(String x:a)if(b.contains(x))inter++;
    int union=a.size()+b.size()-inter;
    return union==0?0:(double)inter/union;
  }
}
