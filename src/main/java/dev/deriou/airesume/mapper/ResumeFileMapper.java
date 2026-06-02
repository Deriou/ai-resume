package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.ResumeFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeFileMapper extends BaseMapper<ResumeFile> {
}
