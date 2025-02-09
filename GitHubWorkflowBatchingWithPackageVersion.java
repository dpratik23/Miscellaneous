import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitHubWorkflowBatchingWithPackageVersion {

    private static final String GITHUB_TOKEN = "your_personal_access_token";
    private static final String OWNER = "owner"; // Repository owner
    private static final String REPO = "repo";   // Repository name
    private static final String WORKFLOW_FILE = "workflow-filename.yml"; // Workflow file name
    private static final String PACKAGE_TYPE = "npm"; // Package type (e.g., npm, maven, docker)
    private static final String PACKAGE_NAME = "your-package-name"; // Package name
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
                "https://api.github.com/repos/" + OWNER + "/" + REPO + "/actions/workflows/" + WORKFLOW_FILE + "/dispatches",
                "-d", "{\"ref\":\"" + branchName + "\"}"
            };

            // Execute the curl command using ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(curlCommand);
            processBuilder.redirectErrorStream(true); // Redirect error stream to input stream

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.out.println("Failed to trigger workflow for branch: " + branchName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void waitForBatchCompletion(List<String> branches) {
        try {
            while (true) {
                // Build the curl command to list workflow runs
                String[] curlCommand = {
                    "curl",
                    "-H", "Accept: application/vnd.github+json",
                    "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                    "https://api.github.com/repos/" + OWNER + "/" + REPO + "/actions/runs"
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
                    System.out.println("Failed to fetch workflow runs.");
                    return;
                }

                // Parse the JSON response to check workflow status
                String jsonResponse = response.toString();
                boolean allCompleted = true;
                org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
                org.json.JSONArray workflowRuns = jsonObject.getJSONArray("workflow_runs");

                for (int i = 0; i < workflowRuns.length(); i++) {
                    org.json.JSONObject run = workflowRuns.getJSONObject(i);
                    String branch = run.getString("head_branch");
                    String status = run.getString("status");
                    long runId = run.getLong("id");

                    if (branches.contains(branch) && !status.equals("completed")) {
                        allCompleted = false;
                        break;
                    } else if (branches.contains(branch) && status.equals("completed")) {
                        // Fetch package version for completed workflow
                        String packageVersion = fetchPackageVersion(runId);
                        System.out.println("Workflow run " + runId + " for branch " + branch + " published package version: " + packageVersion);
                    }
                }

                if (allCompleted) {
                    System.out.println("All workflows in the batch are completed.");
                    break;
                } else {
                    System.out.println("Waiting for workflows to complete...");
                    Thread.sleep(POLL_INTERVAL); // Wait before polling again
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String fetchPackageVersion(long runId) {
        try {
            // Build the curl command to list package versions
            String[] curlCommand = {
                "curl",
                "-H", "Accept: application/vnd.github+json",
                "-H", "Authorization: Bearer " + GITHUB_TOKEN,
                "https://api.github.com/orgs/" + OWNER + "/packages/" + PACKAGE_TYPE + "/" + PACKAGE_NAME + "/versions"
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
                System.out.println("Failed to fetch package versions.");
                return null;
            }

            // Parse the JSON response to find the latest package version
            String jsonResponse = response.toString();
            org.json.JSONArray packageVersions = new org.json.JSONArray(jsonResponse);
            if (packageVersions.length() > 0) {
                org.json.JSONObject latestVersion = packageVersions.getJSONObject(0);
                return latestVersion.getString("name"); // Package version name
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
