package com.example.demo.dto;

import com.example.demo.dto.ColumnWiseCondition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Updation {
    @JsonProperty("OnUpdation")
    private String onUpdation;

    @JsonProperty("ColumnwiseConditions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ColumnWiseCondition> columnWiseConditions;
}
