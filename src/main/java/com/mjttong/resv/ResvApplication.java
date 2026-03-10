package com.mjttong.resv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResvApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResvApplication.class, args);
    }

}
