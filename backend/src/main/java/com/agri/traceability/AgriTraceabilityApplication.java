package com.agri.traceability;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@SpringBootApplication
@MapperScan("com.agri.traceability.mapper")
@EnableSwagger2WebMvc
public class AgriTraceabilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriTraceabilityApplication.class, args);
    }
}
