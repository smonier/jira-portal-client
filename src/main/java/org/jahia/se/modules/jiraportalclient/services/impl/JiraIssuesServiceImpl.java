package org.jahia.se.modules.jiraportalclient.services.impl;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.se.modules.jiraportalclient.services.JiraIssueService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Dictionary;

@Component(
        service = { JiraIssueService.class, ManagedService.class },
        property = "service.pid=org.jahia.se.modules.jiraportalclient.taglibs.JiraIssueList",
        immediate = true
)
public class JiraIssuesServiceImpl implements JiraIssueService, ManagedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesServiceImpl.class);
    private String jiraLogin;
    private String jiraToken;

    @Override
    public boolean createIssue(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority) {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue";
        String encoding = getEncodedCredentials();

        JSONObject issueData = createIssuePayload(projectKey, summary, description, issueType, priority);

        assert issueData != null;
        return sendPostRequest(jiraUrl, encoding, issueData);
    }

    @Override
    public boolean createIssueWithCustomField(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority, String customFieldValue) {
        try {
            String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue";
            String encoding = getEncodedCredentials();

            JSONObject issueData = createIssuePayload(projectKey, summary, description, issueType, priority);
            issueData.getJSONObject("fields").put("customfield_10080", customFieldValue);

            return sendPostRequest(jiraUrl, encoding, issueData);
        } catch (JSONException e) {
            LOGGER.error("Failed to create Jira issue with custom field", e);
            return false;
        }
    }

    @Override
    public boolean updateIssueStatus(String jiraInstance, String issueKey, String transitionId) {
        try {
            String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";
            String encoding = getEncodedCredentials();

            JSONObject validTransition = checkTransitionValidity(jiraInstance, issueKey, transitionId, jiraUrl, encoding);
            if (validTransition != null) {
                JSONObject transitionData = new JSONObject().put("transition", validTransition);
                return sendPostRequest(jiraUrl, encoding, transitionData);
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Failed to update issue status", e);
        }
        return false;
    }

    @Override
    public boolean addCommentToIssue(String jiraInstance, String issueKey, String commentText) {
        try {
            String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/comment";
            String encoding = getEncodedCredentials();

            JSONObject commentData = new JSONObject().put("body", commentText);
            return sendPostRequest(jiraUrl, encoding, commentData);
        } catch (JSONException e) {
            LOGGER.error("Failed to add comment to issue", e);
            return false;
        }
    }

    @Override
    public boolean moveIssue(String jiraInstance, String oldIssueKey, String targetProjectKey) {
        try {
            JSONObject issueDetails = getIssueDetails(jiraInstance, oldIssueKey);
            if (issueDetails == null) {
                LOGGER.error("Unable to fetch issue details for issue key: " + oldIssueKey);
                return false;
            }

            String summary = issueDetails.getJSONObject("fields").getString("summary");
            String description = issueDetails.getJSONObject("fields").getString("description");
            String issueType = issueDetails.getJSONObject("fields").getJSONObject("issuetype").getString("name");
            String priority = issueDetails.getJSONObject("fields").getJSONObject("priority").getString("name");
            String customField = issueDetails.getJSONObject("fields").getString("customfield_10080");

            boolean newIssueCreated = createIssueWithCustomField(jiraInstance, targetProjectKey, summary, description, issueType, priority, customField);
            if (!newIssueCreated) {
                LOGGER.error("Failed to create issue in target project: " + targetProjectKey);
                return false;
            }

            String newIssueKey = getNewIssueKey(jiraInstance, targetProjectKey, summary);
            LOGGER.info("New Issue Key: " + newIssueKey);

            transferActivities(jiraInstance, oldIssueKey, newIssueKey);
            closeIssue(jiraInstance, oldIssueKey);

            LOGGER.info("Issue " + oldIssueKey + " successfully moved to project " + targetProjectKey);
            return true;
        } catch (IOException | JSONException e) {
            LOGGER.error("Failed to move issue", e);
            return false;
        }
    }

    @Override
    public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
        if (dictionary != null) {
            jiraLogin = (String) dictionary.get("jiraLogin");
            jiraToken = (String) dictionary.get("jiraToken");
        }
        if (jiraToken == null || jiraToken.trim().isEmpty()) {
            LOGGER.error("JiraIssue Token not defined. Please add it to the configuration.");
        }
        LOGGER.debug("JiraIssue Token = {}", jiraToken);
    }

    // Utility methods
    private JSONObject createIssuePayload(String projectKey, String summary, String description, String issueType, String priority) {
        try {
            JSONObject issueData = new JSONObject();
            issueData.put("fields", new JSONObject()
                    .put("project", new JSONObject().put("key", projectKey))
                    .put("summary", summary)
                    .put("description", description)
                    .put("issuetype", new JSONObject().put("name", issueType))
                    .put("priority", new JSONObject().put("name", priority)));
            return issueData;
        } catch (JSONException e) {
            LOGGER.error("Error creating JSON for new issue", e);
            return null;
        }
    }

    private String getEncodedCredentials() {
        return Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes(StandardCharsets.UTF_8));
    }

    private boolean sendPostRequest(String jiraUrl, String encoding, JSONObject data) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(jiraUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                LOGGER.info("Request successful. Response code: " + responseCode);
                return true;
            } else {
                LOGGER.error("Error sending POST request to Jira.  Response code:" + responseCode);
                logErrorResponse(connection);
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Error sending POST request to Jira", e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject getIssueDetails(String jiraInstance, String issueKey) {
        try {
            String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey;
            HttpURLConnection connection = (HttpURLConnection) new URL(jiraUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + getEncodedCredentials());

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new JSONObject(convertStreamToString(connection.getInputStream()));
            } else {
                LOGGER.error("Failed to fetch issue details. Response code: " + connection.getResponseCode());
                return null;
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Error retrieving issue details", e);
            return null;
        }
    }

    private void closeIssue(String jiraInstance, String issueKey) {
        try {
            String transitionId = "31"; // Transition ID for closing the issue
            String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";

            JSONObject transitionData = new JSONObject().put("transition", new JSONObject().put("id", transitionId));
            sendPostRequest(jiraUrl, getEncodedCredentials(), transitionData);
        } catch (JSONException e) {
            LOGGER.error("Failed to close issue", e);
        }
    }

    private void transferActivities(String jiraInstance, String oldIssueKey, String newIssueKey) {
        transferComments(jiraInstance, oldIssueKey, newIssueKey);
        transferWorklogs(jiraInstance, oldIssueKey, newIssueKey);
        transferAttachments(jiraInstance, oldIssueKey, newIssueKey);
    }

    private void transferComments(String jiraInstance, String oldIssueKey, String newIssueKey) {
        try {
            JSONArray comments = getIssueComments(jiraInstance, oldIssueKey);
            if (comments != null) {
                for (int i = 0; i < comments.length(); i++) {
                    JSONObject comment = comments.getJSONObject(i);
                    String body = comment.getString("body");
                    addCommentToIssue(jiraInstance, newIssueKey, body);
                }
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Error transferring comments between issues", e);
        }
    }

    private void transferWorklogs(String jiraInstance, String oldIssueKey, String newIssueKey) {
        try {
            JSONArray worklogs = getIssueWorklogs(jiraInstance, oldIssueKey);
            if (worklogs != null) {
                for (int i = 0; i < worklogs.length(); i++) {
                    JSONObject worklog = worklogs.getJSONObject(i);
                    String timeSpent = worklog.getString("timeSpent");
                    String comment = worklog.optString("comment", "");
                    addWorklogToIssue(jiraInstance, newIssueKey, timeSpent, comment);
                }
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Error transferring worklogs between issues", e);
        }
    }

    private void transferAttachments(String jiraInstance, String oldIssueKey, String newIssueKey) {
        try {
            JSONArray attachments = getIssueAttachments(jiraInstance, oldIssueKey);
            if (attachments != null) {
                for (int i = 0; i < attachments.length(); i++) {
                    JSONObject attachment = attachments.getJSONObject(i);
                    String fileName = attachment.getString("filename");
                    String fileUrl = attachment.getString("content");
                    uploadAttachmentToIssue(jiraInstance, newIssueKey, fileUrl, fileName);
                }
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Error transferring attachments between issues", e);
        }
    }

    private JSONArray getIssueComments(String jiraInstance, String issueKey) throws IOException, JSONException {
        String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/comment";
        JSONObject response = sendGetRequest(apiUrl);
        if (response != null && response.has("comments")) {
            return response.getJSONArray("comments");
        }
        return null; // No comments found
    }

    private JSONArray getIssueWorklogs(String jiraInstance, String issueKey) throws IOException, JSONException {
        String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/worklog";
        JSONObject response = sendGetRequest(apiUrl);
        if (response != null && response.has("worklogs")) {
            return response.getJSONArray("worklogs");
        }
        return null; // No worklogs found
    }

    private JSONArray getIssueAttachments(String jiraInstance, String issueKey) throws IOException, JSONException {
        String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "?fields=attachment";
        JSONObject response = sendGetRequest(apiUrl);
        if (response != null && response.getJSONObject("fields").has("attachment")) {
            return response.getJSONObject("fields").getJSONArray("attachment");
        }
        return null; // No attachments found
    }

    private void addWorklogToIssue(String jiraInstance, String issueKey, String timeSpent, String comment) {
        try {
            String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/worklog";
            JSONObject worklogData = new JSONObject().put("timeSpent", timeSpent);
            if (!comment.isEmpty()) {
                worklogData.put("comment", comment);
            }
            sendPostRequest(apiUrl, getEncodedCredentials(), worklogData);
        } catch (JSONException e) {
            LOGGER.error("Error adding worklog to issue", e);
        }
    }

    private void uploadAttachmentToIssue(String jiraInstance, String issueKey, String fileUrl, String fileName) {
        try {
            String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/attachments";

            // Download the attachment file
            File downloadedFile = downloadFile(fileUrl);

            // Create a multipart request to upload the file
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", downloadedFile, ContentType.APPLICATION_OCTET_STREAM, fileName);
            HttpEntity multipartEntity = builder.build();

            sendMultipartPostRequest(apiUrl, multipartEntity);
        } catch (IOException e) {
            LOGGER.error("Error uploading attachment to issue", e);
        }
    }

    private JSONObject sendGetRequest(String apiUrl) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + getEncodedCredentials()); // Basic Auth
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new JSONObject(convertStreamToString(connection.getInputStream()));
            } else {
                LOGGER.error("GET request failed. Response code: " + responseCode);
                return null;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void sendMultipartPostRequest(String apiUrl, HttpEntity multipartEntity) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + getEncodedCredentials());
            connection.setRequestProperty("X-Atlassian-Token", "no-check"); // Required for file uploads

            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                multipartEntity.writeTo(os);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logErrorResponse(connection);
            }
        } catch (IOException e) {
            LOGGER.error("Error sending multipart POST request", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String convertStreamToString(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private File downloadFile(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream inputStream = connection.getInputStream();
        File tempFile = File.createTempFile("download", ".tmp");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }


    private String getNewIssueKey(String jiraInstance, String targetProjectKey, String summary) throws IOException, JSONException {
        // Properly encode the summary
        String encodedSummary = URLEncoder.encode(summary, StandardCharsets.UTF_8.toString());

        // Construct the API URL with the encoded summary
        String apiUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/search?jql=project="
                + targetProjectKey
                + "%20AND%20summary~%22" + encodedSummary + "%22%20ORDER%20BY%20created%20DESC&maxResults=1";

        LOGGER.info("getNewIssueKey : " + apiUrl);

        // Send the GET request
        JSONObject response = sendGetRequest(apiUrl);  // Ensure this function handles response correctly
        if (response != null && response.has("issues")) {
            JSONArray issues = response.getJSONArray("issues");
            if (issues.length() > 0) {
                return issues.getJSONObject(0).getString("key");
            }
        }
        return null;
    }

    private JSONObject checkTransitionValidity(String jiraInstance, String issueKey, String transitionId, String jiraUrl, String encoding) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(jiraUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String jsonResponse = convertStreamToString(connection.getInputStream());

                JSONArray transitionsArray = new JSONObject(jsonResponse).getJSONArray("transitions");
                for (int i = 0; i < transitionsArray.length(); i++) {
                    JSONObject transition = transitionsArray.getJSONObject(i);
                    if (transition.getString("id").equals(transitionId)) {
                        // Valid transition ID found, return the transition object
                        LOGGER.info("Valid transition ID: " + transitionId + " for issue " + issueKey);

                        return transition;
                    }
                }
                LOGGER.error("Invalid transition ID: " + transitionId + " for issue " + issueKey);
                return null; // Transition not found
            } else {
                LOGGER.error("Failed to retrieve transitions for issue " + issueKey + ": HTTP error code: " + responseCode);
                logErrorResponse(connection);
                return null; // Failed to retrieve transitions
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void logErrorResponse(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                LOGGER.error("Error response: " + errorResponse.toString());
            }
        } else {
            LOGGER.error("No error stream available for the connection.");
        }
    }
}