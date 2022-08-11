package com.atguigu.gulimall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GuliESConfig {
    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        COMMON_OPTIONS = builder.build();
    }

    /**
     * final String hostname, final int port, final String scheme
     */
    @Bean
    public RestHighLevelClient esRestClient() {

        RestClientBuilder builder = null;
        // 可以指定多个es
        builder = RestClient.builder(new HttpHost("192.168.56.10", 9200, "http"));

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
