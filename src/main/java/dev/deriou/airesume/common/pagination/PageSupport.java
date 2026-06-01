package dev.deriou.airesume.common.pagination;

public final class PageSupport {

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 100L;

    private PageSupport() {
    }

    public static long page(long page) {
        return page > 0 ? page : DEFAULT_PAGE;
    }

    public static long size(long size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
