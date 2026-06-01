package dev.deriou.airesume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.deriou.airesume.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("""
            UPDATE app_user
            SET credit_balance = credit_balance - #{amount}
            WHERE id = #{userId}
              AND credit_balance >= #{amount}
            """)
    int chargeCredit(@Param("userId") Long userId, @Param("amount") int amount);

    @Update("""
            UPDATE app_user
            SET credit_balance = credit_balance + #{amount}
            WHERE id = #{userId}
            """)
    int grantCredit(@Param("userId") Long userId, @Param("amount") int amount);
}
