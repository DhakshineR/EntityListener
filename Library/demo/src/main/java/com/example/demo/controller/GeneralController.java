package com.example.demo.controller;

import com.example.demo.dto.BaseConfig;
import com.example.demo.dto.ConditionDTO;
import com.example.demo.service.GeneralService;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import org.json.HTTP;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.metamodel.EntityType;

@RestController
@RequestMapping("/api")
public class GeneralController {

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private GeneralService generalService;
    @GetMapping("/allEntities")
    public List<String> getAllEntites(){
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .map(EntityType::getName)
                .sorted()
                .collect(Collectors.toList());
    }
    @PostMapping("/condition")
    public ResponseEntity<String> evaluateCondition(@RequestBody ConditionDTO conditionDTO){
        if(conditionDTO != null){
            //calling functions and adding the condition to baseConfig object
            if(generalService.setBaseConfig(conditionDTO)){
                return ResponseEntity.status(HttpStatus.OK).body("Condition added successfully!!");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Input data");
    }
}


//tet

//test