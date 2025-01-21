import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ExcelGeneratorService {

    // Headers for the Excel file
    private static final String[] HEADERS = {
            "Queues", "Queue Type", "T", "Ev", "aqm", "upqm", "rcvrCh", "dQm", "tq", "SDC", "dqm"
    };

    // Method to read the text file and parse the data
    public List<Map<String, String>> readTextFile(String filePath) throws IOException {
        List<Map<String, String>> queueDataList = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        // Assuming each line contains data in a specific format
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                Map<String, String> queueData = new HashMap<>();
                // Parse line into queue data (adjust based on file format)
                String[] parts = line.split("\\s+"); // Assuming space-separated values
                queueData.put("Queues", parts[0]);
                queueData.put("Queue Type", parts[1]);
                queueData.put("T", parts[2]);
                queueData.put("Ev", parts[3]);
                queueData.put("aqm", parts[4]);
                queueData.put("upqm", parts[5]);
                queueData.put("rcvrCh", parts[6]);
                queueData.put("dQm", parts[7]);
                queueData.put("tq", parts[8]);
                queueData.put("SDC", parts[9]);
                queueData.put("dqm", parts[10]);
                queueDataList.add(queueData);
            }
        }
        return queueDataList;
    }

    // Method to generate Excel file
    public void generateExcel(String outputPath, List<Map<String, String>> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Queue Info");

        // Create header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }

        // Populate data rows
        int rowNum = 1;
        for (Map<String, String> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData.getOrDefault(HEADERS[i], ""));
            }
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    // Main method for testing
    public static void main(String[] args) throws IOException {
        ExcelGeneratorService service = new ExcelGeneratorService();
        String inputFilePath = "path/to/your/input.txt";
        String outputFilePath = "path/to/your/output.xlsx";

        List<Map<String, String>> data = service.readTextFile(inputFilePath);
        service.generateExcel(outputFilePath, data);

        System.out.println("Excel file generated successfully at: " + outputFilePath);
    }
}
