package com.hand.hls.batch.controller;

import com.hand.hls.batch.config.ExcelExportProperties;
import com.hand.hls.batch.repository.ExcelExportInfoRepository;
import com.hand.hls.batch.runner.ExcelExportThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class TestController {
    @Autowired
    private ExcelExportProperties properties;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ExcelExportInfoRepository repository;

    @RequestMapping("test")
    public String publish() {
        new ExcelExportThread(repository, jdbcTemplate, properties.getLocation()).start();
        return "结束";
    }
}
