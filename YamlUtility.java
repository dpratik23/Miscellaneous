import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class YamlUtility {

    public static void removeCustomDbProperty(String filePath) {
        Path yamlPath = Paths.get(filePath);
        
        if (!Files.exists(yamlPath)) {
            System.out.println("YAML file not found: " + filePath);
            return;
        }

        try {
            // Read YAML content as a String
            String yamlContent = Files.readString(yamlPath);

            // Load YAML into a Map
            Yaml yaml = new Yaml();
            Map<String, Object> yamlData = yaml.load(yamlContent);

            // Remove "custom.db" property if it exists
            if (yamlData != null && yamlData.containsKey("custom.db")) {
                yamlData.remove("custom.db");
                System.out.println("'custom.db' property removed.");
            } else {
                System.out.println("'custom.db' property not found.");
                return;
            }

            // Write updated YAML back to the file
            Yaml outputYaml = new Yaml(getDumperOptions());
            String updatedYaml = outputYaml.dump(yamlData);
            Files.writeString(yamlPath, updatedYaml);

        } catch (IOException e) {
            System.err.println("Error processing YAML file: " + e.getMessage());
        }
    }

    private static DumperOptions getDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return options;
    }

    public static void main(String[] args) {
        String filePath = "src/main/resources/application.yml"; // Adjust as needed
        removeCustomDbProperty(filePath);
    }
}
