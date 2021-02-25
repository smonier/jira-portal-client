package org.jahia.se.modules.jiraportalclient.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;


public class JiraIssueDeserializer extends StdDeserializer<JiraIssue> {
    private static Logger logger = LoggerFactory.getLogger(JiraIssueDeserializer.class);

    private static final String JIRAISSUE_KEY = "/key";
    private static final String JIRAISSUE_DATECREATED = "/fields/created";
    private static final String JIRAISSUE_DATEMODIFIED = "/fields/updated";
    private static final String JIRAISSUE_SUMMARY = "/fields/summary";
    private static final String JIRAISSUE_PRIORITY = "/fields/priority/name";
    private static final String JIRAISSUE_PRIORITYICONURL = "/fields/priority/iconUrl";
    private static final String JIRAISSUE_ASSIGNEE = "/fields/assignee/displayName";
    private static final String JIRAISSUE_REPORTER = "/fields/reporter/displayName";
    private static final String JIRAISSUE_STATUS = "/fields/status/name";
    private static final String JIRAISSUE_TYPE = "/fields/issuetype/name";
    private static final String JIRAISSUE_TYPEICONURL = "/fields/issuetype/iconUrl";


    public JiraIssueDeserializer() {
        this(null);
    }

    public JiraIssueDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JiraIssue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        ObjectCodec oc = jsonParser.getCodec();
        JsonNode jiraNode = oc.readTree(jsonParser);
        JiraIssue jiraAsset = new JiraIssue();

        JsonNode jiraIssueKey = jiraNode.at(JIRAISSUE_KEY);
        jiraAsset.setKey(jiraIssueKey.textValue());
        logger.info("Deserializing Jira Issue: "+jiraIssueKey.textValue());

        JsonNode jiraIssueDateCreated = jiraNode.at(JIRAISSUE_DATECREATED);
        jiraAsset.setDateCreated(jiraIssueDateCreated.textValue());
        JsonNode jiraIssueDateModified = jiraNode.at(JIRAISSUE_DATEMODIFIED);
        jiraAsset.setDateModified(jiraIssueDateModified.textValue());
        JsonNode jiraIssueSummary = jiraNode.at(JIRAISSUE_SUMMARY);
        jiraAsset.setSummary(jiraIssueSummary.textValue());
        JsonNode jiraIssuePriority = jiraNode.at(JIRAISSUE_PRIORITY);
        jiraAsset.setPriority(jiraIssuePriority.textValue());
        JsonNode jiraIssuePriorityIconUrl = jiraNode.at(JIRAISSUE_PRIORITYICONURL);
        jiraAsset.setPriorityIconUrl(jiraIssuePriorityIconUrl.textValue());
        JsonNode jiraIssueAssignee = jiraNode.at(JIRAISSUE_ASSIGNEE);
        jiraAsset.setAssignee(jiraIssueAssignee.textValue());
        JsonNode jiraIssueReporter = jiraNode.at(JIRAISSUE_REPORTER);
        jiraAsset.setReporter(jiraIssueReporter.textValue());
        JsonNode jiraIssueStatus = jiraNode.at(JIRAISSUE_STATUS);
        jiraAsset.setStatus(jiraIssueStatus.textValue());
        JsonNode jiraIssueType = jiraNode.at(JIRAISSUE_TYPE);
        jiraAsset.setType(jiraIssueType.textValue());
        JsonNode jiraIssueTypeIconUrl = jiraNode.at(JIRAISSUE_TYPEICONURL);
        jiraAsset.setTypeIconUrl(jiraIssueTypeIconUrl.textValue());
        return jiraAsset;
    }

}
