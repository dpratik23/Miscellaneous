import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileCopyUtility {

    public static void copyAndRenameYamlFiles(String resourcesFolder, String serviceFolder, String appName) {
        Path sourceDir = Paths.get(resourcesFolder);
        Path targetDir = Paths.get(serviceFolder);

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            System.out.println("Resources folder does not exist or is not a directory: " + resourcesFolder);
            return;
        }

        // Ensure the service folder exists
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            System.err.println("Error creating service directory: " + e.getMessage());
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "application-*.yml")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String newFileName = fileName.replace("application-", appName + "-");
                Path targetPath = targetDir.resolve(newFileName);

                // Copy and rename file
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copied and renamed: " + fileName + " → " + newFileName);
            }
        } catch (IOException e) {
            System.err.println("Error processing YAML files: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String resourcesFolder = "src/main/resources"; // Path to resources folder
        String serviceFolder = "service"; // Path to service folder
        String appName = "myApp"; // Application name to rename the files

        copyAndRenameYamlFiles(resourcesFolder, serviceFolder, appName);
    }
}
