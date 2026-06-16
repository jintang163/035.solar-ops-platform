package com.solar.ops.workorder.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.solar.ops.workorder.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://127.0.0.1:9200}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private Duration connectTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private Duration socketTimeout;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        log.info("初始化Elasticsearch客户端，uris: {}", uris);
        ClientConfiguration.MaybeSecureClient builder = ClientConfiguration.builder()
                .connectedTo(uris.replace("http://", "").replace("https://", ""))
                .withConnectTimeout(connectTimeout)
                .withSocketTimeout(socketTimeout);

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            ((ClientConfiguration.TerminalClientConfigurationBuilder) builder)
                    .withBasicAuth(username, password);
        }

        ClientConfiguration configuration = ((ClientConfiguration.TerminalClientConfigurationBuilder) builder).build();
        return RestClients.create(configuration).rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        return new ElasticsearchRestTemplate(elasticsearchClient());
    }
}
