package com.superpatient;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.superpatient.mapper")
public class SuperPatientApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperPatientApplication.class, args);
    }
}
