import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageDetails {
    private String originalMessage;
    private String bankName;
    private String messageType;

    private Map<String, String> finalTagValues;

    public MessageDetails(String originalMessage, String bankName, String messageType) {
        this.originalMessage = originalMessage;
        this.bankName = bankName;
        this.messageType = messageType;
        this.finalTagValues = new HashMap<>();
    }

    public String getOriginalMessage() { return originalMessage; }
    public String getBankName() { return bankName; }
    public String getMessageType() { return messageType; }

    public void setFinalTagValue(String tagName, String value) {
        finalTagValues.put(tagName, Objects.requireNonNullElse(value, ""));
    }

    public String getFinalTagValue(String tagName) {
        return Objects.requireNonNullElse(finalTagValues.get(tagName), "");
    }

    @Override
    public String toCsvHeader() {
        return "OriginalMessage,BankName,MessageType,SEME,SAFE,PSET,SOMEOTHERTAG";
    }

    @Override
    public String toCsvLine() {
        return escapeCsv(originalMessage) + "," +
               escapeCsv(bankName) + "," +
               escapeCsv(messageType) + "," +
               escapeCsv(getFinalTagValue("SEME")) + "," +
               escapeCsv(getFinalTagValue("SAFE")) + "," +
               escapeCsv(getFinalTagValue("PSET")) + "," +
               escapeCsv(getFinalTagValue("SOMEOTHERTAG"));
    }

    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replace("\"", "\"\"");
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("\n")) {
            return "\"" + escapedData + "\"";
        }
        return escapedData;
    }
}
