package org.jahia.se.modules.jiraportalclient.services.impl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Dictionary;
import org.jahia.se.modules.jiraportalclient.services.JiraIssueService;

@Component(service = { JiraIssueService.class,
        ManagedService.class }, property = "service.pid=org.jahia.se.modules.jiraportalclient.taglibs.JiraIssueList", immediate = true)
public class JiraIssuesServiceImpl implements JiraIssueService, ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesServiceImpl.class);
    private String jiraLogin;
    private String jiraToken;


    /**
     * Creates a new issue in Jira with the specified parameters.
     *
     * @param jiraInstance the Jira instance URL (e.g., "yourInstance")
     * @param projectKey   the project key (e.g., "PROJECT123")
     * @param summary      the summary of the issue
     * @param description  the description of the issue
     * @param issueType    the type of the issue (e.g., "Bug", "Task")
     * @param priority     the priority of the issue (e.g., "High", "Medium", "Low")
     * @return the response from Jira API as a String, or null if an error occurred
     * @throws IOException if there is an issue with the network connection or data processing
     */
    @Override
    public boolean createIssue(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority) throws IOException {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        // Create the JSON payload for the issue creation
        JSONObject issueData = new JSONObject();
        try {
            issueData.put("fields", new JSONObject()
                    .put("project", new JSONObject().put("key", projectKey))
                    .put("summary", summary)
                    .put("description", description)
                    .put("issuetype", new JSONObject().put("name", issueType))
                    .put("priority", new JSONObject().put("name", priority))  // Adding priority field
            );
        } catch (JSONException e) {
            LOGGER.error("Error creating JSON for new issue", e);
            return false;
        }

        // Send the HTTP POST request to create the issue
        URL url = new URL(jiraUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Basic " + encoding);
        http.setDoOutput(true);

        try (OutputStream os = http.getOutputStream()) {
            byte[] input = issueData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = http.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED) {  // HTTP 201 Created
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String responseLine;
            StringBuilder response = new StringBuilder();
            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine);
            }
            in.close();
            return true;
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(http.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            LOGGER.error("Failed to create issue: HTTP error code : " + responseCode + ", Error: " + errorResponse.toString());
            return false;
        }
    }

    @Override
    public boolean createIssueWithCustomField(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority, String customFieldValue) throws IOException {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        // Create the JSON payload for the issue creation
        JSONObject issueData = new JSONObject();
        try {
            issueData.put("fields", new JSONObject()
                    .put("project", new JSONObject().put("key", projectKey))
                    .put("summary", summary)
                    .put("description", description)
                    .put("issuetype", new JSONObject().put("name", issueType))
                    .put("priority", new JSONObject().put("name", priority))  // Adding priority field
                    .put("customfield_10080", customFieldValue)  // Adding custom field
            );
        } catch (JSONException e) {
            LOGGER.error("Error creating JSON for new issue", e);
            return false;
        }

        // Send the HTTP POST request to create the issue
        URL url = new URL(jiraUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Basic " + encoding);
        http.setDoOutput(true);

        try (OutputStream os = http.getOutputStream()) {
            byte[] input = issueData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = http.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED) {  // HTTP 201 Created
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String responseLine;
            StringBuilder response = new StringBuilder();
            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine);
            }
            in.close();
            return true;
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(http.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorResponse.append(errorLine);
            }
            errorReader.close();
            LOGGER.error("Failed to create issue: HTTP error code : " + responseCode + ", Error: " + errorResponse.toString());
            return false;
        }
    }
    // Method to update an existing Jira issue's status
    @Override
    public boolean updateIssueStatus(String jiraInstance, String issueKey, String transitionId) throws IOException, JSONException {
        // Step 1: Get the list of available transitions for the issue
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        URL url = new URL(jiraUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Basic " + encoding);

        int responseCode = http.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream response = http.getInputStream();
            String jsonResponse = convertStreamToString(response);

            // Parse the JSON response to find a valid transition ID
            JSONArray transitionsArray = new JSONObject(jsonResponse).getJSONArray("transitions");
            boolean isValidTransition = false;

            for (int i = 0; i < transitionsArray.length(); i++) {
                JSONObject transition = transitionsArray.getJSONObject(i);
                if (transition.getString("id").equals(transitionId)) {
                    isValidTransition = true;
                    break;
                }
            }

            // If the transition ID is valid, proceed with the update
            if (isValidTransition) {
                JSONObject transitionData = new JSONObject();
                try {
                    transitionData.put("transition", new JSONObject().put("id", transitionId));
                } catch (JSONException e) {
                    LOGGER.error("Error creating JSON for issue transition", e);
                    return false;
                }

                // Step 2: Send the POST request to update the issue status
                http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("POST");
                http.setRequestProperty("Content-Type", "application/json");
                http.setRequestProperty("Authorization", "Basic " + encoding);
                http.setDoOutput(true);

                try (OutputStream os = http.getOutputStream()) {
                    byte[] input = transitionData.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                responseCode = http.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    LOGGER.info("Issue " + issueKey + " status updated successfully");
                    return true;
                } else {
                    InputStream errorStream = http.getErrorStream();
                    String errorResponse = convertStreamToString(errorStream);
                    LOGGER.error("Failed to update issue status: HTTP error code : " + responseCode + ", Response: " + errorResponse);
                    return false;
                }
            } else {
                LOGGER.error("Invalid transition ID: " + transitionId + " for issue " + issueKey);
                return false;
            }
        } else {
            InputStream errorStream = http.getErrorStream();
            String errorResponse = convertStreamToString(errorStream);
            LOGGER.error("Failed to get transitions: HTTP error code : " + responseCode + ", Response: " + errorResponse);
            return false;
        }
    }

    /**
     * Adds a comment to a specific Jira issue.
     *
     * @param jiraInstance the Jira instance (e.g., "yourInstance" in "yourInstance.atlassian.net")
     * @param issueKey     the key of the Jira issue (e.g., "PROJECT-123")
     * @param commentText  the comment text to add to the issue
     * @return true if the comment was successfully added, false otherwise
     */
    @Override
    public boolean addCommentToIssue(String jiraInstance, String issueKey, String commentText) {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/comment";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes());

        try {
            // Create JSON object for the comment
            JSONObject commentData = new JSONObject();
            commentData.put("body", commentText);

            // Set up the connection to the Jira REST API
            URL url = new URL(jiraUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setDoOutput(true);

            // Write the JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = commentData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code to check if the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("Comment added successfully to issue " + issueKey);
                return true;
            } else {
                System.err.println("Failed to add comment: HTTP error code " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error adding comment to issue: " + e.getMessage());
        }
        return false;
    }

    /**
     * Moves an issue from one project to another by creating a new issue in the target project,
     * copying the necessary details, and closing the original issue.
     *
     * @param jiraInstance     The Jira instance to use
     * @param oldIssueKey      The issue key of the original issue
     * @param targetProjectKey The key of the target project
     * @return true if the issue was successfully moved, false otherwise
     * @throws IOException if there's an error with the network request
     */
    @Override
    public boolean moveIssue(String jiraInstance, String oldIssueKey, String targetProjectKey) throws IOException, JSONException {
        // Step 1: Get the issue details from the original project
        JSONObject issueDetails = getIssueDetails(jiraInstance, oldIssueKey);
        if (issueDetails == null) {
            LOGGER.error("Unable to fetch issue details for issue key: " + oldIssueKey);
            return false;
        }

        // Step 2: Extract the fields needed to create a new issue
        String summary = issueDetails.getJSONObject("fields").getString("summary");
        String description = issueDetails.getJSONObject("fields").getString("description");
        String issueType = issueDetails.getJSONObject("fields").getJSONObject("issuetype").getString("name");
        String priority = issueDetails.getJSONObject("fields").getJSONObject("priority").getString("name");
        String marketNum = issueDetails.getJSONObject("fields").getString("customfield_10080");

        LOGGER.info("Moving issue {} from issue {} - targetProjectKey : {} - marketNum : {}", jiraInstance, oldIssueKey, targetProjectKey, marketNum);
        // Step 3: Create a new issue in the target project
        boolean newIssueCreated = createIssueWithCustomField(jiraInstance, targetProjectKey, summary, description, issueType, priority, marketNum);
        if (!newIssueCreated) {
            LOGGER.error("Failed to create issue in target project: " + targetProjectKey);
            return false;
        }

        // Step 4: Close or link the original issue (optional)
        closeIssue(jiraInstance, oldIssueKey);

        LOGGER.info("Issue " + oldIssueKey + " successfully moved to project " + targetProjectKey);
        return true;
    }

    /**
     * Fetches the details of an issue by its issue key.
     *
     * @param jiraInstance The Jira instance to use
     * @param issueKey The issue key to fetch
     * @return The issue details as a JSONObject, or null if the request failed
     * @throws IOException if there is an error with the network request
     */
    public JSONObject getIssueDetails(String jiraInstance, String issueKey) throws IOException, JSONException {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey;
        HttpURLConnection connection = (HttpURLConnection) new URL(jiraUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes()));

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            return new JSONObject(convertStreamToString(inputStream));
        } else {
            LOGGER.error("Failed to fetch issue details: " + responseCode);
            return null;
        }
    }

    /**
     * Closes an issue by transitioning it to the "Closed" status.
     *
     * @param jiraInstance The Jira instance to use
     * @param issueKey The issue key to close
     * @throws IOException if there is an error with the network request
     */
    public void closeIssue(String jiraInstance, String issueKey) throws IOException, JSONException {
        // You may need to adjust the transition ID based on your Jira workflow setup
        String transitionId = "31"; // The ID for the "Closed" transition
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";

        JSONObject transitionData = new JSONObject();
        transitionData.put("transition", new JSONObject().put("id", transitionId));

        HttpURLConnection connection = (HttpURLConnection) new URL(jiraUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes()));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = transitionData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            LOGGER.info("Issue " + issueKey + " successfully closed.");
        } else {
            LOGGER.error("Failed to close issue: " + responseCode);
        }
    }

    // Helper method to convert InputStream to String
    private String convertStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, length));
        }
        return sb.toString();
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {

        if (dictionary != null) {
            jiraLogin = (String) dictionary.get("jiraLogin");
            jiraToken = (String) dictionary.get("jiraToken");

        }
        if (!(jiraToken != null && !jiraToken.trim().isEmpty()))
            LOGGER.error(
                    "JiraIssue Token not defined. Please add it to org.jahia.se.modules.taglibs.JiraIssueList.cfg");
        LOGGER.debug("JiraIssue Token = {}", jiraToken);
    }
}
