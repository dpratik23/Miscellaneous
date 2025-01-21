import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SplitAndMap {
    public static void main(String[] args) throws IOException {
        // Path to your file
        String filePath = "path_to_your_file.txt";

        // Read the file content
        List<String> lines = readFile(filePath);

        // Map to store Qm as key and list of QueueInfo as value
        Map<String, List<String>> qmMap = new LinkedHashMap<>();

        // Process the data
        String currentQm = null;
        List<String> queueInfoList = null;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("+++++")) {
                // Skip separator lines
                continue;
            }
            if (line.startsWith("Qm")) {
                // When a new Qm is found, save the previous Qm data if any
                if (currentQm != null && queueInfoList != null) {
                    qmMap.put(currentQm, queueInfoList);
                }
                // Start a new Qm section
                currentQm = line;
                queueInfoList = new ArrayList<>();
            } else {
                // Add QueueInfo to the current Qm's list
                if (queueInfoList != null) {
                    queueInfoList.add(line);
                }
            }
        }
        // Add the last Qm data
        if (currentQm != null && queueInfoList != null) {
            qmMap.put(currentQm, queueInfoList);
        }

        // Print the map
        for (Map.Entry<String, List<String>> entry : qmMap.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            System.out.println("Values: " + entry.getValue());
        }
    }

    // Method to read file into a list of strings
    private static List<String> readFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
