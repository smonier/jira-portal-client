package org.jahia.se.modules.jiraportalclient.taglibs;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.httpclient.HttpException;
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

import org.jahia.se.modules.jiraportalclient.model.JiraIssue;

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
                JSONArray jiraIssueArray = new JSONArray(jiraJsonObject.getString("issues"));
                try {

                    ObjectMapper mapper = new ObjectMapper();
                    JIRAISSUE_ARRAY_LIST = mapper.readValue(jiraIssueArray.toString(), new TypeReference<List<JiraIssue>>(){});

                } finally {
                    http.disconnect();
                    logger.info(http.getResponseCode() + " " + http.getResponseMessage());
                    logger.info("Request {} executed in {} ms", url, (System.currentTimeMillis() - l));
                }

            }
        } catch (HttpException e) {

            logger.error("Error connection: "+url);
            e.printStackTrace();
            return null;
        }
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
