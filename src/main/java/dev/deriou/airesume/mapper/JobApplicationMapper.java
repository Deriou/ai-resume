package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
