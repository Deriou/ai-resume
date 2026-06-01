package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUser;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.entity.CreditTransaction;
import dev.deriou.airesume.entity.User;
import dev.deriou.airesume.mapper.CreditTransactionMapper;
import dev.deriou.airesume.mapper.UserMapper;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final CreditTransactionMapper creditTransactionMapper;

    public CreditServiceImpl(UserMapper userMapper, CreditTransactionMapper creditTransactionMapper) {
        this.userMapper = userMapper;
        this.creditTransactionMapper = creditTransactionMapper;
    }

    @Override
    public Integer getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "user not found");
        }
        return user.getCreditBalance();
    }

    @Override
    public void ensureSufficient(Long userId, int amount) {
        validateAmount(amount);
        if (getBalance(userId) < amount) {
            throw new BizException(ResultCode.BIZ_ERROR, "insufficient credit balance");
        }
    }

    @Override
    @Transactional
    public CreditTransactionVO charge(Long userId, int amount, String type, String refType, Long refId, String remark) {
        validateAmount(amount);
        int updated = userMapper.chargeCredit(userId, amount);
        if (updated == 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "insufficient credit balance");
        }
        return createTransaction(userId, -amount, type, refType, refId, remark);
    }

    @Override
    @Transactional
    public CreditTransactionVO grant(Long userId, int amount, String type, String refType, Long refId, String remark) {
        validateAmount(amount);
        int updated = userMapper.grantCredit(userId, amount);
        if (updated == 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "user not found");
        }
        return createTransaction(userId, amount, type, refType, refId, remark);
    }

    @Override
    public PageVO<CreditTransactionVO> listMyTransactions(long page, long size) {
        LoginUser user = LoginUserSupport.requireRole(LoginUserSupport.ROLE_USER);
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        Page<CreditTransaction> result = creditTransactionMapper.selectPage(
                Page.of(current, pageSize),
                new LambdaQueryWrapper<CreditTransaction>()
                        .eq(CreditTransaction::getUserId, user.userId())
                        .orderByDesc(CreditTransaction::getCreatedAt)
                        .orderByDesc(CreditTransaction::getId)
        );
        List<CreditTransactionVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    public boolean hasTransactionOnDate(Long userId, String type, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return creditTransactionMapper.selectCount(new LambdaQueryWrapper<CreditTransaction>()
                .eq(CreditTransaction::getUserId, userId)
                .eq(CreditTransaction::getType, type)
                .ge(CreditTransaction::getCreatedAt, start)
                .lt(CreditTransaction::getCreatedAt, end)) > 0;
    }

    private CreditTransactionVO createTransaction(
            Long userId,
            int changeAmount,
            String type,
            String refType,
            Long refId,
            String remark
    ) {
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUserId(userId);
        transaction.setChangeAmount(changeAmount);
        transaction.setType(requireText(type, "transaction type is required"));
        transaction.setBalanceAfter(getBalance(userId));
        transaction.setRefType(StringUtils.hasText(refType) ? refType : null);
        transaction.setRefId(refId);
        transaction.setRemark(StringUtils.hasText(remark) ? remark : null);
        creditTransactionMapper.insert(transaction);
        return toVO(creditTransactionMapper.selectById(transaction.getId()));
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "credit amount must be positive");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ResultCode.BIZ_ERROR, message);
        }
        return value.trim();
    }

    private CreditTransactionVO toVO(CreditTransaction transaction) {
        return new CreditTransactionVO(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getChangeAmount(),
                transaction.getType(),
                transaction.getBalanceAfter(),
                transaction.getRefType(),
                transaction.getRefId(),
                transaction.getRemark(),
                transaction.getCreatedAt()
        );
    }
}
