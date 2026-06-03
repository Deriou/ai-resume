package dev.deriou.airesume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("resume_optimize")
public class ResumeOptimize {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private String targetDirection;

    private String summary;

    private String optimizedBullets;

    private String rewriteSuggestions;

    private String llmModel;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long latencyMs;

    private LocalDateTime createdAt;
}
