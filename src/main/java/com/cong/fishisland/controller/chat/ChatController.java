package com.cong.fishisland.controller.chat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.sse.CustomSseEvent;
import com.cong.fishisland.service.impl.FlexChatServiceDemo;
import com.cong.fishisland.service.impl.OkHttpChatServiceDemo;
import com.cong.fishisland.model.dto.chat.MessageQueryRequest;
import com.cong.fishisland.model.vo.chat.RoomMessageVo;
import com.cong.fishisland.model.ws.response.UserChatResponse;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 *
 * @author cong
 * @date 2024/02/19
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
@Api(value = "聊天")
public class ChatController {

    private final RoomMessageService roomMessageService;
    private final WebSocketService webSocketService;

    @Autowired
    private FlexChatServiceDemo flexChatServiceDemo;
    
    @Autowired
    private OkHttpChatServiceDemo okHttpChatServiceDemo;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "流式聊天演示")
    public Flux<String> streamChatDemo(@RequestParam String prompt) {
        return flexChatServiceDemo.streamChat(prompt);
    }

    @PostMapping("/message/page/vo")
    @ApiOperation(value = "分页获取用户房间消息列表")
    public BaseResponse<Page<RoomMessageVo>> listMessageVoByPage(@RequestBody MessageQueryRequest messageQueryRequest) {
        Page<RoomMessageVo> messageVoPage = roomMessageService.listMessageVoByPage(messageQueryRequest);
        return ResultUtils.success(messageVoPage);
    }

    @GetMapping("/online/user")
    @ApiOperation(value = "获取在线用户列表")
    public BaseResponse<List<UserChatResponse>> getOnlineUserList() {
      return   ResultUtils.success(webSocketService.getOnlineUserList());

    }

    @GetMapping(value = "/stream/mock", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "模拟流式返回数据")
    public Flux<String> streamMockDemo(@RequestParam(defaultValue = "Hello World") String message) {
        return flexChatServiceDemo.streamMock(message);
    }

    @GetMapping(value = "/stream/typing", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "模拟打字效果")
    public Flux<String> streamTypingDemo(@RequestParam(defaultValue = "这是一个模拟的AI助手回复，演示流式输出效果。") String text) {
        return flexChatServiceDemo.streamTyping(text);
    }
    
    // ========== POST + SSE 实现版本（现代 AI 服务标准） ==========
    
    @PostMapping(value = "/stream/post", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "POST + SSE 流式聊天（现代标准）")
    public Flux<String> streamChatPost(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = "你好，请问有什么可以帮助您的吗？";
        }
        return flexChatServiceDemo.streamChat(prompt);
    }
    
    @PostMapping(value = "/stream/mock-post", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "POST + SSE 模拟流式返回")
    public Flux<String> streamMockPost(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "POST 请求测试");
        return flexChatServiceDemo.streamMock(message);
    }
    
    @PostMapping(value = "/stream/typing-post", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "POST + SSE 打字效果")
    public Flux<String> streamTypingPost(@RequestBody Map<String, Object> request) {
        String text = (String) request.getOrDefault("text", "POST + SSE 打字效果演示");
        return flexChatServiceDemo.streamTyping(text);
    }
    
    // ========== OkHttp 实现版本 ==========
    
    @GetMapping(value = "/okhttp/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "OkHttp 流式聊天演示")
    public Flux<String> okHttpStreamChatDemo(@RequestParam String prompt) {
        return okHttpChatServiceDemo.streamChat(prompt);
    }
    
    @GetMapping(value = "/okhttp/mock", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "OkHttp 模拟流式返回")
    public Flux<String> okHttpStreamMockDemo(@RequestParam(defaultValue = "Hello OkHttp World") String message) {
        return okHttpChatServiceDemo.streamMockWithOkHttp(message);
    }
    
    @PostMapping(value = "/okhttp/stream-post", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "OkHttp POST + SSE 流式聊天")
    public Flux<String> okHttpStreamChatPost(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = "你好，请问有什么可以帮助您的吗？";
        }
        return okHttpChatServiceDemo.streamChat(prompt);
    }
    
    @PostMapping(value = "/okhttp/mock-post", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "OkHttp POST + SSE 模拟流式返回")
    public Flux<String> okHttpStreamMockPost(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "OkHttp POST 请求测试");
        return okHttpChatServiceDemo.streamMockWithOkHttp(message);
    }
    
    // ========== 自定义SSE事件接口 ==========
    
    @GetMapping(value = "/stream/custom-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "自定义SSE事件格式演示")
    public Flux<String> streamCustomSse(@RequestParam(defaultValue = "自定义SSE测试") String message) {
        return flexChatServiceDemo.streamCustomSse(message);
    }
    
    @PostMapping(value = "/stream/ai-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "AI聊天模拟（自定义SSE格式）")
    public Flux<String> streamAiChat(@RequestBody Map<String, Object> request) {
        String question = (String) request.getOrDefault("question", "你好");
        return flexChatServiceDemo.streamAiChat(question);
    }
    
    @PostMapping(value = "/stream/file-upload", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "文件上传进度（自定义SSE格式）")
    public Flux<String> streamFileUpload(@RequestBody Map<String, Object> request) {
        String filename = (String) request.getOrDefault("filename", "test.pdf");
        return flexChatServiceDemo.streamFileUpload(filename);
    }
    
    // ========== Spring 原生 ServerSentEvent 接口 ==========
    
    @GetMapping(value = "/stream/server-sent-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "Spring 原生 ServerSentEvent 演示")
    public Flux<ServerSentEvent<String>> streamServerSentEvent(@RequestParam(defaultValue = "ServerSentEvent测试") String message) {
        return flexChatServiceDemo.streamServerSentEvents(message);
    }
    
    @PostMapping(value = "/stream/ai-server-sent-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "AI聊天 ServerSentEvent 版本")
    public Flux<ServerSentEvent<String>> streamAiServerSentEvent(@RequestBody Map<String, Object> request) {
        String question = (String) request.getOrDefault("question", "你好");
        return flexChatServiceDemo.streamAiChatServerSentEvents(question);
    }
    
    // ========== 返回 Flux<CustomSseEvent<T>> 格式接口 ==========
    
    @GetMapping(value = "/stream/custom-sse-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "自定义SSE事件对象演示")
    public Flux<CustomSseEvent<String>> streamCustomSseEvent(@RequestParam(defaultValue = "CustomSseEvent测试") String message) {
        return flexChatServiceDemo.streamCustomSseEvents(message);      
    }
    
    @PostMapping(value = "/stream/ai-custom-sse-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "AI聊天 - 自定义SSE事件对象")
    public Flux<CustomSseEvent<String>> streamAiChatCustomSseEvent(@RequestBody Map<String, Object> request) {
        String question = (String) request.getOrDefault("question", "你好");
        return flexChatServiceDemo.streamAiChatCustomSseEvents(question);
    }
    
    @PostMapping(value = "/stream/file-upload-custom-sse-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "文件上传进度 - 自定义SSE事件对象")
    public Flux<CustomSseEvent<String>> streamFileUploadCustomSseEvent(@RequestBody Map<String, Object> request) {
        String filename = (String) request.getOrDefault("filename", "test.pdf");
        return flexChatServiceDemo.streamFileUploadCustomSseEvents(filename);
    }
}