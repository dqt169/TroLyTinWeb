package com.trolytinweb.app;

import android.content.*;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.*;

public class NewsWorker extends Worker {
  public NewsWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}

  @NonNull @Override public Result doWork(){
    try{
      Context c=getApplicationContext();
      SharedPreferences p=c.getSharedPreferences("trolytinweb",Context.MODE_PRIVATE);
      Set<String> topicSet=p.getStringSet("topics",Collections.emptySet());
      if(topicSet==null||topicSet.isEmpty())return Result.success();

      ArrayList<NewsItem> news=NewsRepository.fetchAll(new ArrayList<>(topicSet),0);
      Set<String> oldSet=p.getStringSet("seen_links_v2",Collections.emptySet());
      LinkedHashSet<String> old=new LinkedHashSet<>(oldSet==null?Collections.emptySet():oldSet);

      if(old.isEmpty()){
        saveSeen(p,news,old);
        return Result.success();
      }

      ArrayList<NewsItem> fresh=new ArrayList<>();
      for(NewsItem n:news)if(!old.contains(n.link))fresh.add(n);
      saveSeen(p,news,old);
      if(!fresh.isEmpty())NotificationHelper.notifyNew(c,fresh.subList(0,Math.min(8,fresh.size())));
      return Result.success();
    }catch(Exception e){
      return Result.retry();
    }
  }

  private static void saveSeen(SharedPreferences p,List<NewsItem> current,Set<String> old){
    LinkedHashSet<String> all=new LinkedHashSet<>();
    for(NewsItem n:current){all.add(n.link);if(all.size()>=600)break;}
    if(all.size()<600)for(String s:old){all.add(s);if(all.size()>=600)break;}
    p.edit().putStringSet("seen_links_v2",all).apply();
  }
}
