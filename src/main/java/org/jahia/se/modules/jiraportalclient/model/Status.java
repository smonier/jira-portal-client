package org.jahia.se.modules.jiraportalclient.model;

public class Status {
    private String id;
    private String value;

    public Status(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Status{" +
                "id='" + id + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
