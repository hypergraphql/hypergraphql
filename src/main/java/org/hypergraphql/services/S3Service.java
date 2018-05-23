package org.hypergraphql.services;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.net.URI;

public class S3Service {

    private AWSCredentials credentials(final String key, final String secret) {
        return new BasicAWSCredentials(key, secret);
    }

    public AmazonS3 buildS3(final URI uri, final String key, final String secret) {

        final Regions region = extractRegion(uri);

        return AmazonS3Client.builder()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials(key, secret)))
                .build();
    }

    String extractBucket(final URI uri) {

        return uri.getPath().split("/")[1];
    }

    Regions extractRegion(final URI uri) {

        final String regionString = uri.getHost().split("\\.")[0];
        switch(regionString.toUpperCase()) {

            case ("S3"):
                return Regions.US_EAST_1;
            default:
                final String regionPart = regionString.substring(regionString.indexOf("-") + 1);
                return Regions.fromName(regionPart);
        }
    }

    String extractObjectName(final URI uri) {

        return uri.getPath().substring(uri.getPath().indexOf("/", 1) + 1);
    }
}
