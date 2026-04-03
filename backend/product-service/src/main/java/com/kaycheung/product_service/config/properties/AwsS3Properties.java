package com.kaycheung.product_service.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
@Getter
@Setter
public class AwsS3Properties {
    @NotNull
    private String region;
    @NotNull
    private String originalBucketName;
    @NotNull
    private String thumbnailBucketName;

    @Min(5)
    private long presignDurationMinutes = 5;
}
