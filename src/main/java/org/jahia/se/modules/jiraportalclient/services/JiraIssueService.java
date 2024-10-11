package org.jahia.se.modules.jiraportalclient.services;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface JiraIssueService {

    /**
     * Creates a Jira issue.
     *
     * @param jiraInstance   the Jira instance
     * @param projectKey     the project key
     * @param summary        the summary of the issue
     * @param description    the description of the issue
     * @param issueType      the type of the issue
     * @param priority       the priority of the issue
     * @return               true if the issue was created successfully, false otherwise
     * @throws IOException   if an I/O error occurs
     */
    boolean createIssue(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority) throws IOException;

    /**
     * Updates the status of an existing Jira issue.
     *
     * @param jiraInstance   the Jira instance
     * @param issueKey       the issue key
     * @param transitionId   the transition ID (new status) to update the issue to
     * @return               true if the issue status was updated successfully, false otherwise
     * @throws IOException   if an I/O error occurs
     * @throws JSONException if a JSON error occurs during parsing
     */
    boolean updateIssueStatus(String jiraInstance, String issueKey, String transitionId) throws IOException, JSONException;

    /**
     * Adds a comment to a Jira issue.
     *
     * @param jiraInstance   the Jira instance
     * @param issueKey       the issue key
     * @param commentText    the comment text to add
     * @return               true if the comment was added successfully, false otherwise
     */
    boolean addCommentToIssue(String jiraInstance, String issueKey, String commentText) throws JSONException, IOException;

    /**
     * Creates a Jira issue with a custom field.
     *
     * @param jiraInstance      the Jira instance
     * @param projectKey        the project key
     * @param summary           the summary of the issue
     * @param description       the description of the issue
     * @param issueType         the type of the issue
     * @param priority          the priority of the issue
     * @param customFieldValue  the value for the custom field
     * @return                  true if the issue was created successfully, false otherwise
     */
    boolean createIssueWithCustomField(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority, String customFieldValue) throws IOException, JSONException;

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
    boolean moveIssue(String jiraInstance, String oldIssueKey, String targetProjectKey) throws IOException, JSONException;

    }