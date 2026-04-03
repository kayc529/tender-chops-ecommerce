package com.kaycheung.product_service.job.product_stale_image_cleanup;

import com.kaycheung.product_service.config.properties.ScheduledJobsProperties;
import com.kaycheung.product_service.entity.ProductStaleImageCleanupTask;
import com.kaycheung.product_service.repository.ProductStaleImageCleanupTaskRepository;
import com.kaycheung.product_service.service.AwsS3Service;
import com.kaycheung.product_service.service.ProductStaleImageCleanupTaskPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStaleImageCleanupWorker {
    private final ProductStaleImageCleanupTaskRepository productStaleImageCleanupTaskRepository;
    private final ProductStaleImageCleanupTaskPersistService productStaleImageCleanupTaskPersistService;
    private final AwsS3Service awsS3Service;
    private final ScheduledJobsProperties scheduledJobsProperties;

    public void deleteImages() {
        int batchSize = scheduledJobsProperties.getProductStaleImageCleanup().getBatchSize();
        List<ProductStaleImageCleanupTask> tasks = productStaleImageCleanupTaskRepository.findProcessedAtIsNull(PageRequest.of(0, batchSize));

        if (tasks.isEmpty()) {
            log.debug("No product stale image cleanup tasks found.");
            return;
        }

        log.info("Starting product stale image cleanup. taskCount={}", tasks.size());

        for (ProductStaleImageCleanupTask task : tasks) {
            try {
                awsS3Service.removeS3Objects(task.getProductId(), task.getImageKey(), task.getThumbnailKey());
                productStaleImageCleanupTaskPersistService.markTaskAsProcessed(task.getId(), Instant.now());
            } catch (Exception ex) {
                log.error("Cleanup failed. taskId={}, productId={}, imageKey={}, thumbnailKey={}, error={}",
                        task.getId(), task.getProductId(), task.getImageKey(), task.getThumbnailKey(), ex.getMessage(), ex);

                productStaleImageCleanupTaskPersistService.increaseTaskAttemptCount(task.getId(), ex.getMessage());
            }
        }
    }
}
