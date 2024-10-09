package org.jahia.se.modules.jiraportalclient.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.se.modules.jiraportalclient.services.JiraIssueService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@Component(service = Action.class, immediate = true)
public class JiraIssueAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssueAction.class);

    private JiraIssueService jiraIssueService;

    @Activate
    public void activate() {
        setName("requestJiraUpdate");
        setRequiredMethods("GET,POST");
    }

    @Reference(service = JiraIssueService.class)
    public void setJiraIssueService(JiraIssueService jiraIssueService) {
        this.jiraIssueService = jiraIssueService;
    }

    private static String jiraLogin;
    private static String jiraToken;

    public static void setJiraCredentials(String login, String token) {
        jiraLogin = login;
        jiraToken = token;
    }

    @Override
    public ActionResult doExecute(
            final HttpServletRequest request,
            final RenderContext renderContext,
            final Resource resource,
            final JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            final URLResolver urlResolver) throws Exception {

        // Logging the parameters for debugging purposes
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            LOGGER.info("Key: " + key);
            for (String value : values) {
                LOGGER.info("Value: " + value);
            }
        }

        JSONObject resp = new JSONObject();
        boolean response = false;
        int resultCode = HttpServletResponse.SC_BAD_REQUEST;

        try {
            // Extracting jiraAction with null checks
            String jiraAction = retrieveParameter(parameters, "jiraAction");
            if (jiraAction == null) {
                LOGGER.error("Missing required parameter: jiraAction");
                return generateErrorResponse(resp, "Missing required parameter: jiraAction", resultCode);
            }

            String jiraInstance = retrieveParameter(parameters, "jiraInstance");
            if (jiraInstance == null) {
                LOGGER.error("Missing required parameter: jiraInstance");
                return generateErrorResponse(resp, "Missing required parameter: jiraInstance", resultCode);
            }

            String jiraProject = retrieveParameter(parameters, "jiraProject");

            // Switch case based on jiraAction
            switch (jiraAction) {
                case "updateStatus":
                    response = handleUpdateStatus(parameters, jiraInstance);
                    resultCode = response ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST;
                    resp.put("status", response ? "success" : "failure");
                    resp.put("message", response ? "Issue status updated successfully." : "Failed to update issue status.");
                    break;

                case "createIssue":
                    response = handleCreateIssue(parameters, jiraInstance, jiraProject);
                    resultCode = response ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST;
                    resp.put("status", response ? "success" : "failure");
                    resp.put("message", response ? "Issue created successfully." : "Failed to create issue.");
                    break;

                case "createOrder":
                    response = handleCreateOrder(parameters, session, jiraInstance, jiraProject);
                    resultCode = response ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST;
                    resp.put("status", response ? "success" : "failure");
                    resp.put("message", response ? "Issue created successfully." : "Failed to create issue.");
                    break;

                case "addComment":
                    response = handleAddComment(parameters, jiraInstance);
                    resultCode = response ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST;
                    resp.put("status", response ? "success" : "failure");
                    resp.put("message", response ? "Comment added successfully." : "Failed to add comment.");
                    break;

                default:
                    LOGGER.error("Invalid jiraAction: {}", jiraAction);
                    return generateErrorResponse(resp, "Invalid action specified.", resultCode);
            }

        } catch (Exception e) {
            LOGGER.error("Error executing Jira action: ", e);
            return generateErrorResponse(resp, "An error occurred while processing the request.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

        // Log the response and return the ActionResult
        LOGGER.info("Response: {}", resp.toString());
        return new ActionResult(resultCode, null, resp);
    }

    // Helper method to retrieve parameter from the map with null checks
    private String retrieveParameter(Map<String, List<String>> parameters, String key) {
        if (parameters.get(key) != null && !parameters.get(key).isEmpty()) {
            return parameters.get(key).get(0);
        }
        return null;
    }

    // Helper method to generate error responses
    private ActionResult generateErrorResponse(JSONObject resp, String message, int resultCode) throws Exception {
        resp.put("status", "failure");
        resp.put("message", message);
        return new ActionResult(resultCode, null, resp);
    }

    private ActionResult generateErrorResponse(JSONObject resp, String message, int resultCode, Exception e) throws Exception {
        resp.put("status", "error");
        resp.put("message", message);
        resp.put("errorDetail", e.getMessage());
        return new ActionResult(resultCode, null, resp);
    }

    // Handle Jira issue status update
    private boolean handleUpdateStatus(Map<String, List<String>> parameters, String jiraInstance) throws JSONException, IOException {
        String issueKey = retrieveParameter(parameters, "issueKey");
        String newStatus = retrieveParameter(parameters, "newStatus");
        String targetProjectKey = retrieveParameter(parameters, "targetProjectKey");
        LOGGER.info("targetProjectKey : {}", targetProjectKey);
        if (issueKey == null || newStatus == null) {
            LOGGER.error("Missing required parameters: issueKey or newStatus");
            return false;
        }

        if (newStatus.equals("14")) {
            return jiraIssueService.updateIssueStatus(jiraInstance, issueKey, newStatus) &&
                   jiraIssueService.moveIssue(jiraInstance, issueKey, targetProjectKey);
        } else {
            return jiraIssueService.updateIssueStatus(jiraInstance, issueKey, newStatus);
        }
    }

    // Handle Jira issue creation
    private boolean handleCreateIssue(Map<String, List<String>> parameters, String jiraInstance, String jiraProject) throws IOException {
        String summary = retrieveParameter(parameters, "summary");
        String description = retrieveParameter(parameters, "description");
        String issueType = retrieveParameter(parameters, "issueType");
        String priority = retrieveParameter(parameters, "priority");
        String marketNum = retrieveParameter(parameters, "marketNum");

        if (summary == null || description == null || issueType == null || priority == null || marketNum == null) {
            LOGGER.error("Missing required parameters for issue creation.");
            return false;
        }

        try {
            return jiraIssueService.createIssueWithCustomField(jiraInstance, jiraProject, summary, description, issueType, priority, marketNum);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Handle Jira issue creation
    private boolean handleCreateOrder(Map<String, List<String>> parameters, JCRSessionWrapper session, String jiraInstance, String jiraProject) throws IOException, JSONException, RepositoryException {

        JahiaUser user =  session.getUser();
        String currentUserName = user.getUsername();
        String description = null;
        JSONArray jsonArray = new JSONArray(retrieveParameter(parameters, "products"));
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String uuid = jsonObject.getString("uuid");
            description += getProductDetails(session,uuid);
            description += "Quantité : "+jsonObject.getString("qty")+"<br/><br/>";
        }
        String summary = "Nouvelle commande pour " + currentUserName;
        String issueType = "Order";
        String priority = "Medium";
        String marketNum = "9999-AAA-9999";

        if (summary == null || description == null || issueType == null || priority == null || marketNum == null) {
            LOGGER.error("Missing required parameters for issue creation.");
            return false;
        }

        try {
            return jiraIssueService.createIssueWithCustomField(jiraInstance, jiraProject, summary, description, issueType, priority, marketNum);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
        // Handle adding a comment to a Jira issue
    private boolean handleAddComment(Map<String, List<String>> parameters, String jiraInstance) {
        String commentText = retrieveParameter(parameters, "commentText");
        String commentIssueKey = retrieveParameter(parameters, "issueKey");

        if (commentText == null || commentIssueKey == null) {
            LOGGER.error("Missing required parameters for adding comment.");
            return false;
        }

        return jiraIssueService.addCommentToIssue(jiraInstance, commentIssueKey, commentText);
    }

    private String getProductDetails(JCRSessionWrapper session, String uuid) throws RepositoryException {
        String description = null;
        JCRNodeWrapper node = session.getNodeByIdentifier(uuid);
        description +=  "<br/><strong>" + node.getPropertyAsString("jcr:title") + "</strong><br/>";
        description +=  node.getPropertyAsString("teaser") + "<br/>";
        description +=  node.getPropertyAsString("price") + "EUR<br/>";

        return description;
    }
}