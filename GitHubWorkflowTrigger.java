import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class GitHubWorkflowTrigger {

    private static final String GITHUB_TOKEN = "your_personal_access_token";
    private static final String WORKFLOW_FILE_NAME = "workflow-filename.yml"; // e.g., "ci.yml"

    public static void main(String[] args) {
        String repoUrl = "https://github.com/owner/repo";
        String branchName = "main";
        triggerWorkflow(repoUrl, branchName);
    }

    public static void triggerWorkflow(String repoUrl, String branchName) {
        // Extract owner and repo name from URL
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        String owner = parts[0];
        String repo = parts[1].replace(".git", "");

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches",
                owner, repo, WORKFLOW_FILE_NAME);

        String requestBody = String.format("{\"ref\":\"%s\"}", branchName);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + GITHUB_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                System.out.println("Workflow triggered successfully for " + repoUrl);
            } else {
                System.out.println("Failed to trigger workflow. Status code: " + response.statusCode());
                System.out.println("Response: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
