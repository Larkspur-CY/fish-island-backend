package com.cong.fishisland.service.impl;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.dto.sse.CustomSseEvent;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.cong.fishisland.datasource.ai.MockInterviewDataSource.DEFAULT_MODEL;

@Service
public class FlexChatServiceDemo {

    private static final Logger log = LoggerFactory.getLogger(FlexChatServiceDemo.class);
    @Resource
    private WebClient webClient;
    @Resource
    private AIModelConfig aiModelConfig;

    public Flux<String> streamChat(String prompt) {
        return webClient.post()
                .uri(aiModelConfig.getChutesAi2() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "ccong")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildRequestBody(prompt))
                .retrieve()
                .bodyToFlux(String.class)
                .takeUntil(line -> line.contains("[DONE]"))
                .map(chunk -> {
                    try {
                        log.info(chunk);

                        String json = chunk.substring("data:".length()).trim();
                        Map<String, Object> map = JSON.parseObject(chunk, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            Object content = delta.get("content");
                            return content != null ? content.toString() : "";
                        }
                    } catch (Exception e) {
                        log.error("Error parsing chunk: {}", chunk, e);
                    }
                    return "";
                })
                .filter(s -> !s.isEmpty());
    }

    // 构建请求体
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

        // 设置其他流式参数（根据 SiliconFlowRequest 默认值）
        request.setMax_tokens(512);
        request.setTemperature(0.7);
        request.setTop_p(0.7);
        // 其他参数保持默认值...

        return request; // 直接返回对象，Jackson 会自动序列化
    }

    /**
     * 模拟流式处理数据
     * @param message 处理消息
     * @return 流式响应
     */
    public Flux<String> streamMock(String message) {
        return Flux.interval(Duration.ofMillis(500))
                .take(10)
                .map(i -> {
                    if (i == 0) {
                        return "开始处理您的请求: " + message;
                    } else if (i == 9) {
                        return "处理完成！";
                    } else {
                        return "正在处理中... 步骤 " + i;
                    }
                })
                .concatWith(Flux.just("[DONE]"));
    }

    /**
     * 模拟打字效果
     * @param text 要输出的文本
     * @return 流式响应
     */
    public Flux<String> streamTyping(String text) {
        return Flux.fromIterable(
                text.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .collect(Collectors.toList())
                )
                .delayElements(Duration.ofMillis(100))
                .concatWith(Flux.just("[DONE]"));
    }
    
    // ========== 自定义SSE事件方法 ==========
    
    /**
     * 使用自定义SSE事件格式的模拟流程
     * 展示丰富的事件类型和元数据
     */
    public Flux<String> streamCustomSse(String message) {
        return Flux.create(sink -> {
            // 开始事件
            sink.next(CustomSseEvent.message("开始处理您的请求: " + message).toSseFormat());
            
            // 模拟异步处理
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 8; i++) {
                        Thread.sleep(500);
                        
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        // 进度事件
                        sink.next(CustomSseEvent.progress(
                            "正在处理中... 步骤 " + i, 
                            (int)(i / 8.0 * 100)
                        ).toSseFormat());
                    }
                    
                    Thread.sleep(300);
                    
                    // 完成事件
                    sink.next(CustomSseEvent.complete("处理完成！").toSseFormat());
                    
                    // 结束标记
                    sink.next(CustomSseEvent.done().toSseFormat());
                    
                    sink.complete();
                    
                } catch (InterruptedException e) {
                    sink.error(e);
                } catch (Exception e) {
                    sink.next(CustomSseEvent.error("处理失败: " + e.getMessage()).toSseFormat());
                    sink.complete();
                }
            }).start();
        });
    }
    
    /**
     * 使用自定义SSE事件的AI聊天模拟
     * 包含思考、打字、完成等多种状态
     */
    public Flux<String> streamAiChat(String question) {
        return Flux.create(sink -> {
            new Thread(() -> {
                try {
                    // 1. 思考阶段
                    sink.next(CustomSseEvent.thinking("正在思考您的问题...").toSseFormat());
                    Thread.sleep(800);
                    
                    sink.next(CustomSseEvent.thinking("分析问题要点...").toSseFormat());
                    Thread.sleep(600);
                    
                    // 2. 开始回答
                    String answer = "根据您的问题 '" + question + "'，我的回答是：这是一个很好的问题。";
                    
                    sink.next(CustomSseEvent.message("开始生成回答").toSseFormat());
                    Thread.sleep(300);
                    
                    // 3. 打字效果
                    for (char c : answer.toCharArray()) {
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        sink.next(CustomSseEvent.typing(String.valueOf(c)).toSseFormat());
                        Thread.sleep(50); // 打字速度
                    }
                    
                    Thread.sleep(500);
                    
                    // 4. 完成
                    sink.next(CustomSseEvent.complete("回答完成，希望对您有帮助！").toSseFormat());
                    sink.next(CustomSseEvent.done().toSseFormat());
                    
                    sink.complete();
                    
                } catch (Exception e) {
                    sink.next(CustomSseEvent.error("AI处理异常: " + e.getMessage()).toSseFormat());
                    sink.complete();
                }
            }).start();
        });
    }
    
    /**
     * 文件上传进度模拟（使用自定义SSE）
     */
    public Flux<String> streamFileUpload(String filename) {
        return Flux.create(sink -> {
            new Thread(() -> {
                try {
                    sink.next(CustomSseEvent.message("开始上传文件: " + filename).toSseFormat());
                    
                    for (int progress = 0; progress <= 100; progress += 10) {
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        String message = String.format("上传进度: %d%% (%s)", progress, filename);
                        sink.next(CustomSseEvent.progress(message, progress).toSseFormat());
                        
                        Thread.sleep(200);
                    }
                    
                    sink.next(CustomSseEvent.complete("文件上传完成: " + filename).toSseFormat());
                    sink.next(CustomSseEvent.done().toSseFormat());
                    
                    sink.complete();
                    
                } catch (Exception e) {
                    sink.next(CustomSseEvent.error("文件上传失败: " + e.getMessage()).toSseFormat());
                    sink.complete();
                }
            }).start();
        });
    }
    
    // ========== 使用 Spring 原生 ServerSentEvent 的方法 ==========
    
    /**
     * 返回 Spring 原生的 ServerSentEvent 流
     * 更加符合 Spring WebFlux 标准
     */
    public Flux<ServerSentEvent<String>> streamServerSentEvents(String message) {
        return Flux.create(sink -> {
            new Thread(() -> {
                try {
                    // 1. 开始事件
                    sink.next(ServerSentEvent.<String>builder()
                            .id("msg_001")
                            .event("message")
                            .data("开始处理您的请求: " + message)
                            .retry(Duration.ofSeconds(3))
                            .build());
                    
                    Thread.sleep(500);
                    
                    // 2. 进度事件
                    for (int i = 1; i <= 8; i++) {
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        sink.next(ServerSentEvent.<String>builder()
                                .id("progress_" + i)
                                .event("progress")
                                .data("正在处理中... 步骤 " + i + " (进度: " + (int)(i/8.0*100) + "%)")
                                .build());
                        
                        Thread.sleep(500);
                    }
                    
                    // 3. 完成事件
                    sink.next(ServerSentEvent.<String>builder()
                            .id("complete")
                            .event("complete")
                            .data("处理完成！")
                            .build());
                    
                    // 4. 结束事件
                    sink.next(ServerSentEvent.<String>builder()
                            .id("done")
                            .event("done")
                            .data("[DONE]")
                            .build());
                    
                    sink.complete();
                    
                } catch (Exception e) {
                    sink.next(ServerSentEvent.<String>builder()
                            .id("error")
                            .event("error")
                            .data("处理失败: " + e.getMessage())
                            .build());
                    sink.complete();
                }
            }).start();
        });
    }
    
    /**
     * AI聊天 - 使用 ServerSentEvent
     */
    public Flux<ServerSentEvent<String>> streamAiChatServerSentEvents(String question) {
        return Flux.create(sink -> {
            new Thread(() -> {
                try {
                    // 1. 思考阶段
                    sink.next(ServerSentEvent.<String>builder()
                            .id("thinking_1")
                            .event("thinking")
                            .data("正在思考您的问题...")
                            .build());
                    Thread.sleep(800);
                    
                    sink.next(ServerSentEvent.<String>builder()
                            .id("thinking_2")
                            .event("thinking")
                            .data("分析问题要点...")
                            .build());
                    Thread.sleep(600);
                    
                    // 2. 开始回答
                    String answer = "根据您的问题 '" + question + "'，我的回答是：这是一个很好的问题。";
                    
                    sink.next(ServerSentEvent.<String>builder()
                            .id("start_answer")
                            .event("message")
                            .data("开始生成回答")
                            .build());
                    Thread.sleep(300);
                    
                    // 3. 打字效果 - 每个字符一个事件
                    char[] chars = answer.toCharArray();
                    for (int i = 0; i < chars.length; i++) {
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        sink.next(ServerSentEvent.<String>builder()
                                .id("typing_" + i)
                                .event("typing")
                                .data(String.valueOf(chars[i]))
                                .build());
                        Thread.sleep(50);
                    }
                    
                    Thread.sleep(500);
                    
                    // 4. 完成
                    sink.next(ServerSentEvent.<String>builder()
                            .id("ai_complete")
                            .event("complete")
                            .data("回答完成，希望对您有帮助！")
                            .build());
                    
                    sink.next(ServerSentEvent.<String>builder()
                            .id("ai_done")
                            .event("done")
                            .data("[DONE]")
                            .build());
                    
                    sink.complete();
                    
                } catch (Exception e) {
                    sink.next(ServerSentEvent.<String>builder()
                            .id("ai_error")
                            .event("error")
                            .data("AI处理异常: " + e.getMessage())
                            .build());
                    sink.complete();
                }
            }).start();
        });
    }
    
    // ========== 返回 Flux<CustomSseEvent<T>> 的方法 ==========
    
    /**
     * 返回 CustomSseEvent 对象流（泛型版本）
     * 直接返回事件对象，不转换为SSE格式字符串
     * 注意：Spring会将整个对象序列化为JSON，不会使用event字段作为SSE事件类型
     */
    public Flux<CustomSseEvent<String>> streamCustomSseEvents(String message) {
        return Flux.create(sink -> {
            // 开始事件
            sink.next(CustomSseEvent.<String>message("开始处理您的请求: " + message));
            
            // 模拟异步处理
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 8; i++) {
                        Thread.sleep(500);
                        
                        if (sink.isCancelled()) {
                            break;
                        }
                        
                        // 进度事件
                        sink.next(CustomSseEvent.<String>progress(
                            "正在处理中... 步骤 " + i, 
                            (int)(i / 8.0 * 100)
                        ));
                    }
                    
                    Thread.sleep(300);
                    
                    // 完成事件
                    sink.next(CustomSseEvent.complete("处理完成！"));
                    
                    // 结束标记 - 修正：使用纯净的done事件
                    sink.next(CustomSseEvent.done());
                    
                    sink.complete();
                    
                } catch (InterruptedException e) {
                    sink.error(e);
                } catch (Exception e) {
                    sink.next(CustomSseEvent.error("处理失败: " + e.getMessage()));
                    sink.complete();
                }
            }).start();
        });
    }
    
    /**
     * AI聊天 - 返回 CustomSseEvent 对象流
     */
    public Flux<CustomSseEvent<String>> streamAiChatCustomSseEvents(String question) {
        return Flux.create(sink -> new Thread(() -> {
            try {
                // 1. 思考阶段
                sink.next(CustomSseEvent.thinking("正在思考您的问题..."));
                Thread.sleep(800);

                sink.next(CustomSseEvent.thinking("分析问题要点..."));
                Thread.sleep(600);

                // 2. 开始回答
                String answer = "根据您的问题 '" + question + "'，我的回答是：这是一个很好的问题。";

                sink.next(CustomSseEvent.message("开始生成回答"));
                Thread.sleep(300);

                // 3. 打字效果
                for (char c : answer.toCharArray()) {
                    if (sink.isCancelled()) {
                        break;
                    }

                    sink.next(CustomSseEvent.typing(String.valueOf(c)));
                    Thread.sleep(50); // 打字速度
                }

                Thread.sleep(500);

                // 4. 完成
                sink.next(CustomSseEvent.complete("回答完成，希望对您有帮助！"));

                // 结束标记 - 修正：使用纯净的done事件
                sink.next(CustomSseEvent.done());

                sink.complete();

            } catch (Exception e) {
                sink.next(CustomSseEvent.error("AI处理异常: " + e.getMessage()));
                sink.complete();
            }
        }).start());
    }
    
    /**
     * 文件上传进度 - 返回 CustomSseEvent 对象流
     */
    public Flux<CustomSseEvent<String>> streamFileUploadCustomSseEvents(String filename) {
        return Flux.create(sink -> new Thread(() -> {
            try {
                sink.next(CustomSseEvent.message("开始上传文件: " + filename));

                for (int progress = 0; progress <= 100; progress += 10) {
                    if (sink.isCancelled()) {
                        break;
                    }

                    String message = String.format("上传进度: %d%% (%s)", progress, filename);
                    sink.next(CustomSseEvent.progress(message, progress));

                    Thread.sleep(200);
                }

                sink.next(CustomSseEvent.complete("文件上传完成: " + filename));

                // 结束标记
                sink.next(CustomSseEvent.done());

                sink.complete();

            } catch (Exception e) {
                sink.next(CustomSseEvent.error("文件上传失败: " + e.getMessage()));
                sink.complete();
            }
        }).start());
    }
}
