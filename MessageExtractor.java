import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MessageExtractor {

    private static final String START_TAG = "*ADM";
    private static final Pattern END_TAG_PATTERN = Pattern.compile("/ADM0000(.{5})$");

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java MessageExtractor <inputFilePath> <outputCsvFilePath>");
            return;
        }

        String inputFilePath = args[0];
        String outputCsvFilePath = args[1];

        List<MessageDetails> extractedMessages = new ArrayList<>();
        boolean inMessageBlock = false;
        List<String> currentMessageLines = new ArrayList<>();

        // Create a single instance of MessageParser
        MessageParser parser = new MessageParser();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith(START_TAG)) {
                    if (inMessageBlock) {
                        extractedMessages.add(parser.parseMessage(new ArrayList<>(currentMessageLines)));
                        currentMessageLines.clear();
                    }
                    inMessageBlock = true;
                    currentMessageLines.add(line);
                } else if (inMessageBlock && END_TAG_PATTERN.matcher(line.trim()).matches()) {
                    currentMessageLines.add(line);
                    extractedMessages.add(parser.parseMessage(new ArrayList<>(currentMessageLines)));
                    currentMessageLines.clear();
                    inMessageBlock = false;
                } else if (inMessageBlock) {
                    currentMessageLines.add(line);
                }
            }

            if (inMessageBlock && !currentMessageLines.isEmpty()) {
                 extractedMessages.add(parser.parseMessage(new ArrayList<>(currentMessageLines)));
            }

        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsvFilePath))) {
            if (!extractedMessages.isEmpty()) {
                writer.println(extractedMessages.get(0).toCsvHeader());
            }
            for (MessageDetails details : extractedMessages) {
                writer.println(details.toCsvLine());
            }
            System.out.println("Successfully extracted " + extractedMessages.size() + " messages to " + outputCsvFilePath);
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }
}
