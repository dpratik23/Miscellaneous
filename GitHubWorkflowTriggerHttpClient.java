import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class GitHubWorkflowTriggerHttpClient {

    private static final String GITHUB_TOKEN = "your_personal_access_token";
    private static final String WORKFLOW_FILE_NAME = "ci.yml"; // Your workflow filename

    public static void main(String[] args) {
        String repoUrl = "https://github.com/owner/repo";
        String branchName = "main";
        triggerWorkflow(repoUrl, branchName);
    }

    public static void triggerWorkflow(String repoUrl, String branchName) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Extract owner and repo from URL
            String[] parts = repoUrl.replace("https://github.com/", "")
                                    .replace(".git", "")
                                    .split("/");
            String owner = parts[0];
            String repo = parts[1];

            // GitHub API endpoint
            String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches",
                owner, repo, WORKFLOW_FILE_NAME
            );

            // Create POST request
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Accept", "application/vnd.github+json");
            httpPost.setHeader("Authorization", "Bearer " + GITHUB_TOKEN);
            httpPost.setHeader("Content-Type", "application/json");

            // Build JSON payload
            String jsonPayload = String.format("{\"ref\": \"%s\"}", branchName);
            httpPost.setEntity(new StringEntity(jsonPayload));

            // Execute request
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            // Handle response
            if (statusCode == 204) {
                System.out.println("Successfully triggered workflow for: " + repoUrl);
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Failed to trigger workflow. Status: " + statusCode);
                System.out.println("Response: " + responseBody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
