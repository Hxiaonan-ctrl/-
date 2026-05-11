package com.example.crm.common;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.util.List;

public class ExcelUtils {

    public static void exportExcel(OutputStream out, String sheetName, List<String> head, List<List<String>> data) throws Exception {
        // try-with-resources 自动关闭资源，避免内存泄漏
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            sheet.setDefaultColumnWidth(15); // 默认列宽

            // 表头样式
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headFont.setFontHeightInPoints((short) 12);
            headStyle.setFont(headFont);
            headStyle.setAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 写入表头
            Row headRow = sheet.createRow(0);
            for (int i = 0; i < head.size(); i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(head.get(i));
                cell.setCellStyle(headStyle);
            }

            // 写入数据
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> rowData = data.get(rowIndex);
                for (int i = 0; i < rowData.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i));
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < head.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 15 * 256));
            }

            // 写入输出流
            workbook.write(out);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}