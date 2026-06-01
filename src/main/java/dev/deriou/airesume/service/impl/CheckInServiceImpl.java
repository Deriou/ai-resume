package dev.deriou.airesume.service.impl;

import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.redis.RedisKeys;
import dev.deriou.airesume.service.CheckInService;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.vo.CheckInStatusVO;
import java.time.LocalDate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInServiceImpl implements CheckInService {

    private static final int CHECK_IN_REWARD = 1;

    private final CreditService creditService;
    private final StringRedisTemplate stringRedisTemplate;

    public CheckInServiceImpl(CreditService creditService, StringRedisTemplate stringRedisTemplate) {
        this.creditService = creditService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CheckInStatusVO today() {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        LocalDate today = LocalDate.now();
        boolean checkedIn = isCheckedIn(user.userId(), today);
        return new CheckInStatusVO(checkedIn, today, CHECK_IN_REWARD, creditService.getBalance(user.userId()));
    }

    @Override
    @Transactional
    public CheckInStatusVO checkIn() {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        LocalDate today = LocalDate.now();
        if (creditService.hasTransactionOnDate(user.userId(), CreditService.TYPE_CHECK_IN, today)) {
            throw new BizException(ResultCode.BIZ_ERROR, "already checked in");
        }

        creditService.grant(
                user.userId(),
                CHECK_IN_REWARD,
                CreditService.TYPE_CHECK_IN,
                CreditService.REF_TYPE_CHECK_IN,
                null,
                "daily check-in reward"
        );
        stringRedisTemplate.opsForValue().setBit(RedisKeys.checkIn(user.userId(), today), today.getDayOfMonth() - 1, true);
        return new CheckInStatusVO(true, today, CHECK_IN_REWARD, creditService.getBalance(user.userId()));
    }

    private boolean isCheckedIn(Long userId, LocalDate date) {
        if (creditService.hasTransactionOnDate(userId, CreditService.TYPE_CHECK_IN, date)) {
            return true;
        }
        Boolean bit = stringRedisTemplate.opsForValue().getBit(RedisKeys.checkIn(userId, date), date.getDayOfMonth() - 1);
        return Boolean.TRUE.equals(bit);
    }
}
