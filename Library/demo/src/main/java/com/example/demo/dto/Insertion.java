package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Insertion {
    @JsonProperty("OnInsertion")
    private String onInsertion;

    @JsonProperty("RowwiseConditions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<RowwiseCondition> rowwiseConditions;
}
