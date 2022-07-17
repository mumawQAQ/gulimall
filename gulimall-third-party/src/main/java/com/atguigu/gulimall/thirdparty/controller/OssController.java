package com.atguigu.gulimall.thirdparty.controller;

import com.atguigu.common.utils.R;
import com.google.cloud.storage.*;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class OssController {
    @Autowired
    Storage storage;
    @Value("${spring.cloud.gcp.storage.bucketName}")
    String bucketName;

    @PostMapping("/oss/uploadFile/{fileName}")
    public R uploadFile(@RequestParam("file") MultipartFile file,@PathVariable("fileName") String fileName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.createFrom(blobInfo, new ByteArrayInputStream(file.getBytes()));
        return R.ok();
    }




    @RequestMapping("/oss/upload/{fileName}")
    public R generateV4PutObjectSignedUrl(
            @PathVariable("fileName") String objectName) throws StorageException {

        objectName = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + '/' + objectName;
        // Define Resource
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

        // Generate Signed URL
        Map<String, String> extensionHeaders = new HashMap<>();
        extensionHeaders.put("Content-Type", "application/octet-stream");

        URL url =
                storage.signUrl(
                        blobInfo,
                        15,
                        TimeUnit.MINUTES,
                        Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                        Storage.SignUrlOption.withExtHeaders(extensionHeaders),
                        Storage.SignUrlOption.withV4Signature());
        return R.ok().put("url", url.toString());
    }
}
