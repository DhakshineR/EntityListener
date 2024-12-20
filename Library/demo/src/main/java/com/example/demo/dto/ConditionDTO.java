package com.example.demo.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConditionDTO {
    private String entity;
    private String columnName;
    private String condition;
    private String checkOnce;
    private String delayInMin;
    private String sourceType;
    private String groupName;
    private String trigger;
    private String subscriber;
}
