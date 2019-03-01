package com.hand.hls.batch.controller;

import com.hand.hls.batch.dto.ExcelExportInfo;
import com.hand.hls.batch.repository.ExcelExportInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Controller
public class ExcelController {
    @Autowired
    private ExcelExportInfoRepository excelExportInfoRepository;

    @RequestMapping("/excel/download")
    public ResponseEntity<InputStreamResource> download(ExcelExportInfo info) {
        Optional<ExcelExportInfo> exportInfo = excelExportInfoRepository.findById(info.getExportId());
        if (exportInfo.isPresent()) {
            ExcelExportInfo excelExportInfo = exportInfo.get();
            String savePath = excelExportInfo.getSavePath();
            if (StringUtils.isEmpty(savePath)) {
                return ResponseEntity.noContent().build();
//                throw new RuntimeException("File not exist");
            }
            File file = new File(savePath);
            if (!file.exists()) {
                return ResponseEntity.noContent().build();
            }
            FileSystemResource fileSystemResource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", fileSystemResource.getFilename()));
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            try {
                return ResponseEntity
                        .ok()
                        .headers(headers)
                        .contentLength(fileSystemResource.contentLength())
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(new InputStreamResource(fileSystemResource.getInputStream()));
            } catch (IOException e) {
                return ResponseEntity.noContent().build();
            }
        }

        return ResponseEntity.noContent().build();
    }
}
