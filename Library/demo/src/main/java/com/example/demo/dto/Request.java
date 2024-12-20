package com.example.demo.dto;
import java.util.List;
import java.util.Map;
public class Request {
    private String sourceType;
    private String groupName;
    private Map<String, String> notificationKeys;
    private List<Map<String,Object>> toList;

    public List<Map<String, Object>> getToList() {
        return toList;
    }

    public void setToList(List<Map<String, Object>> toList) {
        this.toList = toList;
    }

    // Getters and Setters
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Map<String, String> getNotificationKeys() {
        return notificationKeys;
    }

    public void setNotificationKeys(Map<String, String> notificationKeys) {
        this.notificationKeys = notificationKeys;
    }

    // Optional: Constructor
    public Request() {}

    public Request(String sourceType, String groupName, Map<String, String> notificationKeys,List<Map<String,Object>> toList) {
        this.sourceType = sourceType;
        this.groupName = groupName;
        this.notificationKeys = notificationKeys;
        this.toList=toList;
    }

    @Override
    public String toString() {
        return "Request{" +
                "sourceType='" + sourceType + '\'' +
                ", groupName='" + groupName + '\'' +
                ", notificationKeys=" + notificationKeys +
                ", toList=" + toList +
                '}';
    }
}
