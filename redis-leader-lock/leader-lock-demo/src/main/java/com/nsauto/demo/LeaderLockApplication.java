package com.nsauto.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeaderLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderLockApplication.class, args);
    }
}