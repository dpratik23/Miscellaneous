import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileCopyUtility {

    public static void main(String[] args) {
        // Define paths
        String appName = "myApp"; // Replace with your application name
        String resourcesFolder = "src/main/resources"; // Path to resources folder
        String serviceFolder = "service"; // Path to service folder (relative to the project root)

        // Create File objects
        File resourcesDir = new File(resourcesFolder);
        File serviceDir = new File(serviceFolder);

        if (!resourcesDir.exists() || !resourcesDir.isDirectory()) {
            System.out.println("Resources folder does not exist or is not a directory.");
            return;
        }

        // Ensure the service folder exists
        if (!serviceDir.exists()) {
            serviceDir.mkdirs();
        }

        // Filter and copy YAML files
        File[] yamlFiles = resourcesDir.listFiles((dir, name) -> name.startsWith("application-") && name.endsWith(".yml"));
        if (yamlFiles != null) {
            for (File file : yamlFiles) {
                try {
                    String newFileName = file.getName().replace("application-", appName + "-");
                    Path targetPath = new File(serviceDir, newFileName).toPath();

                    // Copy and rename file
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied and renamed: " + file.getName() + " to " + targetPath.getFileName());
                } catch (IOException e) {
                    System.err.println("Error copying file " + file.getName() + ": " + e.getMessage());
                }
            }
        } else {
            System.out.println("No YAML files found in the resources folder.");
        }
    }
}
