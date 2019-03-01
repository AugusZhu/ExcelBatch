package com.hand.hls.batch;

import com.hand.hls.batch.config.ExcelExportProperties;
import com.hand.hls.batch.repository.ExcelExportInfoRepository;
import com.hand.hls.batch.runner.ExcelExportThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@Slf4j
public class HlsBatchApplication implements CommandLineRunner {
    @Autowired
    private ExcelExportProperties properties;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExcelExportInfoRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(HlsBatchApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        new ExcelExportThread(repository, jdbcTemplate, properties.getLocation()).start();
        log.info("Excel export thread is running. Location: {}", properties.getLocation());
    }
}
