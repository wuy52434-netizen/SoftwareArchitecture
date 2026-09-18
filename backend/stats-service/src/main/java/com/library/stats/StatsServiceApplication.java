package com.library.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@ComponentScan(basePackages = {"com.library.stats", "com.library.common"})
public class StatsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatsServiceApplication.class, args);
    }
}
