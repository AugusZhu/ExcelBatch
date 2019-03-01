package com.hand.hls.batch.dto;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "excel_export_info")
@Getter
@Setter
public class ExcelExportInfo {
    @Id
    @Column(name = "export_id")
    private Long exportId;
    @Column(name = "sql_content")
    private String sqlContent;
    @Column(name = "column_info")
    private String columnInfo;
    @Column(name = "save_path")
    private String savePath;
    @Column(name = "status")
    private String status;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "creation_date")
    private Date creationDate;
    @Column(name = "last_update_date")
    private Date lastUpdateDate;
}
