package org.hypergraphql.services;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import java.io.InputStream;
import java.net.URI;

public class S3Service {

    private AWSCredentials credentials(final String key, final String secret) {
        return new BasicAWSCredentials(key, secret);
    }

    public AmazonS3 buildS3(final URI uri,
                            final String key,
                            final String secret) {

        final Regions region = extractRegion(uri);

        return AmazonS3Client.builder()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials(key, secret)))
                .build();
    }

    public InputStream openS3Stream(final URI uri,
                                    final String key,
                                    final String secret) {

        S3Object s3Object = getObject(uri, key, secret);
        return s3Object.getObjectContent();
    }

    private S3Object getObject(final URI uri,
                               final String key,
                               final String secret) {

        final AmazonS3 s3 = buildS3(uri, key, secret);
        final String bucketName = extractBucket(uri);
        final String objectName = extractObjectName(uri);
        return s3.getObject(bucketName, objectName);
    }

    String extractBucket(final URI uri) {

        return uri.getPath().split("/")[1];
    }

    Regions extractRegion(final URI uri) {

        final String regionString = uri.getHost().split("\\.")[0];
        if ("S3".equals(regionString.toUpperCase())) {
            return Regions.US_EAST_1;
        }
        final String regionPart = regionString.substring(regionString.indexOf("-") + 1);
        return Regions.fromName(regionPart);
    }

    String extractObjectName(final URI uri) {

        return uri.getPath().substring(uri.getPath().indexOf("/", 1) + 1);
    }
}
