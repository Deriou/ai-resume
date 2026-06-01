package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
