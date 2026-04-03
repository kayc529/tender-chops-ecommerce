package com.kaycheung.product_service.service;

import com.kaycheung.product_service.entity.ProductStaleImageCleanupTask;
import com.kaycheung.product_service.repository.ProductStaleImageCleanupTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductStaleImageCleanupTaskPersistService {

    private final ProductStaleImageCleanupTaskRepository productStaleImageCleanupTaskRepository;

    @Transactional
    public void createTask(UUID productId, String imageKey, String thumbnailKey) {
        ProductStaleImageCleanupTask task = new ProductStaleImageCleanupTask();
        task.setProductId(productId);
        task.setImageKey(imageKey);
        task.setThumbnailKey(thumbnailKey);
        task.setAttemptCount(0);
        task.setLastError(null);
        task.setProcessedAt(null);
        productStaleImageCleanupTaskRepository.save(task);
    }

    @Transactional
    public void markTaskAsProcessed(UUID taskId, Instant now) {
        productStaleImageCleanupTaskRepository.markTaskAsProcessed(taskId, now);
    }

    @Transactional
    public void increaseTaskAttemptCount(UUID taskId, String errorMessage)
    {
        productStaleImageCleanupTaskRepository.increaseAttemptCount(taskId, errorMessage);
    }
}
