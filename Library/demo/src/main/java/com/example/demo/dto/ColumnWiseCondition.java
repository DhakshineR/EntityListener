package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ColumnWiseCondition{
    @JsonProperty("ColumnName")
    private String columnName;

    @JsonProperty("Conditions")
    private List<Condition> conditions;
}
