package com.example.demo.dto;

import java.util.List;

public class NotificationRequest {
    private String sourceType;
    private Email email;
    private SMS sms;
    private List<Recipient> toList;

    // Getters and setters
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public SMS getSms() {
        return sms;
    }

    public void setSms(SMS sms) {
        this.sms = sms;
    }

    public List<Recipient> getToList() {
        return toList;
    }

    public void setToList(List<Recipient> toList) {
        this.toList = toList;
    }
    public static class Email {
        private String body;
        private String subject;

        // Getters and setters

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }
    public static class SMS {
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class Recipient {
        private String firstName;
        private String lastName;
        private String emailAddress;

        // Getters and setters

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
        }
    }
}
