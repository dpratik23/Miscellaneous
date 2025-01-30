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
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DEFINE TOPIC")) {
                    // Extract topic name
                    Pattern pattern = Pattern.compile("DEFINE TOPIC\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String topicName = matcher.group(1);
                        topics.put(topicName, new HashMap<>());
                    }
                } else if (line.startsWith("DEFINE SUB")) {
                    // Start tracking the SUB definition
                    StringBuilder subDefinition = new StringBuilder(line);
                    while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                        subDefinition.append(" ").append(line.trim());
                    }

                    // Parse the SUB definition
                    parseSubDefinition(topics, subDefinition.toString());
                }
            }
        }
        return topics;
    }

    private static void parseSubDefinition(Map<String, Map<String, String>> topics, String subDefinition) {
        // Extract subscriber queue name
        Pattern subPattern = Pattern.compile("DEFINE SUB\\(([^)]+)\\)");
        Matcher subMatcher = subPattern.matcher(subDefinition);
        if (!subMatcher.find()) {
            return; // Invalid SUB definition
        }
        String subscriberQueue = subMatcher.group(1);

        // Extract TOPICSTR
        Pattern topicStrPattern = Pattern.compile("TOPICSTR\\(([^)]+)\\)");
        Matcher topicStrMatcher = topicStrPattern.matcher(subDefinition);
        if (!topicStrMatcher.find()) {
            return; // TOPICSTR not found
        }
        String topicString = topicStrMatcher.group(1);

        // Extract DEST
        Pattern destPattern = Pattern.compile("DEST\\(([^)]+)\\)");
        Matcher destMatcher = destPattern.matcher(subDefinition);
        if (!destMatcher.find()) {
            return; // DEST not found
        }
        String destQueue = destMatcher.group(1);

        // Find the topic name associated with the topic string
        String topicName = findTopicNameByString(topics, topicString);
        if (topicName != null) {
            topics.get(topicName).put(subscriberQueue, destQueue);
        }
    }

    private static String findTopicNameByString(Map<String, Map<String, String>> topics, String topicString) {
        for (Map.Entry<String, Map<String, String>> entry : topics.entrySet()) {
            if (entry.getValue().containsKey("TOPICSTR") && entry.getValue().get("TOPICSTR").equals(topicString)) {
                return entry.getKey();
            }
        }
        return null;
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
        headerRow.createCell(2).setCellValue("Destination Queue");

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
