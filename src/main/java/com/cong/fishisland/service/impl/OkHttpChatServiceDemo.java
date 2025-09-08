package com.cong.fishisland.service.impl;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import okhttp3.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.cong.fishisland.datasource.ai.MockInterviewDataSource.DEFAULT_MODEL;

/**
 * 使用 OkHttp 实现的流式聊天服务
 * 与 WebClient 实现相同的 SSE 流式效果
 * 
 * @author cong
 */
@Service
public class OkHttpChatServiceDemo {

    @Resource
    private AIModelConfig aiModelConfig;
    
    private final OkHttpClient okHttpClient;
    
    public OkHttpChatServiceDemo() {
        // 配置 OkHttp 客户端，专门用于流式请求
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 移除 HTTP/2 配置，使用默认协议
                 .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .build();
    }

    /**
     * 使用 OkHttp 实现流式聊天
     * 与 WebClient.streamChat() 方法功能相同
     */
    public Flux<String> streamChat(String prompt) {
        return Flux.create(sink -> {
            // 构建请求
            Request request = buildRequest(prompt);
            
            // 异步执行请求
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sink.error(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        sink.error(new RuntimeException("HTTP error: " + response.code()));
                        return;
                    }

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            sink.error(new RuntimeException("Response body is null"));
                            return;
                        }
                        // 🔥 关键：流式读取响应体
                        try (BufferedReader reader = new BufferedReader(responseBody.charStream())) {
                            String line;
                            while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                                // 处理 SSE 数据格式
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6); // 去掉 "data: " 前缀
                                    
                                    if ("[DONE]".equals(data.trim())) {
                                        sink.complete();
                                        break;
                                    }
                                    
                                    // 解析并提取内容
                                    String content = parseContent(data);
                                    if (!content.isEmpty()) {
                                        sink.next(content);
                                    }
                                }
                            }
                            
                            sink.complete();
                        }
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    /**
     * 构建 HTTP 请求
     */
    private Request buildRequest(String prompt) {
        SiliconFlowRequest requestBody = buildRequestBody(prompt);
        String jsonBody = JSON.toJSONString(requestBody);

        RequestBody body = RequestBody.create(
            jsonBody, 
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(aiModelConfig.getChutesAi2() + "/chat/completions")
                .post(body)
                .header("Authorization", "ccong")
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .build();
        return request;
    }

    /**
     * 解析 SSE 数据内容
     */
    private String parseContent(String data) {
        try {
            if (data.trim().isEmpty()) {
                return "";
            }
            
            Map<String, Object> map = JSON.parseObject(data, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta != null) {
                    Object content = delta.get("content");
                    return content != null ? content.toString() : "";
                }
            }
        } catch (Exception ignored) {

        }
        return "";
    }

    /**
     * 构建请求体（与 WebClient 版本相同）
     */
    private SiliconFlowRequest buildRequestBody(String prompt) {
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(DEFAULT_MODEL);
        request.setStream(true);

        // 构建消息列表
        List<SiliconFlowRequest.Message> messages = new ArrayList<>();
        SiliconFlowRequest.Message userMessage = new SiliconFlowRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(prompt);
        messages.add(userMessage);

        request.setMessages(messages);
        request.setMax_tokens(512);
        request.setTemperature(0.7);
        request.setTop_p(0.7);

        return request;
    }

    /**
     * 模拟流式数据（演示用）
     */
    public Flux<String> streamMockWithOkHttp(String message) {

        return Flux.create(sink -> {
            // 模拟异步处理
            new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        Thread.sleep(500); // 模拟延迟
                        
                        String data;
                        if (i == 0) {
                            data = "🚀 OkHttp 开始处理: " + message;
                        } else if (i == 9) {
                            data = "[DONE]";
                        } else {
                            data = "⚙️ OkHttp 处理中... 步骤 " + i;
                        }
                        
                        sink.next(data);
                    }
                    
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            }).start();
        });
    }
}