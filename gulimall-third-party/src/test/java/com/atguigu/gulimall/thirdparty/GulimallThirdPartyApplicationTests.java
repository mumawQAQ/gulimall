package com.atguigu.gulimall.thirdparty;

import com.google.cloud.storage.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

    @Autowired
    Storage storage;
    // The ID of your GCP project
    String projectId = "crafty-sanctum-283720";

    // The ID of your GCS bucket
    String bucketName = "gulimall_info";
    @Test
    public void testUpload() throws IOException {

        // The origin for this CORS config to allow requests from
         String origin = "*";

        // The response header to share across origins
         String responseHeader = "*";

        // The maximum amount of time the browser can make requests before it must repeat preflighted
        // requests
         Integer maxAgeSeconds = 3600;

        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);

        // See the HttpMethod documentation for other HTTP methods available:
        // https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/urlfetch/HTTPMethod
        HttpMethod method = HttpMethod.POST;

        Cors cors =
                Cors.newBuilder()
                        .setOrigins(ImmutableList.of(Cors.Origin.of(origin)))
                        .setMethods(ImmutableList.of(HttpMethod.POST,HttpMethod.GET,HttpMethod.PUT,HttpMethod.DELETE,HttpMethod.HEAD))
                        .setResponseHeaders(ImmutableList.of(responseHeader))
                        .setMaxAgeSeconds(maxAgeSeconds)
                        .build();

        bucket.toBuilder().setCors(ImmutableList.of(cors)).build().update();

        System.out.println(
                "Bucket "
                        + bucketName
                        + " was updated with a CORS config to allow GET requests from "
                        + origin
                        + " sharing "
                        + responseHeader
                        + " responses across origins");
    }

    @Test
    public void listCorsRule(){
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // Select all fields. Fields can be selected individually e.g. Storage.BucketField.NAME
        Bucket bucket =
                storage.get(bucketName, Storage.BucketGetOption.fields(Storage.BucketField.values()));

        // Print bucket metadata
        System.out.println("BucketName: " + bucket.getName());
        System.out.println("DefaultEventBasedHold: " + bucket.getDefaultEventBasedHold());
        System.out.println("DefaultKmsKeyName: " + bucket.getDefaultKmsKeyName());
        System.out.println("Id: " + bucket.getGeneratedId());
        System.out.println("IndexPage: " + bucket.getIndexPage());
        System.out.println("Location: " + bucket.getLocation());
        System.out.println("LocationType: " + bucket.getLocationType());
        System.out.println("Metageneration: " + bucket.getMetageneration());
        System.out.println("NotFoundPage: " + bucket.getNotFoundPage());
        System.out.println("RetentionEffectiveTime: " + bucket.getRetentionEffectiveTime());
        System.out.println("RetentionPeriod: " + bucket.getRetentionPeriod());
        System.out.println("RetentionPolicyIsLocked: " + bucket.retentionPolicyIsLocked());
        System.out.println("RequesterPays: " + bucket.requesterPays());
        System.out.println("SelfLink: " + bucket.getSelfLink());
        System.out.println("StorageClass: " + bucket.getStorageClass().name());
        System.out.println("TimeCreated: " + bucket.getCreateTime());
        System.out.println("VersioningEnabled: " + bucket.versioningEnabled());
        bucket.getCors().forEach(cors -> {
            System.out.println("Origin: " + cors.getOrigins());
            System.out.println("Methods: " + cors.getMethods());
            System.out.println("ResponseHeaders: " + cors.getResponseHeaders());
            System.out.println("MaxAgeSeconds: " + cors.getMaxAgeSeconds());
        });
        if (bucket.getLabels() != null) {
            System.out.println("\n\n\nLabels:");
            for (Map.Entry<String, String> label : bucket.getLabels().entrySet()) {
                System.out.println(label.getKey() + "=" + label.getValue());
            }
        }
        if (bucket.getLifecycleRules() != null) {
            System.out.println("\n\n\nLifecycle Rules:");
            for (BucketInfo.LifecycleRule rule : bucket.getLifecycleRules()) {
                System.out.println(rule);
            }
        }
    }

}
