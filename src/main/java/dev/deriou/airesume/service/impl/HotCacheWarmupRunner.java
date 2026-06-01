package dev.deriou.airesume.service.impl;

import dev.deriou.airesume.service.HotDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class HotCacheWarmupRunner {

    private static final Logger log = LoggerFactory.getLogger(HotCacheWarmupRunner.class);

    private final HotDataService hotDataService;

    public HotCacheWarmupRunner(HotDataService hotDataService) {
        this.hotDataService = hotDataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            hotDataService.refreshAll();
            log.info("hot caches warmed up");
        } catch (RuntimeException ex) {
            log.warn("failed to warm up hot caches", ex);
        }
    }
}
