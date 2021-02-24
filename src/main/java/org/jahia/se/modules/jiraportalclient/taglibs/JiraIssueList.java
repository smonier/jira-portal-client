package org.jahia.se.modules.jiraportalclient.taglibs;


import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import org.jahia.se.modules.jiraportalclient.classes.JiraIssue;
@Component
public class JiraIssueList {

    private static Logger logger = LoggerFactory.getLogger(JiraIssueList.class);

    private static String jiraLogin;
    private static String jiraToken;

    @Activate
    public void activate(Map<String, ?> props) {
        try {
            setJiraLogin((String) props.get("jiraLogin"));
            setJiraToken((String) props.get("jiraToken"));
            logger.info(getJiraLogin()+getJiraToken());
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


    public static List<JiraIssue> getJiraIssues(String jiraInstance, String jiraProject) throws IOException, JSONException {
        logger.info("Getting Jira Issues from "+jiraInstance+" for the project "+jiraProject);

        String jiraUrl = "https://" + jiraInstance + ".atlassian.net/rest/api/2/search?jql=project=" + jiraProject;
        String jiraUsername = jiraLogin;
        String jiraOauthToken = jiraToken;
        List<JiraIssue> JIRAISSUE_ARRAY_LIST = new ArrayList<>();
        String jsonReply;
        String encoding = Base64.getEncoder().encodeToString((jiraUsername + ":" + jiraOauthToken).getBytes("UTF-8"));
        URL url = new URL(jiraUrl);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Basic " + encoding);

        if (http.getResponseCode() == 201 || http.getResponseCode() == 200) {
            InputStream response = http.getInputStream();
            jsonReply = convertStreamToString(response);
            JSONObject jiraJsonObject = new JSONObject(jsonReply);
            JSONArray jiraIssueArray = new JSONArray(jiraJsonObject.getString("issues"));
            try {
                for (int i = 0; i < jiraIssueArray.length(); i++) {
                    JSONObject array1 = jiraIssueArray.getJSONObject(i);
                    String key = array1.getString("key");
                    JSONObject array2 = array1.getJSONObject("fields");
                    String dateCreated = array2.getString("created");
                    String dateModified = array2.getString("updated");
                    String summary = array2.getString("summary");
                    JSONObject array3 = array2.getJSONObject("priority");
                    String priority = array3.getString("name");
                    String priorityIconUrl = array3.getString("iconUrl");
                    JSONObject array4 = array2.getJSONObject("assignee");
                    String assignee = array4.getString("displayName");
                    JSONObject array5 = array2.getJSONObject("reporter");
                    String reporter = array5.getString("displayName");
                    JSONObject array6 = array2.getJSONObject("status");
                    String status = array6.getString("name");
                    JSONObject array7 = array2.getJSONObject("issuetype");
                    String type = array7.getString("name");
                    String typeIconUrl = array7.getString("iconUrl");



                    JIRAISSUE_ARRAY_LIST.add(new JiraIssue(type, key, summary, assignee, reporter, priority, status, dateCreated, dateModified, priorityIconUrl, typeIconUrl));

                }
            } catch (JSONException e) {
                logger.error("Error parsing JSONObject in JSONArray - Looping Issues");
                e.printStackTrace();
            }
        }
        logger.info(http.getResponseCode() + " " + http.getResponseMessage());
        return JIRAISSUE_ARRAY_LIST;
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
