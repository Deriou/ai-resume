package dev.deriou.airesume.context;

import java.util.Optional;

public final class UserHolder {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void save(LoginUser user) {
        USER_HOLDER.set(user);
    }

    public static Optional<LoginUser> current() {
        return Optional.ofNullable(USER_HOLDER.get());
    }

    public static void remove() {
        USER_HOLDER.remove();
    }
}
