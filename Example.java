import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class MQDumpParser {

    public static Map<String, Map<String, Map<String, String>>> parseDump(String filePath) throws Exception {
        Map<String, Map<String, Map<String, String>>> dumpData = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentObjectType = null;
            String currentObjectName = null;
            Map<String, String> currentAttributes = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("ALTER") || line.startsWith("DEFINE") || line.startsWith("SET")) {
                    // Extract object type and name
                    String[] parts = line.split("\\s+");
                    currentObjectType = parts[1]; // e.g., QLOCAL, CHANNEL
                    currentObjectName = extractObjectName(line); // Extract name from parentheses

                    // Initialize attributes map
                    currentAttributes = new HashMap<>();
                    dumpData.computeIfAbsent(currentObjectType, k -> new HashMap<>()).put(currentObjectName, currentAttributes);
                } else if (line.startsWith("+") && currentAttributes != null) {
                    // Extract attributes
                    String[] keyValue = line.split("\\(");
                    if (keyValue.length >= 2) {
                        String key = keyValue[0].trim().substring(1).trim(); // e.g., DEFPSIST
                        String value = keyValue[1].split("\\)")[0].replace("'", ""); // e.g., YES
                        currentAttributes.put(key, value);
                    }
                }
            }
        }
        return dumpData;
    }

    private static String extractObjectName(String line) {
        // Extract object name from parentheses
        int start = line.indexOf('(');
        int end = line.indexOf(')');
        if (start != -1 && end != -1) {
            return line.substring(start + 1, end).replace("'", "");
        }
        return null;
    }
}
