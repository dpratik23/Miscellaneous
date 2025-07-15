// MessageDetails.java
public class MessageDetails {
    private String originalMessage;
    private String bankName;
    private String semeValue;
    // Add more fields for other tags you need to extract

    public MessageDetails(String originalMessage, String bankName, String semeValue) {
        this.originalMessage = originalMessage;
        this.bankName = bankName;
        this.semeValue = semeValue;
    }

    // Getters for all fields
    public String getOriginalMessage() {
        return originalMessage;
    }

    public String getBankName() {
        return bankName;
    }

    public String getSemeValue() {
        return semeValue;
    }

    // You can add a method to get CSV header or line
    public String toCsvHeader() {
        return "OriginalMessage,BankName,SEMEValue";
    }

    public String toCsvLine() {
        // Simple CSV escaping for now. For production, consider robust CSV libraries.
        return escapeCsv(originalMessage) + "," +
               escapeCsv(bankName) + "," +
               escapeCsv(semeValue);
    }

    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replace("\"", "\"\""); // Escape double quotes
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("\n")) {
            return "\"" + escapedData + "\""; // Enclose in quotes if it contains comma, quote or newline
        }
        return escapedData;
    }
}
