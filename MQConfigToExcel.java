import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MQConfigToExcel {

    public static void main(String[] args) {
        Path inputFilePath = Paths.get("QM1_config.mqsc"); // Path to the MQSC dump file
        Path outputFilePath = Paths.get("MQ_Config_Report.xlsx"); // Output Excel file

        try {
            // Read the MQSC file
            Map<String, Map<String, String>> queues = parseMQSCFile(inputFilePath);
            Map<String, Map<String, String>> topics = parseTopicsFromMQSCFile(inputFilePath);

            // Create Excel workbook and sheets
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet1 = workbook.createSheet("Queue Details");
            Sheet sheet2 = workbook.createSheet("Remote Queue Details");
            Sheet sheet3 = workbook.createSheet("Topic Details");

            // Write Queue Details to Sheet 1
            writeQueueDetails(sheet1, queues);

            // Write Remote Queue Details to Sheet 2
            writeRemoteQueueDetails(sheet2, queues);

            // Write Topic Details to Sheet 3
            writeTopicDetails(sheet3, topics);

            // Write the output to an Excel file
            try (OutputStream fileOut = Files.newOutputStream(outputFilePath)) {
                workbook.write(fileOut);
            }

            System.out.println("Excel file created successfully: " + outputFilePath);

            // Close the workbook
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Map<String, String>> parseMQSCFile(Path filePath) throws IOException {
        Map<String, Map<String, String>> queues = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
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

    private static Map<String, Map<String, String>> parseTopicsFromMQSCFile(Path filePath) throws IOException {
        Map<String, Map<String, String>> topics = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            String currentTopic = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DEFINE TOPIC")) {
                    // Extract topic name
                    Pattern pattern = Pattern.compile("DEFINE TOPIC\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        currentTopic = matcher.group(1);
                        topics.put(currentTopic, new HashMap<>());
                    }
                } else if (line.startsWith("DEFINE SUB")) {
                    // Extract subscriber details
                    Pattern pattern = Pattern.compile("DEFINE SUB\\(([^)]+)\\) TOPICOBJ\\(([^)]+)\\) DESTQ\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String subscriberQueue = matcher.group(1);
                        String topicName = matcher.group(2);
                        String destQueue = matcher.group(3);

                        if (!topics.containsKey(topicName)) {
                            topics.put(topicName, new HashMap<>());
                        }
                        topics.get(topicName).put(subscriberQueue, destQueue);
                    }
                }
            }
        }
        return topics;
    }

    private static String extractValue(String line) {
        // Pattern to match both formats: PROP('value') and PROP(value)
        Pattern pattern = Pattern.compile("\\b(\\w+)\\([']?([^')]+)[']?\\)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(2); // Return the value (without quotes if present)
        }
        return ""; // Return an empty string if no match is found
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

    private static void writeTopicDetails(Sheet sheet, Map<String, Map<String, String>> topics) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Topic Name");
        headerRow.createCell(1).setCellValue("Subscriber Queue Name");
        headerRow.createCell(2).setCellValue("Subscriber Queue Manager");

        for (Map.Entry<String, Map<String, String>> entry : topics.entrySet()) {
            String topicName = entry.getKey();
            for (Map.Entry<String, String> subscriber : entry.getValue().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(topicName);
                row.createCell(1).setCellValue(subscriber.getKey());
                row.createCell(2).setCellValue(subscriber.getValue());
            }
        }
    }
}
