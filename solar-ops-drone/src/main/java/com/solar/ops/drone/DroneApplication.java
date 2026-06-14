package com.solar.ops.drone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.solar.ops.drone",
        "com.solar.ops.admin",
        "com.solar.ops.common"
})
@MapperScan(basePackages = {
        "com.solar.ops.drone.mapper",
        "com.solar.ops.admin.mapper"
})
@EnableSwagger2WebMvc
@EnableAsync
public class DroneApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroneApplication.class, args);
    }
}
