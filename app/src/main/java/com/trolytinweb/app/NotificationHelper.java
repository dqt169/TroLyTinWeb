package com.trolytinweb.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

public final class NotificationHelper {
  public static final String CHANNEL="new_news_v2";
  private NotificationHelper(){}

  public static void ensureChannel(Context c){
    if(Build.VERSION.SDK_INT>=26){
      NotificationChannel ch=new NotificationChannel(CHANNEL,"Tin mới theo chủ đề",NotificationManager.IMPORTANCE_DEFAULT);
      ch.setDescription("Thông báo khi phát hiện tin mới đúng chủ đề theo dõi");
      ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
  }

  public static void notifyNew(Context c,List<NewsItem> news){
    if(news==null||news.isEmpty())return;
    if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
    ensureChannel(c);

    Intent i=new Intent(c,MainActivity.class);
    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pi=PendingIntent.getActivity(c,20,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

    String title=news.size()==1?"Có tin mới: "+news.get(0).topic:"Có "+news.size()+" tin mới đúng chủ đề";
    StringBuilder text=new StringBuilder();
    for(int x=0;x<Math.min(4,news.size());x++){
      if(x>0)text.append("\n");
      text.append("• ").append(news.get(x).title);
    }

    Notification.Builder b=Build.VERSION.SDK_INT>=26
      ?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
    b.setSmallIcon(R.drawable.ic_launcher)
      .setContentTitle(title)
      .setContentText(news.get(0).title)
      .setStyle(new Notification.BigTextStyle().bigText(text.toString()))
      .setAutoCancel(true)
      .setContentIntent(pi);
    ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(2002,b.build());
  }
}
