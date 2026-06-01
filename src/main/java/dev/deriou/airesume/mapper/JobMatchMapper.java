package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.JobMatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMatchMapper extends BaseMapper<JobMatch> {
}
