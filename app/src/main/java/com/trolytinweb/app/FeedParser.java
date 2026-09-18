package com.trolytinweb.app;

import android.text.Html;
import org.w3c.dom.*;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;

public final class FeedParser {
  private FeedParser(){}

  public static List<NewsItem> parse(InputStream input,String topic,String origin) throws Exception {
    List<NewsItem> out=new ArrayList<>();
    DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
    try{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(Exception ignored){}
    try{f.setFeature("http://xml.org/sax/features/external-general-entities",false);}catch(Exception ignored){}
    try{f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);}catch(Exception ignored){}
    f.setExpandEntityReferences(false);
    Document d=f.newDocumentBuilder().parse(input);
    NodeList list=d.getElementsByTagName("item");
    for(int i=0;i<list.getLength() && i<60;i++){
      Element e=(Element)list.item(i);
      String title=text(e,"title"), link=text(e,"link");
      if(title.isEmpty()||link.isEmpty()) continue;
      String source=text(e,"source");
      if(source.isEmpty()){
        source="Nguồn web";
        int p=title.lastIndexOf(" - ");
        if(p>10){ source=title.substring(p+3).trim(); title=title.substring(0,p).trim(); }
      }
      String desc=Html.fromHtml(text(e,"description"),Html.FROM_HTML_MODE_LEGACY)
        .toString().replaceAll("\\s+"," ").trim();
      if(desc.length()>360) desc=desc.substring(0,357)+"…";
      out.add(new NewsItem(topic,title,link,source,desc,parseDate(text(e,"pubDate")),origin));
    }
    return out;
  }

  private static String text(Element e,String tag){
    NodeList n=e.getElementsByTagName(tag);
    return n.getLength()==0?"":n.item(0).getTextContent().trim();
  }

  private static long parseDate(String s){
    String[] p={
      "EEE, dd MMM yyyy HH:mm:ss z",
      "EEE, dd MMM yyyy HH:mm:ss Z",
      "EEE, dd MMM yyyy HH:mm:ss 'GMT'"
    };
    for(String x:p) try{
      Date d=new SimpleDateFormat(x,Locale.US).parse(s);
      if(d!=null)return d.getTime();
    }catch(Exception ignored){}
    return System.currentTimeMillis();
  }
}
