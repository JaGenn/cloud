package com.example.cloud;

import com.example.cloud.model.props.MinioProperties;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@TestConfiguration
@SpringBootTest
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);

        registry.add("MINIO_URL", () -> "http://localhost:9000");
        registry.add("MINIO_ACCESS_KEY", () -> "your-access-key");
        registry.add("MINIO_SECRET_KEY", () -> "your-secret-key");
        registry.add("MINIO_BUCKET", () -> "you-bucket-name");
    }

    @BeforeAll
    static void startContainers() {
        postgres.start();
    }

}
