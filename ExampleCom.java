import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.Map;

public class MQDumpComparator {

    public static void compareAndGenerateExcel(
            Map<String, Map<String, Map<String, String>>> source,
            Map<String, Map<String, Map<String, String>>> target,
            String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("MQ Comparison");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Object Type");
            headerRow.createCell(1).setCellValue("Object Name");
            headerRow.createCell(2).setCellValue("Attribute");
            headerRow.createCell(3).setCellValue("Source Value");
            headerRow.createCell(4).setCellValue("Target Value");
            headerRow.createCell(5).setCellValue("Status");

            int rowNum = 1;
            CellStyle highlightStyle = workbook.createCellStyle();
            highlightStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Compare objects
            for (String objectType : source.keySet()) {
                Map<String, Map<String, String>> sourceObjects = source.get(objectType);
                Map<String, Map<String, String>> targetObjects = target.get(objectType);

                if (targetObjects == null) {
                    // Object type missing in target
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(objectType);
                    row.createCell(5).setCellValue("Object type missing in target");
                    continue;
                }

                for (String objectName : sourceObjects.keySet()) {
                    Map<String, String> sourceAttributes = sourceObjects.get(objectName);
                    Map<String, String> targetAttributes = targetObjects.get(objectName);

                    if (targetAttributes == null) {
                        // Object missing in target
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(objectType);
                        row.createCell(1).setCellValue(objectName);
                        row.createCell(5).setCellValue("Missing in target");
                        continue;
                    }

                    for (String attribute : sourceAttributes.keySet()) {
                        String sourceValue = sourceAttributes.get(attribute);
                        String targetValue = targetAttributes.get(attribute);

                        if (!sourceValue.equals(targetValue)) {
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(0).setCellValue(objectType);
                            row.createCell(1).setCellValue(objectName);
                            row.createCell(2).setCellValue(attribute);
                            row.createCell(3).setCellValue(sourceValue);
                            row.createCell(4).setCellValue(targetValue);
                            row.createCell(5).setCellValue("Mismatch");
                            row.getCell(5).setCellStyle(highlightStyle);
                        }
                    }
                }
            }

            // Save Excel file
            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
