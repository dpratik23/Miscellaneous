import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MQConfigToExcel {

    public static void main(String[] args) {
        String inputFilePath = "QM1_config.mqsc"; // Path to the MQSC dump file
        String outputFilePath = "MQ_Config_Report.xlsx"; // Output Excel file

        try {
            // Read the MQSC file
            Map<String, Map<String, String>> queues = parseMQSCFile(inputFilePath);

            // Create Excel workbook and sheets
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet1 = workbook.createSheet("Queue Details");
            Sheet sheet2 = workbook.createSheet("Remote Queue Details");

            // Write Queue Details to Sheet 1
            writeQueueDetails(sheet1, queues);

            // Write Remote Queue Details to Sheet 2
            writeRemoteQueueDetails(sheet2, queues);

            // Write the output to an Excel file
            try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
                workbook.write(fileOut);
            }

            System.out.println("Excel file created successfully: " + outputFilePath);

            // Close the workbook
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Map<String, String>> parseMQSCFile(String filePath) throws IOException {
        Map<String, Map<String, String>> queues = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentQueue = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DEFINE QLOCAL") || line.startsWith("DEFINE QREMOTE")) {
                    // Extract queue name
                    Pattern pattern = Pattern.compile("DEFINE (QLOCAL|QREMOTE)\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        currentQueue = matcher.group(2);
                        queues.put(currentQueue, new HashMap<>());
                        queues.get(currentQueue).put("TYPE", matcher.group(1));
                    }
                } else if (currentQueue != null) {
                    // Extract queue properties
                    if (line.contains("MAXDEPTH")) {
                        queues.get(currentQueue).put("MAXDEPTH", extractValue(line));
                    } else if (line.contains("BOQNAME")) {
                        queues.get(currentQueue).put("BOQNAME", extractValue(line));
                    } else if (line.contains("RNAME")) {
                        queues.get(currentQueue).put("RNAME", extractValue(line));
                    } else if (line.contains("RQMNAME")) {
                        queues.get(currentQueue).put("RQMNAME", extractValue(line));
                    }
                }
            }
        }
        return queues;
    }

    private static String extractValue(String line) {
        Pattern pattern = Pattern.compile("\\b(\\w+)\\('([^']+)'\\)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    private static void writeQueueDetails(Sheet sheet, Map<String, Map<String, String>> queues) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Queue Name");
        headerRow.createCell(1).setCellValue("Queue Type");
        headerRow.createCell(2).setCellValue("Max Depth");
        headerRow.createCell(3).setCellValue("Backout Queue");

        for (Map.Entry<String, Map<String, String>> entry : queues.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().get("TYPE"));
            row.createCell(2).setCellValue(entry.getValue().getOrDefault("MAXDEPTH", "N/A"));
            row.createCell(3).setCellValue(entry.getValue().getOrDefault("BOQNAME", "N/A"));
        }
    }

    private static void writeRemoteQueueDetails(Sheet sheet, Map<String, Map<String, String>> queues) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Remote Queue Name");
        headerRow.createCell(1).setCellValue("Remote Queue Manager");
        headerRow.createCell(2).setCellValue("Local Queue Name on Remote QM");

        for (Map.Entry<String, Map<String, String>> entry : queues.entrySet()) {
            if ("QREMOTE".equals(entry.getValue().get("TYPE"))) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().getOrDefault("RQMNAME", "N/A"));
                row.createCell(2).setCellValue(entry.getValue().getOrDefault("RNAME", "N/A"));
            }
        }
    }
}
