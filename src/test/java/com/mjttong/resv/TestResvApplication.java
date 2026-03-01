package com.mjttong.resv;

import org.springframework.boot.SpringApplication;

public class TestResvApplication {

    public static void main(String[] args) {
        SpringApplication.from(ResvApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
