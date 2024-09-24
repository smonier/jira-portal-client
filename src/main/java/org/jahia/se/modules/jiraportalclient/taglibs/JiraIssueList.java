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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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

                logger.info("Issue Types: " + issueTypesList);

            } else {
                InputStream errorStream = http.getErrorStream();
                String errorResponse = convertStreamToString(errorStream);
                logger.error("Failed to get issue types: HTTP error code : " + responseCode + ", Response: " + errorResponse);
            }
        } catch (IOException | JSONException e) {
            logger.error("Error connecting to Jira: " + e.getMessage());
        }

        return issueTypesList;
    }

    public static List<String> getIssueActivities(String jiraInstance, String issueKey) throws IOException {
        List<String> activities = new ArrayList<>();

        // Jira API URL for issue activities (comments and change history)
        String commentsUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "/comment";
       // String changelogUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/issue/" + issueKey + "?expand=changelog";
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        // Fetch comments
        activities.addAll(fetchActivitiesFromUrl(commentsUrl, encoding, "comments"));

        // Fetch changelog (history)
     //   activities.addAll(fetchActivitiesFromUrl(changelogUrl, encoding, "changelog"));

        return activities;
    }

    /**
     * Fetches activity details from a given Jira API URL.
     *
     * @param apiUrl   the Jira API URL to fetch data from
     * @param encoding the base64 encoded authentication string
     * @param type     the type of activity to fetch ("comments" or "changelog")
     * @return a list of activity details
     * @throws IOException if there is an issue with the network connection or data processing
     */
    private static List<String> fetchActivitiesFromUrl(String apiUrl, String encoding, String type) throws IOException {
        List<String> activities = new ArrayList<>();
        URL url = new URL(apiUrl);

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String jsonResponse = convertStreamToString(inputStream);
                JSONObject responseObject = new JSONObject(jsonResponse);

                if (type.equals("comments")) {
                    JSONArray commentsArray = responseObject.getJSONArray("comments");
                    for (int i = 0; i < commentsArray.length(); i++) {
                        JSONObject comment = commentsArray.getJSONObject(i);
                        String commentBody = comment.getString("body");
                        String author = comment.getJSONObject("author").getString("displayName");
                        String created = comment.getString("created");

                        activities.add("Comment by " + author + " on " + formatDate(created)+" : " + commentBody);
                    }
                } else if (type.equals("changelog")) {
                    JSONObject changelogObject = responseObject.getJSONObject("changelog");
                    JSONArray historyArray = changelogObject.getJSONArray("histories");
                    for (int i = 0; i < historyArray.length(); i++) {
                        JSONObject history = historyArray.getJSONObject(i);
                        String created = history.getString("created");
                        String user = history.getJSONObject("author").getString("displayName");
                        JSONArray itemsArray = history.getJSONArray("items");
                        for (int j = 0; j < itemsArray.length(); j++) {
                            JSONObject item = itemsArray.getJSONObject(j);
                            String field = item.getString("field");
                            String fromString = item.optString("fromString", "none");
                            String toString = item.getString("toString");
                            activities.add("Change by " + user + " on " + created + ": " + field + " changed from '" + fromString + "' to '" + toString + "'");
                        }
                    }
                }
            } else {
                logger.error("Failed to fetch activities: HTTP error code " + responseCode);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return activities;
    }

    /**
     * Retrieves all issues from a Jira project filtered by a custom field.
     *
     * @param jiraInstance the Jira instance (e.g., "yourInstance" in "yourInstance.atlassian.net")
     * @param projectKey   the key of the Jira project (e.g., "PROJECT-123")
     * @param renderContext the context of the current rendering operation
     * @return a list of JiraIssue objects matching the filter criteria
     * @throws IOException          if there is an issue with the network connection or data processing
     * @throws JSONException        if there is an error parsing the JSON response
     * @throws RepositoryException  if there is an issue accessing the repository
     * @throws ExecutionException   if there is an issue with concurrent execution
     * @throws InterruptedException if the operation is interrupted
     */
    public static List<JiraIssue> getIssuesByCustomField(String jiraInstance, String projectKey, RenderContext renderContext)
            throws IOException, JSONException, RepositoryException, ExecutionException, InterruptedException {
        List<JiraIssue> JIRAISSUE_ARRAY_LIST = new ArrayList<>();

        PortalFunctions jiraCustomProject = new PortalFunctions();

        String jiraCustomField = jiraCustomProject.getPropertyValue("jiraCustomField", renderContext, contextServerService);
        String jiraCustomFieldValue = jiraCustomProject.getPropertyValue("jiraCustomFieldValue", renderContext, contextServerService);
        String jiraProjectNameValue = jiraCustomProject.getPropertyValue("jiraProjectName", renderContext, contextServerService);

        if (jiraProjectNameValue != null) {
            projectKey = jiraProjectNameValue;
        }

        // Determine the correct JQL operator based on the field type or other information
        String jqlOperator = getJqlOperatorForField(jiraCustomField);

        // Jira API URL for search using JQL (Jira Query Language)
        String jql = String.format("project = %s AND %s %s \"%s\"", projectKey, jiraCustomField, jqlOperator, jiraCustomFieldValue);
        String jiraUrl = String.format("https://%s.atlassian.net/rest/api/2/search?jql=%s", jiraInstance, urlEncode(jql));
        String encoding = Base64.getEncoder().encodeToString((jiraLogin + ":" + jiraToken).getBytes("UTF-8"));

        logger.info("CUSTOM FIELD QUERY URL: " + jiraUrl);
        HttpURLConnection connection = null;
        BufferedReader in = null;

        try {
            URL url = new URL(jiraUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setRequestProperty("Content-Type", "application/json");

            long startTime = System.currentTimeMillis();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                InputStream responseStream = connection.getInputStream();
                String jsonResponse = convertStreamToString(responseStream);

                JSONObject jiraJsonObject = new JSONObject(jsonResponse);
                JSONArray jiraIssueArray = jiraJsonObject.optJSONArray("issues");

                logger.info("JIRA Response: " + jiraIssueArray.toString());

                if (jiraIssueArray != null) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JIRAISSUE_ARRAY_LIST = mapper.readValue(jiraIssueArray.toString(), new TypeReference<List<JiraIssue>>() {});
                    } catch (Exception e) {
                        logger.error("Error parsing JSON to JiraIssue list: ", e);
                    }
                }
            } else {
                // Capture error response body for further inspection
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    errorResponse.append(inputLine);
                }

                logger.error("Failed to retrieve issues: HTTP error code " + connection.getResponseCode() + " " + connection.getResponseMessage());
                logger.error("Error response: " + errorResponse.toString());

                throw new IOException("Failed to retrieve issues: " + errorResponse.toString());
            }

            logger.info("Request {} executed in {} ms", url, (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logger.error("Error connecting to: " + jiraUrl, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("Error closing input stream: ", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        logger.info("List returned from: " + jiraUrl);
        return JIRAISSUE_ARRAY_LIST;
    }

    /**
     * URL-encodes the given string for use in a query parameter.
     *
     * @param value the string to encode
     * @return the URL-encoded string
     * @throws IOException if encoding fails
     */
    private static String urlEncode(String value) throws IOException {
        return java.net.URLEncoder.encode(value, "UTF-8");
    }

    /**
     * Determines the appropriate JQL operator for a given custom field.
     *
     * @param field the custom field to check
     * @return the correct JQL operator (e.g., "=", "~", "in")
     */
    private static String getJqlOperatorForField(String field) {
        // This is a simple placeholder logic. You need to implement logic based on your Jira configuration.
        // For example, if 'EIN' is a text field, you might use '~' instead of '='.
        if (field.equals("EIN")) {
            return "~"; // 'contains' operator for text fields
        }
        // Add more cases for other fields as needed.
        return "="; // Default to '=' for standard fields
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

    /**
     * Formats a given date string in the format "yyyy-MM-dd'T'HH:mm:ss.SSSZ" to "MM:dd:yyyy hh:mm".
     *
     * @param dateString the input date string in the format "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
     * @return the formatted date string in the format "MM:dd:yyyy hh:mm"
     * @throws ParseException if the input date string is not in the expected format
     */
    public static String formatDate(String dateString) throws ParseException {
        // Input format matching the provided date string
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        // Desired output format
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM. dd yyyy 'at' hh:mm");

        // Parse the input date string to a Date object
        Date date = inputFormat.parse(dateString);

        // Format the Date object to the desired output format
        return outputFormat.format(date);
    }

}
