package dev.deriou.airesume.vo;

import java.util.List;

public record PageVO<T>(
        List<T> records,
        long page,
        long size,
        long total,
        long pages
) {
    public static <T> PageVO<T> of(List<T> records, long page, long size, long total, long pages) {
        return new PageVO<>(records, page, size, total, pages);
    }
}
