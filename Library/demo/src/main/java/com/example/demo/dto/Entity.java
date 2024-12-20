package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Entity {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Insertion")
    private Insertion insertion;

    @JsonProperty("Updation")
    private Updation updation;
}
