package com.cong.fishisland.controller.test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 测试控制器
 * 用于提供SSE测试页面
 *
 * @author cong
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/sse")
    public ResponseEntity<String> sseTestPage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/sse-test.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("测试页面未找到");
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<String> simpleTestPage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/sse-simple.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("简单测试页面未找到");
        }
    }
    
    @GetMapping("/compare")
    public ResponseEntity<String> compareTestPage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/webclient-vs-okhttp.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("对比测试页面未找到");
        }
    }
    
    @GetMapping("/post-sse")
    public ResponseEntity<String> postSseTestPage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/post-sse-test.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("POST + SSE 测试页面未找到");
        }
    }

    @GetMapping("/ai/chat")
    public ResponseEntity<String> aiChatPage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/ai-chat.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("简单测试页面未找到");
        }
    }
    
    @GetMapping("/custom-sse")
    public ResponseEntity<String> customSsePage() {
        try {
            ClassPathResource resource = new ClassPathResource("static/custom-sse-demo.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(404)
                    .body("自定义SSE演示页面未找到");
        }
    }
}