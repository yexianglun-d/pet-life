package com.petlife.server.modules.location.config;

import com.petlife.server.modules.location.service.AmapWebServiceProperties;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 高德 Web 服务 HTTP Client 配置。
 */
@Configuration
public class AmapWebServiceClientConfig {

    @Bean
    @Qualifier("amapRestClient")
    public RestClient amapRestClient(
        RestClientBuilderConfigurer builderConfigurer,
        AmapWebServiceProperties properties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return builderConfigurer.configure(RestClient.builder())
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }
}
