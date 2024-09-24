package org.jahia.se.modules.jiraportalclient.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.se.modules.jiraportalclient.services.JiraIssueService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.jahia.se.modules.jiraportalclient.taglibs.JiraIssueList.updateIssueStatus;

@Component(service = Action.class, immediate = true)
public class JiraIssueAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssueAction.class);

    @Activate
    public void activate() {
        setName("requestJiraUpdate");
        setRequiredMethods("GET,POST");
    }

    private JiraIssueService jiraIssueService;

    @Reference(service = JiraIssueService.class)
    public void setJiraIssueService(JiraIssueService jiraIssueService) {
        this.jiraIssueService = jiraIssueService;
    }

    public JiraIssueService getJiraIssueService() {
        return jiraIssueService;
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

        JSONObject resp = new JSONObject();
        boolean response = false;
        int resultCode = HttpServletResponse.SC_BAD_REQUEST;

        try {
            // Extract parameters
            String jiraAction = parameters.get("jiraAction").get(0);
            String jiraProject = parameters.get("jiraProject").get(0);
            String jiraInstance = parameters.get("jiraInstance").get(0);

            switch (jiraAction) {
                case "updateStatus":
                    String issueKey = parameters.get("issueKey").get(0);
                    String newStatus = parameters.get("newStatus").get(0);

                    // Perform the Jira issue status update
                    response = jiraIssueService.updateIssueStatus(jiraInstance, issueKey, newStatus);

                    // Update resultCode and response message based on the outcome
                    if (response) {
                        resultCode = HttpServletResponse.SC_OK;
                        resp.put("status", "success");
                        resp.put("message", "Issue status updated successfully.");
                    } else {
                        resultCode = HttpServletResponse.SC_BAD_REQUEST;
                        resp.put("status", "failure");
                        resp.put("message", "Failed to update issue status.");
                    }
                    break;

                case "createIssue":
                    String summary = parameters.get("summary").get(0);
                    String description = parameters.get("description").get(0);
                    String issueType = parameters.get("issueType").get(0);
                    String priority = parameters.get("priority").get(0);


                    // Perform the Jira issue status update
                    response = jiraIssueService.createIssue(jiraInstance,jiraProject,summary,description,issueType,priority);

                    // Update resultCode and response message based on the outcome
                    if (response) {
                        resultCode = HttpServletResponse.SC_OK;
                        resp.put("status", "success");
                        resp.put("message", "Issue status updated successfully.");
                    } else {
                        resultCode = HttpServletResponse.SC_BAD_REQUEST;
                        resp.put("status", "failure");
                        resp.put("message", "Failed to update issue status.");
                    }
                    break;

                case "addComment":
                    String commentText = parameters.get("commentText").get(0);
                    String commentIssueKey = parameters.get("issueKey").get(0);

                    // Perform the Jira issue status update
                    response = jiraIssueService.addCommentToIssue(jiraInstance,commentIssueKey,commentText);

                    // Update resultCode and response message based on the outcome
                    if (response) {
                        resultCode = HttpServletResponse.SC_OK;
                        resp.put("status", "success");
                        resp.put("message", "Comment added successfully for request: "+commentIssueKey);
                    } else {
                        resultCode = HttpServletResponse.SC_BAD_REQUEST;
                        resp.put("status", "failure");
                        resp.put("message", "Failed to update issue status.");
                    }
                    break;
                default:
                    // Handle any unrecognized actions
                    resultCode = HttpServletResponse.SC_BAD_REQUEST;
                    resp.put("status", "failure");
                    resp.put("message", "Invalid action specified.");
                    break;
            }

        } catch (Exception e) {
            // Handle exceptions and populate the error message in the response
            LOGGER.error("Error updating issue status: ", e);
            resultCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            resp.put("status", "error");
            resp.put("message", "An error occurred while updating the issue status.");
            resp.put("errorDetail", e.getMessage());
        }

        // Log the response object for debugging purposes
        LOGGER.info("Response: {}", resp.toString());

        // Return the ActionResult with the appropriate HTTP status code and response JSON
        return new ActionResult(resultCode, null, resp);
    }
}
