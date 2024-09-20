package org.jahia.se.modules.jiraportalclient.model;

public class IssueType {
    private String id;
    private String label;
    private String logo;

    public IssueType(String id, String label, String logo) {
        this.id = id;
        this.label = label;
        this.logo = logo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public String toString() {
        return "IssueType{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", logo='" + logo + '\'' +
                '}';
    }
}

