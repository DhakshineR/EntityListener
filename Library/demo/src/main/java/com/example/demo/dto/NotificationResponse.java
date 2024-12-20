package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public class NotificationResponse {
    private String status;
    private Data data;
    private List<String> unProcessedUsers;
    private List<String> messageList;

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<String> getUnProcessedUsers() {
        return unProcessedUsers;
    }

    public void setUnProcessedUsers(List<String> unProcessedUsers) {
        this.unProcessedUsers = unProcessedUsers;
    }

    public List<String> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<String> messageList) {
        this.messageList = messageList;
    }

    // Inner Class for Data
    public static class Data {
        private Map<String, List<String>> groupDetails;

        public Map<String, List<String>> getGroupDetails() {
            return groupDetails;
        }

        public void setGroupDetails(Map<String, List<String>> groupDetails) {
            this.groupDetails = groupDetails;
        }
    }

    @Override
    public String toString() {
        return "NotificationResponse{" +
                "status='" + status + '\'' +
                ", data=" + data +
                ", unProcessedUsers=" + unProcessedUsers +
                ", messageList=" + messageList +
                '}';
    }
}
