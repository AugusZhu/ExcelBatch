package com.hand.hls.batch.runner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hand.hls.batch.dto.ExcelExportInfo;
import com.hand.hls.batch.repository.ExcelExportInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExcelExportThread extends Thread {
    public static final ThreadLocal<ExcelExportThread> current = new ThreadLocal<>();
    private static final long DEFAULT_SLEEP_TIME = 5000L;

    private static final String STATUS_NEW = "NEW";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_ERROR = "ERROR";
    private static final String STATUS_FINISH = "FINISH";
    public static boolean exit = false;

    private ExcelExportInfoRepository excelExportInfoRepository;
    private JdbcTemplate jdbcTemplate;
    private File savePath;

    public ExcelExportThread(ExcelExportInfoRepository excelExportInfoRepository, JdbcTemplate jdbcTemplate, String filePath) {
        super();
        this.excelExportInfoRepository = excelExportInfoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.savePath = new File(filePath);
        current.set(this);
    }

    @Override
    public void run() {
        int maxRowNumber = 1000000;
        ExcelExportInfo info = new ExcelExportInfo();
        info.setStatus(STATUS_NEW);
//        1. 找到最新的一条未导出的数据记录
//        2. 获取sql和列配置
//        3. 通过sql查询出结果，并根据列配置生成excel文件
//        4. 更新文件路径以及生成状态
        while (true) {
            if(exit){
                break;
            }
            Optional<ExcelExportInfo> firstOne = excelExportInfoRepository.findFirstByStatusOrderByCreationDate(STATUS_NEW);
            if (!firstOne.isPresent()) {
//                没数据时休眠一段时间
                try {
                    Thread.sleep(DEFAULT_SLEEP_TIME);
                    continue;
                } catch (InterruptedException e) {
                    // pass
                }
            }
            Date now = new Date(System.currentTimeMillis());
            ExcelExportInfo excelExportInfo = firstOne.get();
            String sqlContent = excelExportInfo.getSqlContent();
            String columnInfo = excelExportInfo.getColumnInfo();
            if (StringUtils.isEmpty(sqlContent) || StringUtils.isEmpty(columnInfo)) {
                excelExportInfo.setStatus(STATUS_ERROR);
                excelExportInfo.setLastUpdateDate(now);
                excelExportInfoRepository.save(excelExportInfo);
                continue;
            }

//            开始生成文件
            try {
                JSONArray columns = JSON.parseArray(columnInfo);
//            修改状态为运行中
                excelExportInfo.setStatus(STATUS_RUNNING);
                excelExportInfoRepository.save(excelExportInfo);

//                初始化excel
                SXSSFWorkbook wb = new SXSSFWorkbook(50);
                CellStyle dateFormat = wb.createCellStyle();
                dateFormat.setDataFormat(wb.createDataFormat().getFormat("yyyy-MM-DD HH:mm"));
                dateFormat.setAlignment(HorizontalAlignment.CENTER);

                DataFormat dataFormat = wb.createDataFormat();
                CellStyle textStyle = wb.createCellStyle();
                textStyle.setDataFormat(dataFormat.getFormat("@"));

                final AtomicInteger rowCount = new AtomicInteger(1);
                final AtomicInteger sheetIndex = new AtomicInteger(1);
                final SXSSFSheet[] sheet = {wb.createSheet()};

                createHeaderRow(columns, wb, sheet[0]);

                jdbcTemplate.query(sqlContent, (resultSet, rowNum) -> sheet[0] = createSheet(wb, sheet[0], resultSet, rowCount, sheetIndex, maxRowNumber, columns, dateFormat, textStyle));

//                导出完成，更新状态及文件路径
                String uuid = UUID.randomUUID().toString();
                String fileName = "excel" + uuid;
                File file = new File(savePath, fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                wb.write(fileOutputStream);
                excelExportInfo.setSavePath(file.getAbsolutePath());
                excelExportInfo.setStatus(STATUS_FINISH);
                excelExportInfo.setLastUpdateDate(now);
                excelExportInfoRepository.save(excelExportInfo);

            } catch (Exception e) {
                log.error("Export excel file failed.", e);
                excelExportInfo.setStatus(STATUS_ERROR);
                excelExportInfoRepository.save(excelExportInfo);
            }
        }
    }

    private SXSSFSheet createSheet(SXSSFWorkbook wb, SXSSFSheet sheet, Object object, AtomicInteger count,
                                   AtomicInteger rowIndex, int rowMaxNumber, JSONArray columnInfos,
                                   CellStyle dateFormat, CellStyle textStyle) {
        if (count.get() % rowMaxNumber == 0) {
            sheet = wb.createSheet();
            createHeaderRow(columnInfos, wb, sheet);
            rowIndex.set(0);
        }
        count.getAndIncrement();
        SXSSFRow row = sheet.createRow(rowIndex.getAndIncrement());

        createRow(columnInfos, object, row, dateFormat, textStyle);
        return sheet;
    }

    private void createRow(JSONArray columnInfos, Object object, SXSSFRow row, CellStyle dateFormat, CellStyle textStyle) {
        for (int columnIndex = 0; columnIndex < columnInfos.size(); columnIndex++) {
            Object fieldObject = null;
            JSONObject columnInfo = columnInfos.getJSONObject(columnIndex);
            String columnName = columnInfo.getString("name");
            try {
                if (object instanceof ResultSet) {
                    fieldObject = ((ResultSet) object).getObject(columnName);
                } else if (object instanceof Map) {
                    fieldObject = ((Map) object).get(columnName);
                } else {
                    fieldObject = PropertyUtils.getProperty(object, columnName);
                }

            } catch (Exception e) {
                log.trace("Get value from object.", e);
            }
//            String type = columnInfos.get(columnIndex).getType();
//            FIXME: modelOutput中提供的列配置中没有类型，暂时全部当做字符串
            String type = "STRING";

            SXSSFCell cell = row.createCell(columnIndex);

            if (null == fieldObject) {
                cell.setCellType(CellType.STRING);
                cell.setCellValue((String) null);
            } else {
                switch (type.toUpperCase(Locale.CHINA)) {
                    case "NUMBER":
                    case "FLOAT":
                        cell.setCellStyle(textStyle);
                        cell.setCellValue(fieldObject.toString());
                        break;
                    case "DOUBLE":
                        cell.setCellStyle(textStyle);
                        cell.setCellValue(fieldObject.toString());
                        break;
                    case "INT":
                    case "INTEGER":
                    case "LONG":
                        cell.setCellStyle(textStyle);
                        cell.setCellValue(fieldObject.toString());
                        break;
                    case "DATE":
                        cell.setCellStyle(dateFormat);
                        cell.setCellValue((Date) fieldObject);
                        break;
                    case "BOOLEAN":
                        cell.setCellType(CellType.BOOLEAN);
                        if (fieldObject instanceof Boolean) {
                            cell.setCellValue((Boolean) fieldObject);
                        } else {
                            cell.setCellValue(fieldObject.toString());
                        }
                        break;
                    default:
                        cell.setCellType(CellType.STRING);
                        String value = fieldObject.toString();
                        if(value != null && value.contains("00:00:00.0")){
                            value = value.replace("00:00:00.0", "");
                        }
                        cell.setCellValue(value);
                        break;
                }
            }
        }
    }

    private void createHeaderRow(JSONArray columnInfos, SXSSFWorkbook wb, SXSSFSheet sheet) {
        SXSSFRow firstRow = sheet.createRow(0);
        // 设置列字体align
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < columnInfos.size(); i++) {
            JSONObject jsonObject = columnInfos.getJSONObject(i);
            SXSSFCell firstCell = firstRow.createCell(i);
            firstCell.setCellValue(jsonObject.getString("prompt"));
            // 设置列宽度
            sheet.setColumnWidth(i, jsonObject.getIntValue("width")  * 16);
            firstCell.setCellStyle(cellStyle);
        }
    }


}
