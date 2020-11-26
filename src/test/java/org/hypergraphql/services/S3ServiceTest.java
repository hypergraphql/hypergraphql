package org.hypergraphql.services;

import com.amazonaws.regions.Regions;
import java.net.URI;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3ServiceTest {

    @Test
    void build_client_happy_path_east1() throws Exception {
        val service = new S3Service();
        val uri = new URI("s3://s3.amazon.com");
        val key = "key";
        val secret = "secret";
        val actual = service.buildS3(uri, key, secret);
        assertEquals(actual.getRegionName(), Regions.US_EAST_1.getName());
    }

    @Test
    void build_client_happy_path_west1() throws Exception {
        val service = new S3Service();
        val uri = new URI("s3://s3-us-west-1.amazon.com");
        val key = "key";
        val secret = "secret";
        val actual = service.buildS3(uri, key, secret);
        assertEquals(actual.getRegionName(), Regions.US_WEST_1.getName());
    }
}
