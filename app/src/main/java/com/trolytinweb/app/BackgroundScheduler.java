package com.trolytinweb.app;

import android.content.Context;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public final class BackgroundScheduler {
  private BackgroundScheduler(){}

  public static void schedule(Context c){
    Constraints constraints=new Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build();
    PeriodicWorkRequest req=new PeriodicWorkRequest.Builder(NewsWorker.class,30,TimeUnit.MINUTES)
      .setConstraints(constraints)
      .build();
    WorkManager.getInstance(c).enqueueUniquePeriodicWork(
      "trolytinweb-news-watch-v2",
      ExistingPeriodicWorkPolicy.UPDATE,
      req
    );
  }
}
