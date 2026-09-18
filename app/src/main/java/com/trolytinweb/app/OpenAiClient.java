package com.trolytinweb.app;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class OpenAiClient {
  private OpenAiClient(){}

  public static String summarize(String apiKey,List<NewsItem> items) throws Exception {
    if(apiKey==null||apiKey.trim().isEmpty()) throw new Exception("Chưa có OpenAI API key.");
    String prompt=buildPrompt(items);
    try{
      return request(apiKey,prompt,true);
    }catch(Exception first){
      return request(apiKey,prompt,false);
    }
  }

  private static String request(String key,String prompt,boolean webSearch)throws Exception{
    URL url=new URL("https://api.openai.com/v1/responses");
    HttpURLConnection c=(HttpURLConnection)url.openConnection();
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setConnectTimeout(15000);
    c.setReadTimeout(90000);
    c.setRequestProperty("Authorization","Bearer "+key.trim());
    c.setRequestProperty("Content-Type","application/json");

    JSONObject body=new JSONObject();
    body.put("model","gpt-5.6-luna");
    body.put("max_output_tokens",1000);
    body.put("instructions",
      "Bạn là trợ lý tổng hợp tin tức tiếng Việt. Hãy ưu tiên sự kiện mới, đối chiếu nhiều nguồn, "
      +"không suy đoán. Nếu các nguồn mâu thuẫn, nói rõ. Trả lời ngắn gọn theo 4 mục: "
      +"TÓM TẮT, CÁC NHÓM TIN, ĐIỂM ĐÁNG CHÚ Ý, NGUỒN NÊN MỞ.");
    body.put("input",prompt);
    if(webSearch){
      JSONArray tools=new JSONArray();
      tools.put(new JSONObject().put("type","web_search"));
      body.put("tools",tools);
    }

    byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);
    try(OutputStream os=c.getOutputStream()){os.write(data);}
    int code=c.getResponseCode();
    InputStream stream=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
    String raw=read(stream);
    c.disconnect();
    if(code<200||code>=300){
      String msg=raw;
      try{
        JSONObject e=new JSONObject(raw).optJSONObject("error");
        if(e!=null)msg=e.optString("message",raw);
      }catch(Exception ignored){}
      throw new Exception("OpenAI: "+msg);
    }
    String text=extractText(new JSONObject(raw));
    if(text.isEmpty()) throw new Exception("AI không trả về nội dung.");
    return text.trim();
  }

  private static String buildPrompt(List<NewsItem> items){
    StringBuilder b=new StringBuilder();
    b.append("Đây là các kết quả mà ứng dụng vừa thu thập. Hãy dùng web search khi cần để mở/kiểm tra thêm nguồn công khai và tổng hợp dựa trên nhiều nguồn.\n\n");
    int limit=Math.min(24,items.size());
    for(int i=0;i<limit;i++){
      NewsItem n=items.get(i);
      b.append(i+1).append(". [").append(n.cluster).append("] ")
        .append(n.title).append("\nNguồn: ").append(n.source)
        .append(" | Bộ tìm: ").append(n.origin)
        .append("\nLink: ").append(n.link);
      if(!n.description.isEmpty())b.append("\nMô tả: ").append(n.description);
      b.append("\n\n");
    }
    b.append("Không được coi mọi tiêu đề là sự thật đã xác minh. Hãy phân biệt điều nhiều nguồn cùng xác nhận với thông tin chỉ xuất hiện ở một nguồn.");
    return b.toString();
  }

  private static String extractText(JSONObject root){
    StringBuilder out=new StringBuilder();
    JSONArray arr=root.optJSONArray("output");
    if(arr==null)return root.optString("output_text","");
    for(int i=0;i<arr.length();i++){
      JSONObject item=arr.optJSONObject(i);
      if(item==null)continue;
      JSONArray content=item.optJSONArray("content");
      if(content==null)continue;
      for(int j=0;j<content.length();j++){
        JSONObject part=content.optJSONObject(j);
        if(part==null)continue;
        if("output_text".equals(part.optString("type"))){
          String t=part.optString("text","");
          if(!t.isEmpty()){if(out.length()>0)out.append("\n");out.append(t);}
        }
      }
    }
    return out.toString();
  }

  private static String read(InputStream in)throws Exception{
    if(in==null)return "";
    try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
      StringBuilder b=new StringBuilder(); String line;
      while((line=r.readLine())!=null)b.append(line).append('\n');
      return b.toString();
    }
  }
}
