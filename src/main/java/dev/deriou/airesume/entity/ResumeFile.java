package dev.deriou.airesume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("resume_file")
public class ResumeFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long resumeId;

    private Long userId;

    private String originalName;

    private String storedName;

    private String storagePath;

    private String contentType;

    private Long fileSize;

    private String fileExt;

    private LocalDateTime createdAt;
}
