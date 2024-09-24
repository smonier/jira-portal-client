package org.jahia.se.modules.jiraportalclient.services;

import org.json.JSONException;

import java.io.IOException;

public interface JiraIssueService {

    boolean createIssue(String jiraInstance, String projectKey, String summary, String description, String issueType, String priority) throws IOException ;
    boolean updateIssueStatus(String jiraInstance, String issueKey, String transitionId) throws IOException, JSONException;
    boolean addCommentToIssue(String jiraInstance, String issueKey, String commentText);

    }
