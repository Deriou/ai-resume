package dev.deriou.airesume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private String title;

    private String jdContent;

    private String techStack;

    private String location;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
