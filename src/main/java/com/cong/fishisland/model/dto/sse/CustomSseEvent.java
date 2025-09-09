package com.cong.fishisland.model.dto.sse;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * 自定义SSE事件消息结构（支持泛型）
 * 提供更灵活的字段控制和业务语义
 * 
 * 注意：为了让Spring正确处理SSE事件类型，需要确保event字段能被正确识别
 *
 * @author cong
 * @date 2024/12/08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomSseEvent<T> {
    
    /**
     * 事件ID - 用于客户端重连时的断点续传
     */
    @Nullable
    private String id;
    
    /**
     * 事件类型 - 便于前端区分处理逻辑
     * 例如: message, error, progress, complete, typing, thinking
     */
    @Nullable
    private String event;
    
    /**
     * 实际数据内容（支持泛型）
     */
    @Nullable
    private T data;
    
    /**
     * 重试间隔(毫秒) - 客户端断开后的重连间隔
     */
    @Nullable
    private Long retry;
    
    /**
     * 消息状态 - 业务自定义字段
     * 例如: processing, completed, error, waiting
     */
    @Nullable
    private String status;
    
    /**
     * 进度百分比 - 用于显示处理进度
     */
    @Nullable
    private Integer progress;
    
    /**
     * 时间戳 - 消息生成时间
     */
    @Nullable
    private Long timestamp;
    
    /**
     * 额外元数据 - JSON字符串格式
     */
    @Nullable
    private String metadata;
    
    // ========== 模仿 ServerSentEvent 的 getter 方法命名 ==========
    
    /**
     * 获取事件ID（模仿 ServerSentEvent.id() 方法）
     */
    @Nullable
    public String id() {
        return this.id;
    }
    
    /**
     * 获取事件类型（模仿 ServerSentEvent.event() 方法）
     */
    @Nullable
    public String event() {
        return this.event;
    }
    
    /**
     * 获取数据内容（模仿 ServerSentEvent.data() 方法）
     */
    @Nullable
    public T data() {
        return this.data;
    }
    
    /**
     * 获取重试间隔（模仿 ServerSentEvent.retry() 方法）
     */
    @Nullable
    public java.time.Duration retry() {
        return this.retry != null ? java.time.Duration.ofMillis(this.retry) : null;
    }
    
    /**
     * 重写toString方法，返回标准SSE格式
     * 这样当对象被转换为字符串时，会输出正确的SSE格式
     */
    @Override
    public String toString() {
        return toSseFormat();
    }
    
    /**
     * 转换为标准SSE格式字符串
     * 遵循 Server-Sent Events 规范
     * 与Spring官方ServerSseEvent保持一致格式
     * 
     * 注意：这个方法返回原始的SSE格式字符串，
     * 会被Spring WebFlux直接输出而不再次包装
     */
    public String toSseFormat() {
        // 由于Spring WebFlux会对Flux<String>中的每个字符串自动添加"data:"前缀
        // 我们需要返回一个特殊格式的字符串，让Spring不要再次包装
        
        // 解决方案：返回一个完整的SSE格式字符串，但在开头加上\r\n来“欺骗”Spring
        StringBuilder sse = new StringBuilder();
        
        // 添加一个空行来让Spring认为这已经是完整的SSE数据
        sse.append("\r\n");
        
        if (id != null) {
            sse.append("id:").append(id).append("\n");
        }
        
        if (event != null) {
            sse.append("event:").append(event).append("\n");
        }
        
        if (retry != null) {
            sse.append("retry:").append(retry).append("\n");
        }
        
        if (progress != null) {
            sse.append("progress:").append(progress).append("\n");
        }
        
        if (data != null) {
            String dataString = (data instanceof String) ? (String) data : String.valueOf(data);
            String[] lines = dataString.split("\n", -1);
            for (String line : lines) {
                sse.append("data:").append(line).append("\n");
            }
        }
        
        sse.append("\n");
        
        return sse.toString();
    }
    
    // ========== 便捷构建方法 ==========
    
    /**
     * 创建简单消息事件
     */
    public static CustomSseEvent message(String data) {
        return CustomSseEvent.builder()
                .id("msg_" + System.currentTimeMillis())
                .event("message")
                .data(data)
                .build();
    }
    
    /**
     * 创建进度事件
     */
    public static CustomSseEvent progress(String message, int progress) {
        return CustomSseEvent.builder()
                .id("progress_" + System.currentTimeMillis())
                .event("progress")
                .data(message)
                .progress(progress)
                .build();
    }
    
    /**
     * 创建错误事件
     */
    public static CustomSseEvent error(String errorMessage) {
        return CustomSseEvent.builder()
                .id("error_" + System.currentTimeMillis())
                .event("error")
                .data(errorMessage)
                .build();
    }
    
    /**
     * 创建完成事件
     */
    public static CustomSseEvent complete(String result) {
        return CustomSseEvent.builder()
                .id("complete_" + System.currentTimeMillis())
                .event("complete")
                .data(result)
                .build();
    }
    
    /**
     * 创建打字效果事件
     */
    public static CustomSseEvent typing(String character) {
        return CustomSseEvent.builder()
                .id("typing_" + System.currentTimeMillis())
                .event("typing")
                .data(character)
                .build();
    }
    
    /**
     * 创建AI思考事件
     */
    public static CustomSseEvent thinking(String thought) {
        return CustomSseEvent.builder()
                .id("thinking_" + System.currentTimeMillis())
                .event("thinking")
                .data(thought)
                .build();
    }
    
    /**
     * 创建结束标记事件
     * 注意：不包含额外元数据，保持纯净的完成标志
     */
    public static CustomSseEvent done() {
        return CustomSseEvent.builder()
                .id("done_" + System.currentTimeMillis())
                .event("done")
                .data("[DONE]")
                .build();
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

}