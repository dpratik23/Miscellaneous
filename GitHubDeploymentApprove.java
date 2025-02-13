import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GitHubDeploymentApprove {

    private static final String GITHUB_TOKEN = "your_personal_access_token";
    private static final String OWNER = "owner"; // Repository owner
    private static final String REPO = "repo";   // Repository name
    private static final String ENVIRONMENT = "production"; // Environment name
    private static final int BATCH_SIZE = 10; // Number of workflows to trigger per batch
    private static final long POLL_INTERVAL = 10000; // Poll every 10 seconds

    public static void main(String[] args) {
        try {
            List<String> batch = new ArrayList<>();
            for (int i = 1; i <= 20; i++) { // Example: Trigger 20 workflows in batches
                if (batch.size() >= BATCH_SIZE) {
                    waitForBatchCompletion(batch);
                    batch.clear();
                }
                String branchName = "branch-" + i; // Example branch name
                triggerWorkflow(branchName);
                batch.add(branchName);
                System.out.println("Triggered workflow for branch: " + branchName);
            }
            if (!batch.isEmpty()) {
                waitForBatchCompletion(batch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void triggerWorkflow(String branchName) {
        try {
            // Build the curl command to trigger the workflow
            String[] curlCommand = {
                "curl",
                "-X", "POST",
                "-H", "Accept: application/vnd.github+json",
                "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                "-H", "Content-Type: application/json",
                "https://api.github.com/repos/" + OWNER + "/" + REPO + "/actions/workflows/ci.yml/dispatches",
                "-d", "{\\\"ref\\\":\\\"" + branchName + "\\\"}" // Escaped JSON for Windows
            };

            // Execute the curl command using ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(curlCommand);
            processBuilder.redirectErrorStream(true); // Redirect error stream to input stream

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Read the output (if any)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Handle the result
            if (exitCode == 0) {
                System.out.println("Successfully triggered workflow for branch: " + branchName);
            } else {
                System.out.println("Failed to trigger workflow for branch: " + branchName);
                System.out.println("Output: " + output.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void waitForBatchCompletion(List<String> branches) {
        try {
            while (true) {
                // Get today's date in YYYY-MM-DD format
                String today = LocalDate.now().toString();

                // Build the curl command to list deployments
                String[] curlCommand = {
                    "curl",
                    "-H", "Accept: application/vnd.github+json",
                    "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                    "https://api.github.com/repos/" + OWNER + "/" + REPO + "/deployments?environment=" + ENVIRONMENT
                };

                // Execute the curl command using ProcessBuilder
                ProcessBuilder processBuilder = new ProcessBuilder(curlCommand);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.out.println("Failed to fetch deployments.");
                    return;
                }

                // Parse the JSON response to check deployment status
                String jsonResponse = response.toString();
                boolean allCompleted = true;
                org.json.JSONArray deployments = new org.json.JSONArray(jsonResponse);

                for (int i = 0; i < deployments.length(); i++) {
                    org.json.JSONObject deployment = deployments.getJSONObject(i);
                    long deploymentId = deployment.getLong("id");
                    String branch = deployment.getString("ref");
                    String status = getDeploymentStatus(deploymentId);

                    if (branches.contains(branch)) {
                        if (status.equals("waiting")) {
                            // Approve the deployment if it is waiting
                            approveDeployment(deploymentId);
                            System.out.println("Approved deployment " + deploymentId + " for branch " + branch + ".");
                        } else if (!status.equals("success")) {
                            allCompleted = false;
                            System.out.println("Deployment for branch " + branch + " is " + status + ".");
                        } else {
                            System.out.println("Deployment for branch " + branch + " is completed.");
                        }
                    }
                }

                if (allCompleted) {
                    System.out.println("All deployments in the batch are completed.");
                    break;
                } else {
                    System.out.println("Waiting for deployments to complete...");
                    Thread.sleep(POLL_INTERVAL); // Wait before polling again
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDeploymentStatus(long deploymentId) {
        try {
            // Build the curl command to list deployment statuses
            String[] curlCommand = {
                "curl",
                "-H", "Accept: application/vnd.github+json",
                "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                "https://api.github.com/repos/" + OWNER + "/" + REPO + "/deployments/" + deploymentId + "/statuses"
            };

            // Execute the curl command using ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(curlCommand);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Failed to fetch deployment statuses.");
                return null;
            }

            // Parse the JSON response to get the latest status
            String jsonResponse = response.toString();
            org.json.JSONArray statuses = new org.json.JSONArray(jsonResponse);
            if (statuses.length() > 0) {
                org.json.JSONObject latestStatus = statuses.getJSONObject(0);
                return latestStatus.getString("state");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void approveDeployment(long deploymentId) {
        try {
            // Build the curl command to approve the deployment
            String[] curlCommand = {
                "curl",
                "-X", "POST",
                "-H", "Accept: application/vnd.github+json",
                "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                "-H", "Content-Type: application/json",
                "https://api.github.com/repos/" + OWNER + "/" + REPO + "/deployments/" + deploymentId + "/statuses",
                "-d", "{\\\"state\\\":\\\"approved\\\"}" // Escaped JSON for Windows
            };

            // Execute the curl command using ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(curlCommand);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Read the output (if any)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Handle the result
            if (exitCode == 0) {
                System.out.println("Successfully approved deployment: " + deploymentId);
            } else {
                System.out.println("Failed to approve deployment: " + deploymentId);
                System.out.println("Output: " + output.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
