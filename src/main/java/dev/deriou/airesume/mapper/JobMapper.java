package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.Job;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
