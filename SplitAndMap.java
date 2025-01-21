import java.io.*;
import java.util.*;

public class SplitAndMap {
    public static void main(String[] args) throws IOException {
        // Path to your file
        String filePath = "path_to_your_file.txt";

        // Read the file content
        List<String> lines = readFile(filePath);

        // Map to store dynamic keys and corresponding list of values
        Map<String, List<String>> dynamicMap = new LinkedHashMap<>();

        // Process the data
        String currentKey = null;
        List<String> valueList = null;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("+++++")) {
                // Skip separator lines
                continue;
            }
            if (currentKey == null) {
                // First non-separator line is treated as the key
                currentKey = line;
                valueList = new ArrayList<>();
            } else if (line.startsWith("Q")) {
                // If a new key is encountered, store the previous key-value pair
                dynamicMap.put(currentKey, valueList);
                currentKey = line;
                valueList = new ArrayList<>();
            } else {
                // Add the line to the current key's value list
                if (valueList != null) {
                    valueList.add(line);
                }
            }
        }
        // Add the last key-value pair
        if (currentKey != null && valueList != null) {
            dynamicMap.put(currentKey, valueList);
        }

        // Print the map
        for (Map.Entry<String, List<String>> entry : dynamicMap.entrySet()) {
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
