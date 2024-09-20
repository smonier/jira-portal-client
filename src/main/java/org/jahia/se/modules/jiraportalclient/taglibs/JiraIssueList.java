package org.jahia.se.modules.jiraportalclient.taglibs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.se.modules.jiraportalclient.model.IssueType;
import org.jahia.se.modules.jiraportalclient.model.Status;
import org.jahia.services.render.RenderContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import org.jahia.se.modules.jiraportalclient.model.JiraIssue;
import org.jahia.se.modules.jiraportalclient.functions.PortalFunctions;

import javax.jcr.RepositoryException;

@Component
public class JiraIssueList {

    private static final Logger logger = LoggerFactory.getLogger(JiraIssueList.class);

    private static String jiraLogin;
    private static String jiraToken;
    private static ContextServerService contextServerService;

    @Reference(service = ContextServerService.class)
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }

    @Activate
    public void activate(Map<String, ?> props) {
        try {
            setJiraLogin((String) props.get("jiraLogin"));
            setJiraToken((String) props.get("jiraToken"));
            logger.info(getJiraLogin() + getJiraToken());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getJiraLogin() {
        return jiraLogin;
    }

    public void setJiraLogin(String jiraLogin) {
        this.jiraLogin = jiraLogin;
    }

    public String getJiraToken() {
        return jiraToken;
    }

    public void setJiraToken(String jiraToken) {
        this.jiraToken = jiraToken;
    }

    // Method to get Jira Issues
    public static List<JiraIssue> getJiraIssues(String jiraInstance, String jiraProject) throws IOException, JSONException {
        logger.info("Getting Jira Issues from " + jiraInstance + " for the project " + jiraProject);

        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/search?jql=project=" + jiraProject;
        String jiraUsername = jiraLogin;
        String jiraOauthToken = jiraToken;
        List<JiraIssue> JIRAISSUE_ARRAY_LIST = new ArrayList<>();

        String jsonReply;
        String encoding = Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraOauthToken).getBytes("UTF-8"));
        URL url = new URL(jiraUrl);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Basic " + encoding);
            long l = System.currentTimeMillis();

            if (http.getResponseCode() == 201 || http.getResponseCode() == 200) {
                InputStream response = http.getInputStream();
                jsonReply = convertStreamToString(response);
                JSONObject jiraJsonObject = new JSONObject(jsonReply);
                JSONArray jiraIssueArray = new JSONArray(jiraJsonObject.optString("issues"));
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JIRAISSUE_ARRAY_LIST = mapper.readValue(jiraIssueArray.toString(), new TypeReference<List<JiraIssue>>(){});
                } finally {
                    http.disconnect();
                    logger.info(http.getResponseCode() + " " + http.getResponseMessage());
                    logger.info("Request {} executed in {} ms", url, (System.currentTimeMillis() - l));
                }
            }
        } catch (Exception e) {
            logger.error("Error connection: "+url);
            e.printStackTrace();
            return null;
        }
        logger.info("List return from: "+url);

        return JIRAISSUE_ARRAY_LIST;
    }

    // Method to create a new Jira issue
    public static String createIssue(String jiraInstance, String projectKey, String summary, String description, String issueType) throws IOException {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        JSONObject issueData = new JSONObject();
        try {
            issueData.put("fields", new JSONObject()
                    .put("project", new JSONObject().put("key", projectKey))
                    .put("summary", summary)
                    .put("description", description)
                    .put("issuetype", new JSONObject().put("name", issueType))
            );
        } catch (JSONException e) {
            logger.error("Error creating JSON for new issue", e);
            return null;
        }

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
        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            String responseLine;
            StringBuilder response = new StringBuilder();
            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine);
            }
            in.close();
            return response.toString();
        } else {
            logger.error("Failed to create issue: HTTP error code : " + responseCode);
            return null;
        }
    }
    public static List<JiraIssue> getJiraTickets(String jiraInstance, String jiraProject, RenderContext renderContext) throws IOException, JSONException, RepositoryException, InterruptedException, ExecutionException {

        //      RenderContext renderContext = (RenderContext) pageContext.getAttribute("renderContext", PageContext.REQUEST_SCOPE);

        PortalFunctions jiraProjectName = new PortalFunctions();

        String jiraProjectNameValue = jiraProjectName.getPropertyValue("jiraProjectName", renderContext, contextServerService);
        if (jiraProjectNameValue != null) {
            jiraProject = jiraProjectNameValue;
        }
        logger.info("Getting Jira Issues from " + jiraInstance + " for the project " + jiraProject);

        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/search?jql=project=" + jiraProject;
        String jiraUsername = jiraLogin;
        String jiraOauthToken = jiraToken;
        logger.info("JiraURL: "+jiraUrl);
        List<JiraIssue> JIRAISSUE_ARRAY_LIST = new ArrayList<>();

        String jsonReply;
        String encoding = Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraOauthToken).getBytes("UTF-8"));
        URL url = new URL(jiraUrl);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Basic " + encoding);
            long l = System.currentTimeMillis();

            if (http.getResponseCode() == 201 || http.getResponseCode() == 200) {
                InputStream response = http.getInputStream();
                jsonReply = convertStreamToString(response);
                JSONObject jiraJsonObject = new JSONObject(jsonReply);
                JSONArray jiraIssueArray = new JSONArray(jiraJsonObject.optString("issues"));
                logger.info("JIRA : "+ jiraIssueArray.toString());
                try {

                    ObjectMapper mapper = new ObjectMapper();
                    JIRAISSUE_ARRAY_LIST = mapper.readValue(jiraIssueArray.toString(), new TypeReference<List<JiraIssue>>() {
                    });

                } finally {
                    http.disconnect();
                    logger.info(http.getResponseCode() + " " + http.getResponseMessage());
                    logger.info("Request {} executed in {} ms", url, (System.currentTimeMillis() - l));
                }
            }
        } catch (Exception e) {

            logger.error("Error connection: " + url);
            e.printStackTrace();
            return null;
        }
        logger.info("List return from: " + url);

        return JIRAISSUE_ARRAY_LIST;
    }
    // Method to update an existing Jira issue's status
    public static boolean updateIssueStatus(String jiraInstance, String issueKey, String transitionId) throws IOException {
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        JSONObject transitionData = new JSONObject();
        try {
            transitionData.put("transition", new JSONObject().put("id", transitionId));
        } catch (JSONException e) {
            logger.error("Error creating JSON for issue transition", e);
            return false;
        }

        URL url = new URL(jiraUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Basic " + encoding);
        http.setDoOutput(true);

        try (OutputStream os = http.getOutputStream()) {
            byte[] input = transitionData.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = http.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            logger.info("Issue " + issueKey + " status updated successfully");
            return true;
        } else {
            logger.error("Failed to update issue status: HTTP error code : " + responseCode);
            return false;
        }
    }


    // Method to get available statuses for a Jira project
    public static List<Status> getStatusList(String jiraInstance, String jiraProject)
            throws MalformedURLException, UnsupportedEncodingException {
        logger.info("Getting available statuses from " + jiraInstance + " for the project " + jiraProject);

        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/project/" + jiraProject + "/statuses";
        String jiraUsername = jiraLogin;
        String jiraOauthToken = jiraToken;
        List<Status> statusList = new ArrayList<>();

        String encoding = Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraOauthToken).getBytes("UTF-8"));
        URL url = new URL(jiraUrl);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Basic " + encoding);
            long startTime = System.currentTimeMillis();

            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream response = http.getInputStream();
                String jsonReply = convertStreamToString(response);
                JSONArray issueTypesArray = new JSONArray(jsonReply);

                // Loop through each issue type and extract statuses
                for (int i = 0; i < issueTypesArray.length(); i++) {
                    JSONObject issueTypeObject = issueTypesArray.getJSONObject(i);
                    JSONArray statusesArray = issueTypeObject.getJSONArray("statuses");

                    // Loop through each status in the statuses array
                    for (int j = 0; j < statusesArray.length(); j++) {
                        JSONObject statusObject = statusesArray.getJSONObject(j);
                        String statusId = statusObject.getString("id");
                        String statusName = statusObject.getString("name");

                        // Create a Status object and add to the list
                        Status status = new Status(statusId, statusName);
                        statusList.add(status);
                    }
                }

                logger.info("Request {} executed in {} ms", url, (System.currentTimeMillis() - startTime));
                logger.info("StatusList: {}", statusList.toString());

            } else {
                logger.error("Failed to get statuses: HTTP error code : " + http.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error connecting to Jira: " + url + ", " + e);
        }

        return statusList;
    }

    public static List<Status> getAvailableTransitions(String jiraInstance, String issueKey)
            throws MalformedURLException, UnsupportedEncodingException {
        logger.info("Getting available transitions for issue " + issueKey + " from " + jiraInstance);

        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/transitions";
        String jiraUsername = jiraLogin;  // Replace with your Jira username
        String jiraOauthToken = jiraToken; // Replace with your Jira token
        List<Status> transitionList = new ArrayList<>();

        String encoding = Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraOauthToken).getBytes("UTF-8"));
        URL url = new URL(jiraUrl);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Basic " + encoding);
            long startTime = System.currentTimeMillis();

            if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream response = http.getInputStream();
                String jsonReply = convertStreamToString(response);
                JSONObject responseObject = new JSONObject(jsonReply);
                JSONArray transitionsArray = responseObject.getJSONArray("transitions");

                // Loop through each transition and extract its details
                for (int i = 0; i < transitionsArray.length(); i++) {
                    JSONObject transitionObject = transitionsArray.getJSONObject(i);
                    String transitionId = transitionObject.getString("id");
                    String transitionName = transitionObject.getString("name");

                    // Log the details of each transition
                    logger.info("Transition ID: " + transitionId + ", Transition Name: " + transitionName);

                    // Create a Status object and add to the list
                    Status transition = new Status(transitionId, transitionName);
                    transitionList.add(transition);
                }

                logger.info("Request executed in {} ms", (System.currentTimeMillis() - startTime));
                logger.info("Transition List: {}", transitionList.toString());

            } else {
                InputStream errorStream = http.getErrorStream();
                String errorResponse = convertStreamToString(errorStream);
                logger.error("Failed to get transitions: HTTP error code : " + http.getResponseCode() + ", Response: " + errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error connecting to Jira: " + url, e);
        }

        return transitionList;
    }

    /**
     * Retrieves the list of issue types for a specific project in Jira.
     *
     * @param jiraInstance the Jira instance URL (e.g., "yourInstance")
     * @param projectKey   the project key (e.g., "PROJECT123")
     * @return a list of IssueType objects containing id, label, and logo
     * @throws MalformedURLException if the URL is malformed
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    public static List<IssueType> getIssueTypesForProject(String jiraInstance, String projectKey)
            throws MalformedURLException, UnsupportedEncodingException {

        // Jira API URL for the project
        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/project/" + projectKey;
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));
        List<IssueType> issueTypesList = new ArrayList<>();

        URL url = new URL(jiraUrl);
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Content-Type", "application/json");
            http.setRequestProperty("Authorization", "Basic " + encoding);
            http.setRequestMethod("GET");

            int responseCode = http.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream response = http.getInputStream();
                String jsonReply = convertStreamToString(response);
                JSONObject projectObject = new JSONObject(jsonReply);

                // Extract the issue types array
                JSONArray issueTypesArray = projectObject.getJSONArray("issueTypes");

                // Loop through each issue type and extract its details
                for (int i = 0; i < issueTypesArray.length(); i++) {
                    JSONObject issueTypeObject = issueTypesArray.getJSONObject(i);
                    String issueTypeId = issueTypeObject.getString("id");
                    String issueTypeName = issueTypeObject.getString("name");
                    String issueTypeIconUrl = issueTypeObject.getString("iconUrl");

                    // Create an IssueType object and add to the list
                    IssueType issueType = new IssueType(issueTypeId, issueTypeName, issueTypeIconUrl);
                    issueTypesList.add(issueType);
                }

                System.out.println("Issue Types: " + issueTypesList);

            } else {
                InputStream errorStream = http.getErrorStream();
                String errorResponse = convertStreamToString(errorStream);
                System.err.println("Failed to get issue types: HTTP error code : " + responseCode + ", Response: " + errorResponse);
            }
        } catch (IOException | JSONException e) {
            System.err.println("Error connecting to Jira: " + e.getMessage());
        }

        return issueTypesList;
    }

    // Helper method to convert InputStream to String
    private static String convertStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, length));
        }
        return sb.toString();
    }
}
