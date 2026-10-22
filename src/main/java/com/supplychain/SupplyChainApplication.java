package com.supplychain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupplyChainApplication {

    private static final Logger logger = LoggerFactory.getLogger(SupplyChainApplication.class);

    public static void main(String[] args) {
        logger.info("============================================");
        logger.info("  SUPPLY CHAIN MANAGEMENT SYSTEM STARTING   ");
        logger.info("============================================");

        SpringApplication.run(SupplyChainApplication.class, args);

        logger.info("============================================");
        logger.info("  APPLICATION STARTED SUCCESSFULLY           ");
        logger.info("  Endpoints: http://localhost:8080/api       ");
        logger.info("============================================");
    }
}
