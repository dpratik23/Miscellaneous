import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ManifestUpdater {

    public static void main(String[] args) throws IOException {
        // Path to the manifest file
        String manifestPath = "path/to/manifest.yml";

        // Initialize SnakeYAML
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK); // Use block style for readability
        Yaml yaml = new Yaml(options);

        // Load the manifest file into a Map
        Map<String, Object> manifest = yaml.load(new FileInputStream(manifestPath));

        // Get the list of applications (usually a list under the "applications" key)
        List<Map<String, Object>> applications = (List<Map<String, Object>>) manifest.get("applications");

        if (applications != null && !applications.isEmpty()) {
            // Assume we're updating the first application in the list
            Map<String, Object> appConfig = applications.get(0);

            // Get the existing services or create a new list if none exist
            List<String> services = (List<String>) appConfig.get("services");
            if (services == null) {
                services = new ArrayList<>();
                appConfig.put("services", services);
            }

            // Add new services (e.g., service-3, service-4)
            services.add("service-3");
            services.add("service-4");

            // Write the updated manifest back to the file
            try (FileWriter writer = new FileWriter(manifestPath)) {
                yaml.dump(manifest, writer);
            }

            System.out.println("Manifest updated successfully!");
        } else {
            System.out.println("No applications found in the manifest.");
        }
    }
}
