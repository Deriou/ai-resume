package dev.deriou.airesume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_match")
public class JobMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private Long jobId;

    private Integer matchScore;

    private String strengths;

    private String gaps;

    private String suggestions;

    private LocalDateTime createdAt;
}
