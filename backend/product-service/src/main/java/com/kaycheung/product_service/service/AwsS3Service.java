package com.kaycheung.product_service.service;

import com.kaycheung.product_service.config.properties.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {
    private final ProductStaleImageCleanupTaskPersistService productStaleImageCleanupTaskPersistService;
    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    public void removeS3Objects(UUID productId, String imageKey, String thumbnailKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(awsS3Properties.getOriginalBucketName())
                .key(imageKey)
                .build());

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(awsS3Properties.getThumbnailBucketName())
                .key(thumbnailKey)
                .build());

        log.info("Deleted S3 objects successfully for productId={}, imageKey={}, thumbnailKey={}",
                productId, imageKey, thumbnailKey);

    }
}
