import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// MessageParser.java
public class MessageParser {

    // Regex patterns for tags. We can add more as needed.
    private static final Pattern SEME_PATTERN = Pattern.compile(":20C::SEME//(.*?)$");
    // Add more patterns for other tags, e.g.,
    // private static final Pattern SOME_OTHER_TAG_PATTERN = Pattern.compile(":XXY::SOME//(.*?)$");

    public static MessageDetails parseMessage(List<String> messageLines) {
        if (messageLines == null || messageLines.isEmpty()) {
            return null;
        }

        String originalMessage = String.join(System.lineSeparator(), messageLines);
        String bankName = "";
        String semeValue = "";

        // Extract bank name from the first line (30th index, 3 characters)
        if (!messageLines.isEmpty()) {
            String firstLine = messageLines.get(0);
            if (firstLine.length() >= 33) { // 30th index means characters from 29 to 31
                bankName = firstLine.substring(29, 32); // substring(startIndex, endIndex) endIndex is exclusive
            }
        }

        // Iterate through lines to find and extract tag values
        for (String line : messageLines) {
            Matcher semeMatcher = SEME_PATTERN.matcher(line);
            if (semeMatcher.find()) {
                semeValue = semeMatcher.group(1);
            }
            // Add similar logic for other tags
            // Matcher someOtherTagMatcher = SOME_OTHER_TAG_PATTERN.matcher(line);
            // if (someOtherTagMatcher.find()) {
            //     someOtherTagValue = someOtherTagMatcher.group(1);
            // }
        }

        return new MessageDetails(originalMessage, bankName, semeValue);
    }
}
