import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; // To preserve insertion order of blocks
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    private static final Pattern MESSAGE_TYPE_PATTERN = Pattern.compile("^(.*?):(\\d{3}):(.*)");
    private static final Pattern BLOCK_START_PATTERN = Pattern.compile(":16R:([A-Z0-9]+)$");
    private static final Pattern BLOCK_END_PATTERN = Pattern.compile(":16S:([A-Z0-9]+)$");

    private final Map<String, Pattern> tagPatterns;

    public MessageParser() {
        this.tagPatterns = new HashMap<>();
        tagPatterns.put("SEME", Pattern.compile(":20C::SEME//(.*?)$"));
        tagPatterns.put("SAFE", Pattern.compile(":97A::SAFE//(.*?)$"));
        tagPatterns.put("PSET", Pattern.compile(":98A::PSET//(.*?)$"));
        tagPatterns.put("SOMEOTHERTAG", Pattern.compile(":XXA::SOMEOTHER//(.*?)$"));
    }

    public MessageDetails parseMessage(List<String> messageLines) {
        if (messageLines == null || messageLines.isEmpty()) {
            return null;
        }

        String originalMessage = String.join(System.lineSeparator(), messageLines);
        String bankName = "";
        String messageType = "";

        // Temporarily store all extracted tags, categorized by the block they were found in.
        // LinkedHashMap preserves the order in which blocks were encountered.
        Map<String, Map<String, String>> collectedTagsByBlock = new LinkedHashMap<>();
        Deque<String> blockContextStack = new ArrayDeque<>();

        // Initial processing for common header-like info
        if (!messageLines.isEmpty()) {
            String firstLine = messageLines.get(0);
            if (firstLine.length() >= 33) {
                bankName = firstLine.substring(29, 32);
            }
            if (messageLines.size() > 1) {
                String potentialMtLine = messageLines.get(1);
                Matcher mtMatcher = MESSAGE_TYPE_PATTERN.matcher(potentialMtLine);
                if (mtMatcher.find() && mtMatcher.groupCount() >= 2) {
                    messageType = mtMatcher.group(2);
                }
            }
        }

        // First Pass: Collect all tags by their block context
        // Iterate starting from the line after the initial ADM lines (lines 0 and 1)
        for (int i = 2; i < messageLines.size(); i++) {
            String line = messageLines.get(i);

            Matcher blockStartMatcher = BLOCK_START_PATTERN.matcher(line);
            if (blockStartMatcher.find()) {
                blockContextStack.push(blockStartMatcher.group(1));
                continue;
            }

            Matcher blockEndMatcher = BLOCK_END_PATTERN.matcher(line);
            if (blockEndMatcher.find()) {
                String closingBlockName = blockEndMatcher.group(1);
                if (!blockContextStack.isEmpty() && blockContextStack.peek().equals(closingBlockName)) {
                    blockContextStack.pop();
                } else {
                    System.err.println("Warning: Mismatched or unexpected end block: " + line + " (Deque: " + blockContextStack + ")");
                }
                continue;
            }

            // Determine the current effective block context for the tag.
            // If the stack is empty, it means we are currently outside any 16R/16S block.
            // For tags found here, we can assign a special block name like "OUTER_TAGS".
            // Per new requirement: "all the tags will have at least one parent 16R tag",
            // this "OUTER_TAGS" case might only apply to content after final 16S and before /ADM0000.
            String currentBlock = blockContextStack.isEmpty() ? "OUTER_TAGS" : blockContextStack.peek();

            // Extract tags based on the currentBlock
            for (Map.Entry<String, Pattern> entry : tagPatterns.entrySet()) {
                String tagName = entry.getKey();
                Pattern tagPattern = entry.getValue();

                Matcher tagMatcher = tagPattern.matcher(line);
                if (tagMatcher.find()) {
                    String extractedValue = tagMatcher.group(1);
                    collectedTagsByBlock.computeIfAbsent(currentBlock, k -> new HashMap<>()).put(tagName, extractedValue);
                    break;
                }
            }
        }

        // Second Pass: Create the final MessageDetails object with selected values
        MessageDetails details = new MessageDetails(originalMessage, bankName, messageType);

        // Apply messageType-specific logic to select the final value for each tag.
        // This is where you define your rules for which block's tag takes precedence for a given MT type.
        if ("103".equals(messageType)) {
            // For MT103, get SEME from FIAC block specifically, if not found, search all blocks in order.
            details.setFinalTagValue("SEME", getTagValue(collectedTagsByBlock, "SEME", "FIAC")); // Specific block
            details.setFinalTagValue("SAFE", getTagValue(collectedTagsByBlock, "SAFE", "GENL")); // Specific block
            details.setFinalTagValue("PSET", getTagValue(collectedTagsByBlock, "PSET", null)); // Search all blocks in order
            details.setFinalTagValue("SOMEOTHERTAG", getTagValue(collectedTagsByBlock, "SOMEOTHERTAG", null)); // Search all blocks in order
        } else if ("202".equals(messageType)) {
            // For MT202, get SEME from LINK block specifically, if not found, search all blocks in order.
            details.setFinalTagValue("SEME", getTagValue(collectedTagsByBlock, "SEME", "LINK")); // Specific block
            details.setFinalTagValue("SAFE", getTagValue(collectedTagsByBlock, "SAFE", null)); // Search all blocks in order
            details.setFinalTagValue("PSET", getTagValue(collectedTagsByBlock, "PSET", "FIAC")); // Specific block
            details.setFinalTagValue("SOMEOTHERTAG", getTagValue(collectedTagsByBlock, "SOMEOTHERTAG", "GENL")); // Specific block
        } else {
            // Default logic if messageType is not specifically handled: search all blocks in order
            details.setFinalTagValue("SEME", getTagValue(collectedTagsByBlock, "SEME", null));
            details.setFinalTagValue("SAFE", getTagValue(collectedTagsByBlock, "SAFE", null));
            details.setFinalTagValue("PSET", getTagValue(collectedTagsByBlock, "PSET", null));
            details.setFinalTagValue("SOMEOTHERTAG", getTagValue(collectedTagsByBlock, "SOMEOTHERTAG", null));
        }

        return details;
    }

    /**
     * Retrieves a tag value based on specified block preference or first occurrence.
     * @param collection The map of collected tags by block, where block order is preserved (LinkedHashMap).
     * @param tagName The name of the tag to retrieve.
     * @param specificBlockName If provided (not null/empty), will only look for the tag within this specific block.
     * Otherwise, it will search all blocks in the order they appeared in the message.
     * @return The tag value, or an empty string if not found.
     */
    private String getTagValue(Map<String, Map<String, String>> collection, String tagName, String specificBlockName) {
        // If a specific block name is provided and not empty
        if (specificBlockName != null && !specificBlockName.isEmpty()) {
            return collection.getOrDefault(specificBlockName, new HashMap<>()).getOrDefault(tagName, "");
        } else {
            // If no specific block name is provided, search all blocks in order of appearance
            for (Map.Entry<String, Map<String, String>> blockEntry : collection.entrySet()) {
                String value = blockEntry.getValue().get(tagName);
                if (value != null && !value.isEmpty()) {
                    return value; // Return the first occurrence found
                }
            }
            return ""; // Tag not found in any block
        }
    }
}
