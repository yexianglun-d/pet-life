package com.petlife.server.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 服务端启动入口。
 *
 * <p>当前阶段采用模块化单体，业务模块按垂直领域组织在 {@code com.petlife.server.modules} 下，
 * 以保证核心主链路能够在一个可控进程内快速稳定迭代。</p>
 */
@SpringBootApplication(
    scanBasePackages = "com.petlife.server",
    exclude = UserDetailsServiceAutoConfiguration.class
)
@MapperScan(
    basePackages = "com.petlife.server.modules",
    annotationClass = Mapper.class
)
public class PetLifeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetLifeServerApplication.class, args);
    }
}
