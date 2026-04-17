package com.petlife.server.bootstrap;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * 服务端启动入口。
 *
 * <p>当前阶段采用模块化单体，业务模块按垂直领域组织在 {@code com.petlife.server.modules} 下，
 * 以保证核心主链路能够在一个可控进程内快速稳定迭代。</p>
 */
@SpringBootApplication(
    scanBasePackages = "com.petlife.server",
    exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
    }
)
public class PetLifeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetLifeServerApplication.class, args);
    }
}
