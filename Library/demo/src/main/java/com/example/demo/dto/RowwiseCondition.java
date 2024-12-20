package com.example.demo.dto;

import com.example.demo.dto.Condition;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RowwiseCondition {
    @JsonProperty("ColumnName")
    private String columnName;

    @JsonProperty("Conditions")
    private List<Condition> conditions;
}

