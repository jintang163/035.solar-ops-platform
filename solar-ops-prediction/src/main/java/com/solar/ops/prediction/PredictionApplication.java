package com.solar.ops.prediction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.solar.ops.prediction",
        "com.solar.ops.admin",
        "com.solar.ops.common",
        "com.solar.ops.workorder"
})
@MapperScan(basePackages = {
        "com.solar.ops.prediction.mapper",
        "com.solar.ops.admin.mapper",
        "com.solar.ops.workorder.mapper"
})
@EnableSwagger2WebMvc
public class PredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredictionApplication.class, args);
    }
}
