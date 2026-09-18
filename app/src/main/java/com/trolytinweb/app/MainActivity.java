package com.trolytinweb.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private static final String PREF="trolytinweb";
  private final ArrayList<String> topics=new ArrayList<>();
  private final ArrayList<NewsItem> items=new ArrayList<>();
  private ArrayAdapter<String> topicAdapter;
  private NewsAdapter newsAdapter;
  private Spinner range,topicSpinner;
  private EditText apiKeyInput;
  private TextView status,summary,aiStatus;
  private final ExecutorService pool=Executors.newFixedThreadPool(3);
  private final Handler main=new Handler(Looper.getMainLooper());

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    NotificationHelper.ensureChannel(this);
    requestNotificationPermission();
    loadTopics();
    setContentView(buildUi());
    BackgroundScheduler.schedule(this);
    if(!topics.isEmpty())refresh();
  }

  private View buildUi(){
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(12),dp(10),dp(12),dp(8));

    TextView title=new TextView(this);
    title.setText("Trợ lý Tin Web V2");
    title.setTextSize(23);
    title.setTypeface(null,Typeface.BOLD);
    title.setTextColor(Color.rgb(31,95,74));
    root.addView(title);

    TextView sub=new TextView(this);
    sub.setText("Nhiều nguồn • Gom nhóm • AI tóm tắt • Báo tin mới nền");
    sub.setTextSize(12);
    sub.setTextColor(Color.DKGRAY);
    root.addView(sub);

    EditText input=new EditText(this);
    input.setHint("Nhập chủ đề cần theo dõi...");
    input.setSingleLine(true);
    Button add=new Button(this); add.setText("Thêm");
    LinearLayout row=new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.addView(input,new LinearLayout.LayoutParams(0,-2,1));
    row.addView(add);
    root.addView(row);

    topicSpinner=new Spinner(this);
    topicAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,topics);
    topicSpinner.setAdapter(topicAdapter);
    root.addView(topicSpinner);

    range=new Spinner(this);
    range.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
      new String[]{"24 giờ","7 ngày","30 ngày"}));
    range.setSelection(1);
    root.addView(range);

    LinearLayout buttons=new LinearLayout(this);
    Button refresh=new Button(this); refresh.setText("Cập nhật tin");
    Button remove=new Button(this); remove.setText("Xóa chủ đề");
    buttons.addView(refresh,new LinearLayout.LayoutParams(0,-2,1));
    buttons.addView(remove,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(buttons);

    TextView aiTitle=new TextView(this);
    aiTitle.setText("AI tổng hợp");
    aiTitle.setTypeface(null,Typeface.BOLD);
    aiTitle.setPadding(0,dp(6),0,0);
    root.addView(aiTitle);

    apiKeyInput=new EditText(this);
    apiKeyInput.setSingleLine(true);
    apiKeyInput.setHint(hasApiKey()?"Đã lưu API key • nhập khóa mới để thay":"Nhập OpenAI API key...");
    apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);

    Button saveKey=new Button(this); saveKey.setText("Lưu khóa");
    LinearLayout keyRow=new LinearLayout(this);
    keyRow.setOrientation(LinearLayout.HORIZONTAL);
    keyRow.addView(apiKeyInput,new LinearLayout.LayoutParams(0,-2,1));
    keyRow.addView(saveKey);
    root.addView(keyRow);

    Button ai=new Button(this);
    ai.setText("AI phân tích nhiều nguồn");
    root.addView(ai);

    aiStatus=new TextView(this);
    aiStatus.setText(hasApiKey()?"AI: sẵn sàng • GPT-5.6 Luna":"AI: chưa có API key • vẫn có tóm tắt cục bộ");
    aiStatus.setTextSize(12);
    root.addView(aiStatus);

    status=new TextView(this);
    status.setText("Kiểm tra nền khoảng mỗi 30 phút khi có mạng.");
    status.setPadding(0,dp(4),0,0);
    root.addView(status);

    summary=new TextView(this);
    summary.setPadding(0,dp(8),0,dp(8));
    summary.setTextIsSelectable(true);
    root.addView(summary);

    ListView newsList=new ListView(this);
    newsAdapter=new NewsAdapter(this,items);
    newsList.setAdapter(newsAdapter);
    root.addView(newsList,new LinearLayout.LayoutParams(-1,0,1));

    add.setOnClickListener(v->{
      String t=input.getText().toString().trim();
      if(t.length()<2){toast("Hãy nhập chủ đề rõ hơn.");return;}
      if(!topics.contains(t)){
        topics.add(t);
        saveTopics();
        topicAdapter.notifyDataSetChanged();
        BackgroundScheduler.schedule(this);
      }
      input.setText("");
      refresh();
    });

    refresh.setOnClickListener(v->refresh());

    remove.setOnClickListener(v->{
      if(topics.isEmpty())return;
      int p=topicSpinner.getSelectedItemPosition();
      if(p>=0&&p<topics.size()){
        topics.remove(p);
        saveTopics();
        topicAdapter.notifyDataSetChanged();
        items.clear();
        newsAdapter.notifyDataSetChanged();
        summary.setText("");
        BackgroundScheduler.schedule(this);
      }
    });

    saveKey.setOnClickListener(v->{
      String key=apiKeyInput.getText().toString().trim();
      if(key.isEmpty()){toast("Nhập API key trước khi lưu.");return;}
      getSharedPreferences(PREF,MODE_PRIVATE).edit().putString("openai_key",key).apply();
      apiKeyInput.setText("");
      apiKeyInput.setHint("Đã lưu API key • nhập khóa mới để thay");
      aiStatus.setText("AI: sẵn sàng • GPT-5.6 Luna");
      toast("Đã lưu khóa AI trên thiết bị.");
    });

    ai.setOnClickListener(v->runAiAnalysis());

    newsList.setOnItemClickListener((p,v,pos,id)->{
      try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(items.get(pos).link)));}
      catch(Exception e){toast("Không mở được liên kết.");}
    });

    return root;
  }

  private void refresh(){
    if(topics.isEmpty()){status.setText("Chưa có chủ đề theo dõi.");return;}
    if(!online()){status.setText("Không có kết nối Internet.");return;}
    status.setText("Đang lấy tin từ nhiều nguồn...");
    final ArrayList<String> current=new ArrayList<>(topics);
    final int rangePos=range.getSelectedItemPosition();

    pool.execute(()->{
      try{
        ArrayList<NewsItem> result=NewsRepository.fetchAll(current,rangePos);
        main.post(()->{
          items.clear();
          items.addAll(result);
          newsAdapter.notifyDataSetChanged();
          markSeenCurrent();
          status.setText("Đã cập nhật "+items.size()+" tin • "
            +NewsClusterer.countGroups(items)+" nhóm • "
            +new SimpleDateFormat("HH:mm dd/MM",new Locale("vi","VN")).format(new Date()));
          summary.setText(LocalSummarizer.summarize(items));
        });
      }catch(Exception e){
        main.post(()->status.setText("Lỗi cập nhật: "+shortMessage(e)));
      }
    });
  }

  private void runAiAnalysis(){
    if(items.isEmpty()){toast("Hãy cập nhật tin trước.");return;}
    String key=getSharedPreferences(PREF,MODE_PRIVATE).getString("openai_key","");
    if(key==null||key.trim().isEmpty()){
      toast("Nhập OpenAI API key rồi nhấn Lưu khóa.");
      return;
    }
    aiStatus.setText("AI đang đọc và đối chiếu nhiều nguồn...");
    summary.setText("Đang phân tích...");
    ArrayList<NewsItem> snapshot=new ArrayList<>(items);

    pool.execute(()->{
      try{
        String text=OpenAiClient.summarize(key,snapshot);
        main.post(()->{
          summary.setText(text);
          aiStatus.setText("AI: đã phân tích "+Math.min(24,snapshot.size())+" kết quả • GPT-5.6 Luna");
        });
      }catch(Exception e){
        main.post(()->{
          summary.setText(LocalSummarizer.summarize(items));
          aiStatus.setText("AI lỗi: "+shortMessage(e));
        });
      }
    });
  }

  private void requestNotificationPermission(){
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},901);
    }
  }

  private boolean online(){
    ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    if(cm==null)return true;
    Network n=cm.getActiveNetwork();
    if(n==null)return false;
    NetworkCapabilities cap=cm.getNetworkCapabilities(n);
    return cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }

  private void loadTopics(){
    Set<String> s=getSharedPreferences(PREF,MODE_PRIVATE).getStringSet("topics",Collections.emptySet());
    if(s!=null)topics.addAll(new LinkedHashSet<>(s));
  }

  private void saveTopics(){
    getSharedPreferences(PREF,MODE_PRIVATE).edit()
      .putStringSet("topics",new LinkedHashSet<>(topics)).apply();
  }

  private boolean hasApiKey(){
    String k=getSharedPreferences(PREF,MODE_PRIVATE).getString("openai_key","");
    return k!=null&&!k.trim().isEmpty();
  }

  private void markSeenCurrent(){
    SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);
    Set<String> oldSet=p.getStringSet("seen_links_v2",Collections.emptySet());
    LinkedHashSet<String> all=new LinkedHashSet<>();
    for(NewsItem n:items){all.add(n.link);if(all.size()>=600)break;}
    if(oldSet!=null&&all.size()<600)for(String s:oldSet){all.add(s);if(all.size()>=600)break;}
    p.edit().putStringSet("seen_links_v2",all).apply();
  }

  private String shortMessage(Exception e){
    String s=e.getMessage();
    if(s==null||s.trim().isEmpty())s=e.getClass().getSimpleName();
    return s.length()>180?s.substring(0,180)+"…":s;
  }

  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

  @Override protected void onDestroy(){
    pool.shutdownNow();
    super.onDestroy();
  }

  static class NewsAdapter extends BaseAdapter{
    final Context c; final List<NewsItem> data;
    NewsAdapter(Context c,List<NewsItem>d){this.c=c;this.data=d;}
    public int getCount(){return data.size();}
    public Object getItem(int p){return data.get(p);}
    public long getItemId(int p){return p;}

    public View getView(int p,View v,ViewGroup parent){
      LinearLayout box=new LinearLayout(c);
      box.setOrientation(LinearLayout.VERTICAL);
      int pad=(int)(10*c.getResources().getDisplayMetrics().density+0.5f);
      box.setPadding(pad,pad,pad,pad);

      NewsItem n=data.get(p);
      TextView group=new TextView(c);
      group.setText(n.cluster+" • "+n.origin+" • "+n.source);
      group.setTextSize(11);
      group.setTextColor(Color.rgb(31,95,74));
      box.addView(group);

      TextView t=new TextView(c);
      t.setText(n.title);
      t.setTextSize(16);
      t.setTypeface(null,Typeface.BOLD);
      box.addView(t);

      TextView m=new TextView(c);
      String when=n.publishedAt>0
        ?new SimpleDateFormat("HH:mm dd/MM",new Locale("vi","VN")).format(new Date(n.publishedAt))
        :"";
      m.setText(n.topic+(when.isEmpty()?"":" • "+when));
      m.setTextSize(12);
      m.setTextColor(Color.GRAY);
      box.addView(m);

      if(!n.description.isEmpty()){
        TextView d=new TextView(c);
        d.setText(n.description);
        d.setMaxLines(3);
        d.setTextSize(13);
        box.addView(d);
      }
      return box;
    }
  }
}
