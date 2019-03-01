package com.hand.hls.batch.repository;

import com.hand.hls.batch.dto.ExcelExportInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExcelExportInfoRepository extends JpaRepository<ExcelExportInfo, Long> {
    Optional<ExcelExportInfo> findFirstByStatusOrderByCreationDate(String status);
}
