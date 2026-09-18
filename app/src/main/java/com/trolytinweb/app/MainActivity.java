package com.trolytinweb.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private static final String PREF="trolytinweb";
  private final ArrayList<String> topics=new ArrayList<>();
  private final ArrayList<NewsItem> items=new ArrayList<>();
  private ArrayAdapter<String> topicAdapter;
  private NewsAdapter newsAdapter;
  private Spinner range;
  private TextView status,summary;
  private ListView newsList;
  private final ExecutorService pool=Executors.newFixedThreadPool(3);
  private final Handler main=new Handler(Looper.getMainLooper());

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    loadTopics();
    setContentView(buildUi());
    if(!topics.isEmpty()) refresh();
  }

  private View buildUi(){
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(14,14,14,14);
    TextView title=new TextView(this); title.setText("Trợ lý Tin Web"); title.setTextSize(24); title.setTypeface(null,Typeface.BOLD); title.setTextColor(Color.rgb(31,95,74)); root.addView(title);

    EditText input=new EditText(this); input.setHint("Nhập chủ đề cần theo dõi...");
    Button add=new Button(this); add.setText("Thêm chủ đề");
    LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.addView(input,new LinearLayout.LayoutParams(0,-2,1)); row.addView(add); root.addView(row);

    Spinner topicSpinner=new Spinner(this);
    topicAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,topics);
    topicSpinner.setAdapter(topicAdapter); root.addView(topicSpinner);

    range=new Spinner(this);
    range.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"24 giờ","7 ngày","30 ngày"}));
    range.setSelection(1); root.addView(range);

    LinearLayout buttons=new LinearLayout(this);
    Button refresh=new Button(this); refresh.setText("Cập nhật");
    Button remove=new Button(this); remove.setText("Xóa chủ đề");
    buttons.addView(refresh,new LinearLayout.LayoutParams(0,-2,1)); buttons.addView(remove,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(buttons);

    status=new TextView(this); status.setText("Thêm chủ đề để bắt đầu."); root.addView(status);
    summary=new TextView(this); summary.setPadding(0,8,0,8); root.addView(summary);

    newsList=new ListView(this); newsAdapter=new NewsAdapter(this,items); newsList.setAdapter(newsAdapter);
    root.addView(newsList,new LinearLayout.LayoutParams(-1,0,1));

    add.setOnClickListener(v->{ String t=input.getText().toString().trim(); if(t.length()<2)return;
      if(!topics.contains(t)){topics.add(t); saveTopics(); topicAdapter.notifyDataSetChanged();}
      input.setText(""); refresh();
    });
    refresh.setOnClickListener(v->refresh());
    remove.setOnClickListener(v->{ if(topics.isEmpty())return; int p=topicSpinner.getSelectedItemPosition(); if(p>=0&&p<topics.size()){topics.remove(p);saveTopics();topicAdapter.notifyDataSetChanged();items.clear();newsAdapter.notifyDataSetChanged();} });
    newsList.setOnItemClickListener((p,v,pos,id)->startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(items.get(pos).link))));
    return root;
  }

  private void refresh(){
    if(topics.isEmpty()){status.setText("Chưa có chủ đề.");return;}
    if(!online()){status.setText("Không có kết nối Internet.");return;}
    status.setText("Đang cập nhật...");
    final ArrayList<String> current=new ArrayList<>(topics);
    final int rangePos=range.getSelectedItemPosition();
    pool.execute(()->{
      ArrayList<NewsItem> all=new ArrayList<>();
      for(String topic:current) try{all.addAll(fetch(topic,rangePos));}catch(Exception ignored){}
      LinkedHashMap<String,NewsItem> unique=new LinkedHashMap<>();
      all.sort((a,b)->Long.compare(b.publishedAt,a.publishedAt));
      for(NewsItem n:all) unique.putIfAbsent(n.title.toLowerCase(Locale.ROOT),n);
      ArrayList<NewsItem> result=new ArrayList<>(unique.values());
      main.post(()->{items.clear();items.addAll(result);newsAdapter.notifyDataSetChanged();
        status.setText("Đã cập nhật "+items.size()+" tin • "+new SimpleDateFormat("HH:mm dd/MM",new Locale("vi","VN")).format(new Date()));
        summary.setText(items.isEmpty()?"Chưa có kết quả phù hợp.":"Tổng hợp nhanh: "+Math.min(3,items.size())+" tin đầu là những kết quả mới nhất theo các chủ đề bạn đang theo dõi.");
      });
    });
  }

  private List<NewsItem> fetch(String topic,int rangePos)throws Exception{
    String when=rangePos==0?" when:1d":rangePos==2?" when:30d":" when:7d";
    String q=URLEncoder.encode(topic+when,StandardCharsets.UTF_8.name());
    URL u=new URL("https://news.google.com/rss/search?q="+q+"&hl=vi&gl=VN&ceid=VN:vi");
    HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(15000);
    c.setRequestProperty("User-Agent","Mozilla/5.0 Android TroLyTinWeb/1.0");
    try(InputStream in=c.getInputStream()){return FeedParser.parse(in,topic);}finally{c.disconnect();}
  }

  private boolean online(){
    ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    if(cm==null)return true; Network n=cm.getActiveNetwork(); if(n==null)return false;
    NetworkCapabilities cap=cm.getNetworkCapabilities(n); return cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  private void loadTopics(){
    Set<String> s=getSharedPreferences(PREF,MODE_PRIVATE).getStringSet("topics",new LinkedHashSet<>());
    topics.addAll(s);
  }
  private void saveTopics(){getSharedPreferences(PREF,MODE_PRIVATE).edit().putStringSet("topics",new LinkedHashSet<>(topics)).apply();}

  @Override protected void onDestroy(){pool.shutdownNow();super.onDestroy();}

  static class NewsAdapter extends BaseAdapter{
    final Context c; final List<NewsItem> data;
    NewsAdapter(Context c,List<NewsItem>d){this.c=c;this.data=d;}
    public int getCount(){return data.size();} public Object getItem(int p){return data.get(p);} public long getItemId(int p){return p;}
    public View getView(int p,View v,ViewGroup parent){
      LinearLayout box=new LinearLayout(c); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(12,12,12,12);
      NewsItem n=data.get(p);
      TextView t=new TextView(c); t.setText(n.title); t.setTextSize(16); t.setTypeface(null,Typeface.BOLD); box.addView(t);
      TextView m=new TextView(c); m.setText(n.topic+" • "+n.source); m.setTextSize(12); m.setTextColor(Color.GRAY); box.addView(m);
      if(!n.description.isEmpty()){TextView d=new TextView(c); d.setText(n.description); d.setMaxLines(3); box.addView(d);}
      return box;
    }
  }
}
