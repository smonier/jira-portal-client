package org.jahia.se.modules.jiraportalclient.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.text.ParseException;
import java.text.SimpleDateFormat;


@JsonDeserialize(using = JiraIssueDeserializer.class)
public class JiraIssue {

    private String type;
    private String typeIconUrl;
    private String key;
    private String summary;
    private String description;
    private String assignee;
    private String reporter;
    private String priority;
    private String priorityIconUrl;
    private String status;
    private String dateCreated;
    private String dateModified;
    private String ein;


//  public JiraIssue(String type, String key, String summary, String assignee, String reporter, String priority, String status, String dateCreated, String dateModified, String priorityIconUrl, String typeIconUrl) {

    public JiraIssue() {
        this.type = type;
        this.typeIconUrl = typeIconUrl;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.assignee = assignee;
        this.reporter = reporter;
        this.priority = priority;
        this.priorityIconUrl = priorityIconUrl;
        this.status = status;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
        this.ein = ein;
    }



    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getReporter() {
        return reporter;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getDateCreated() throws ParseException {
        if (dateCreated != "null") {
            String ds1 = dateCreated.substring(0, 10);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf2 = new SimpleDateFormat("E, dd MMM yyyy");
            String formatedDate = sdf2.format(sdf1.parse(ds1));
            return formatedDate;
        } else {
            return null;
        }
    }

    public String getDateModified() throws ParseException {
        if (dateModified != "null") {
            String ds1 = dateModified.substring(0, 10);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdf2 = new SimpleDateFormat("E, dd MMM yyyy");
            String formatedDate = sdf2.format(sdf1.parse(ds1));
            return formatedDate;
        } else {
            return null;
        }
    }

    public String getPriorityIconUrl() {
        return priorityIconUrl;
    }

    public String getTypeIconUrl() {
        return typeIconUrl;
    }

    public String getEin() {
        return ein;
    }


    public void setType(String type) {
        this.type = type;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateModified(String dateModified) { this.dateModified = dateModified; }

    public void setPriorityIconUrl(String priorityIconUrl) {
        this.priorityIconUrl = priorityIconUrl;
    }

    public void setTypeIconUrl(String typeIconUrl) {
        this.typeIconUrl = typeIconUrl;
    }

    public void setEin(String ein) {
        this.ein = ein;
    }

}
