package dev.deriou.airesume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;
import dev.deriou.airesume.common.pagination.PageSupport;
import dev.deriou.airesume.context.LoginUserSupport;
import dev.deriou.airesume.dto.AdminCreditGrantRequest;
import dev.deriou.airesume.entity.User;
import dev.deriou.airesume.mapper.UserMapper;
import dev.deriou.airesume.service.AdminUserService;
import dev.deriou.airesume.service.CreditService;
import dev.deriou.airesume.vo.AdminUserVO;
import dev.deriou.airesume.vo.CreditTransactionVO;
import dev.deriou.airesume.vo.PageVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final String REF_TYPE_ADMIN_GRANT = "ADMIN_GRANT";

    private final UserMapper userMapper;
    private final CreditService creditService;

    public AdminUserServiceImpl(UserMapper userMapper, CreditService creditService) {
        this.userMapper = userMapper;
        this.creditService = creditService;
    }

    @Override
    public PageVO<AdminUserVO> users(long page, long size, String role, String status, String keyword) {
        requireAdmin();
        long current = PageSupport.page(page);
        long pageSize = PageSupport.size(size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        String normalizedRole = normalizeRole(role);
        if (normalizedRole != null) {
            wrapper.eq(User::getRole, normalizedRole);
        }
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus != null) {
            wrapper.eq(User::getStatus, normalizedStatus);
        }
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(q -> q.like(User::getUsername, like).or().like(User::getNickName, like));
        }
        wrapper.orderByAsc(User::getId);
        Page<User> result = userMapper.selectPage(Page.of(current, pageSize), wrapper);
        List<AdminUserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageVO.of(records, result.getCurrent(), result.getSize(), result.getTotal(), result.getPages());
    }

    @Override
    @Transactional
    public CreditTransactionVO grantCredit(Long userId, AdminCreditGrantRequest request) {
        requireAdmin();
        if (request.amount() == null || request.amount() <= 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "amount must be greater than 0");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "user not found");
        }
        if (!LoginUserSupport.ROLE_USER.equals(target.getRole())) {
            throw new BizException(ResultCode.BIZ_ERROR, "admin can only grant credits to USER accounts");
        }
        String remark = StringUtils.hasText(request.remark()) ? request.remark().trim() : "admin grant";
        return creditService.grant(
                userId,
                request.amount(),
                CreditService.TYPE_ADMIN_GRANT,
                REF_TYPE_ADMIN_GRANT,
                null,
                remark
        );
    }

    private void requireAdmin() {
        LoginUserSupport.requireRole(LoginUserSupport.ROLE_ADMIN);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (!LoginUserSupport.ROLE_USER.equals(normalized)
                && !LoginUserSupport.ROLE_ENTERPRISE.equals(normalized)
                && !LoginUserSupport.ROLE_ADMIN.equals(normalized)) {
            throw new BizException(ResultCode.BIZ_ERROR, "role must be USER, ENTERPRISE or ADMIN");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"DISABLED".equals(normalized)) {
            throw new BizException(ResultCode.BIZ_ERROR, "status must be ACTIVE or DISABLED");
        }
        return normalized;
    }

    private AdminUserVO toVO(User user) {
        return new AdminUserVO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getNickName(),
                user.getCreditBalance(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
