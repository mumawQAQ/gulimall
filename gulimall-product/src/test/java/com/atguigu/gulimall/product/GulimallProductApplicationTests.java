package com.atguigu.gulimall.product;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTests {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void redisTest() {
        redisTemplate.opsForValue().set("test", "hello" + UUID.randomUUID().toString());
        System.out.println(redisTemplate.opsForValue().get("test"));
    }
}
