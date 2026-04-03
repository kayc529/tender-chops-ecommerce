package com.kaycheung.product_service.config;

import com.kaycheung.product_service.config.properties.AwsS3Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Bean
    public S3Presigner s3Presigner(AwsS3Properties awsS3Properties) {
        return S3Presigner.builder()
                .region(Region.of(awsS3Properties.getRegion()))
                .build();
    }

    @Bean
    public S3Client s3Client(AwsS3Properties awsS3Properties) {
        return S3Client.builder()
                .region(Region.of(awsS3Properties.getRegion()))
                .build();
    }
}
