package dev.deriou.airesume.context;

import dev.deriou.airesume.common.api.ResultCode;
import dev.deriou.airesume.common.exception.BizException;

public final class LoginUserSupport {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ENTERPRISE = "ENTERPRISE";
    public static final String ROLE_ADMIN = "ADMIN";

    private LoginUserSupport() {
    }

    public static LoginUser currentUser() {
        return UserHolder.current()
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED));
    }

    public static LoginUser requireRole(String role) {
        LoginUser user = currentUser();
        if (!role.equals(user.role())) {
            throw new BizException(ResultCode.FORBIDDEN, "role " + role + " is required");
        }
        return user;
    }
}
