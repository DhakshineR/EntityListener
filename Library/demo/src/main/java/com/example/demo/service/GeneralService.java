package com.example.demo.service;

import com.example.demo.config.JsonLoader;
import com.example.demo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeneralService {
    public static BaseConfig baseConfig;
    @Autowired
    private JsonLoader jsonLoader;

    public boolean setBaseConfig(ConditionDTO conditionDTO) {
        // the method is too long should short it for further purpose
        System.out.println(conditionDTO);
        // load config file
        baseConfig = jsonLoader.getBaseConfig();
        System.out.println(baseConfig);
        if (conditionDTO != null) {
            List<Entity> entities = baseConfig.getEntities();
            // checking if entity already there in baseConfig
            if (entities != null && entities.size() >= 1) {
                for (Entity entity : entities) {
                    if (entity.getName().equalsIgnoreCase(conditionDTO.getEntity())) {
                        // if entity is already present add condition to its column
                        if (conditionDTO.getTrigger().equalsIgnoreCase("Insertion")) {
                            // add the condition to the columnName if it is already present
                            List<RowwiseCondition> rows = entity.getInsertion().getRowwiseConditions();
                            for (RowwiseCondition row : rows) {
                                if (conditionDTO.getColumnName().equalsIgnoreCase(row.getColumnName())) {
                                    // add the condition here
                                    Condition condition = new Condition();
                                    condition.setCheck(conditionDTO.getCondition());
                                    condition.setCheckOnce(conditionDTO.getCheckOnce());
                                    condition.setDelayInMin(conditionDTO.getDelayInMin());
                                    condition.setGroupName(conditionDTO.getGroupName());
                                    condition.setSourceType(conditionDTO.getSourceType());
                                    condition.setSubscriber(conditionDTO.getSubscriber());
                                    row.getConditions().add(condition);
                                    jsonLoader.loadBaseConfig(baseConfig);
                                    return true;
                                }
                            }
                            RowwiseCondition rowwiseCondition = new RowwiseCondition();
                            rowwiseCondition.setColumnName(conditionDTO.getColumnName());
                            List<Condition> conditions = new ArrayList<>();
                            Condition condition = new Condition();
                            condition.setCheck(conditionDTO.getCondition());
                            condition.setCheckOnce(conditionDTO.getCheckOnce());
                            condition.setDelayInMin(conditionDTO.getDelayInMin());
                            condition.setGroupName(conditionDTO.getGroupName());
                            condition.setSourceType(conditionDTO.getSourceType());
                            condition.setSubscriber(conditionDTO.getSubscriber());
                            conditions.add(condition);
                            rowwiseCondition.setConditions(conditions);
                            jsonLoader.loadBaseConfig(baseConfig);
                            return true;
                        } else if (conditionDTO.getTrigger().equalsIgnoreCase("Updation")) {
                            List<ColumnWiseCondition> columnWiseConditionsList = entity.getUpdation()
                                    .getColumnWiseConditions();
                            for (ColumnWiseCondition columns : columnWiseConditionsList) {
                                if (conditionDTO.getColumnName().equalsIgnoreCase(columns.getColumnName())) {
                                    Condition condition = new Condition();
                                    condition.setCheck(conditionDTO.getCondition());
                                    condition.setCheckOnce(conditionDTO.getCheckOnce());
                                    condition.setDelayInMin(conditionDTO.getDelayInMin());
                                    condition.setGroupName(conditionDTO.getGroupName());
                                    condition.setSourceType(conditionDTO.getSourceType());
                                    condition.setSubscriber(conditionDTO.getSubscriber());
                                    columns.getConditions().add(condition);
                                    jsonLoader.loadBaseConfig(baseConfig);
                                    return true;
                                }
                                ColumnWiseCondition column = new ColumnWiseCondition();
                                column.setColumnName(conditionDTO.getColumnName());
                                List<Condition> conditions = new ArrayList<>();
                                Condition condition = new Condition();
                                condition.setCheck(conditionDTO.getCondition());
                                condition.setCheckOnce(conditionDTO.getCheckOnce());
                                condition.setDelayInMin(conditionDTO.getDelayInMin());
                                condition.setGroupName(conditionDTO.getGroupName());
                                condition.setSourceType(conditionDTO.getSourceType());
                                condition.setSubscriber(conditionDTO.getSubscriber());
                                conditions.add(condition);
                                column.setConditions(conditions);
                                jsonLoader.loadBaseConfig(baseConfig);
                                return true;
                            }

                        } else {
                            // it is deletion
                        }
                    }
                }
                Entity newEntity = new Entity();
                newEntity.setName(conditionDTO.getEntity());
                if (conditionDTO.getTrigger().equalsIgnoreCase("Insertion")) {
                    Insertion insertion = new Insertion();
                    insertion.setOnInsertion("Yes");// default
                    List<RowwiseCondition> rowwiseConditionList = new ArrayList<>();
                    RowwiseCondition rowwiseCondition = new RowwiseCondition();
                    rowwiseCondition.setColumnName(conditionDTO.getColumnName());
                    List<Condition> conditions = new ArrayList<>();
                    Condition condition = new Condition();
                    condition.setCheck(conditionDTO.getCondition());
                    condition.setCheckOnce(conditionDTO.getCheckOnce());
                    condition.setDelayInMin(conditionDTO.getDelayInMin());
                    condition.setGroupName(conditionDTO.getGroupName());
                    condition.setSourceType(conditionDTO.getSourceType());
                    condition.setSubscriber(conditionDTO.getSubscriber());
                    conditions.add(condition);
                    rowwiseCondition.setConditions(conditions);
                    rowwiseConditionList.add(rowwiseCondition);
                    insertion.setRowwiseConditions(rowwiseConditionList);
                    newEntity.setInsertion(insertion);
                    System.out.println(newEntity);

                } else if (conditionDTO.getTrigger().equalsIgnoreCase("Updation")) {
                    Updation updation = new Updation();
                    updation.setOnUpdation("Yes");// default
                    List<ColumnWiseCondition> columnWiseConditionList = new ArrayList<>();
                    ColumnWiseCondition columnWiseCondition = new ColumnWiseCondition();
                    columnWiseCondition.setColumnName(conditionDTO.getColumnName());
                    List<Condition> conditions = new ArrayList<>();
                    Condition condition = new Condition();
                    condition.setCheck(conditionDTO.getCondition());
                    condition.setCheckOnce(conditionDTO.getCheckOnce());
                    condition.setDelayInMin(conditionDTO.getDelayInMin());
                    condition.setGroupName(conditionDTO.getGroupName());
                    condition.setSourceType(conditionDTO.getSourceType());
                    condition.setSubscriber(conditionDTO.getSubscriber());
                    conditions.add(condition);
                    columnWiseCondition.setConditions(conditions);
                    columnWiseConditionList.add(columnWiseCondition);
                    updation.setColumnWiseConditions(columnWiseConditionList);
                    newEntity.setUpdation(updation);
                    System.out.println(newEntity);
                } else {
                    // add deletion part here
                }
                entities.add(newEntity);
                baseConfig.setEntities(entities);
                jsonLoader.loadBaseConfig(baseConfig);
                return true;
            }
            // if not there then we should add the entity to baseConfig with the given
            // condition
            List<Entity> entityList = new ArrayList<>();
            Entity entity = new Entity();
            entity.setName(conditionDTO.getEntity());
            if (conditionDTO.getTrigger().equalsIgnoreCase("Insertion")) {
                Insertion insertion = new Insertion();
                insertion.setOnInsertion("Yes");// default
                List<RowwiseCondition> rowwiseConditionList = new ArrayList<>();
                RowwiseCondition rowwiseCondition = new RowwiseCondition();
                rowwiseCondition.setColumnName(conditionDTO.getColumnName());
                List<Condition> conditions = new ArrayList<>();
                Condition condition = new Condition();
                condition.setCheck(conditionDTO.getCondition());
                condition.setCheckOnce(conditionDTO.getCheckOnce());
                condition.setDelayInMin(conditionDTO.getDelayInMin());
                condition.setGroupName(conditionDTO.getGroupName());
                condition.setSourceType(conditionDTO.getSourceType());
                condition.setSubscriber(conditionDTO.getSubscriber());
                conditions.add(condition);
                rowwiseCondition.setConditions(conditions);
                rowwiseConditionList.add(rowwiseCondition);
                insertion.setRowwiseConditions(rowwiseConditionList);
                entity.setInsertion(insertion);
                System.out.println(entity);

            } else if (conditionDTO.getTrigger().equalsIgnoreCase("Updation")) {
                Updation updation = new Updation();
                updation.setOnUpdation("Yes");// default
                List<ColumnWiseCondition> columnWiseConditionList = new ArrayList<>();
                ColumnWiseCondition columnWiseCondition = new ColumnWiseCondition();
                columnWiseCondition.setColumnName(conditionDTO.getColumnName());
                List<Condition> conditions = new ArrayList<>();
                Condition condition = new Condition();
                condition.setCheck(conditionDTO.getCondition());
                condition.setCheckOnce(conditionDTO.getCheckOnce());
                condition.setDelayInMin(conditionDTO.getDelayInMin());
                condition.setGroupName(conditionDTO.getGroupName());
                condition.setSourceType(conditionDTO.getSourceType());
                condition.setSubscriber(conditionDTO.getSubscriber());
                conditions.add(condition);
                columnWiseCondition.setConditions(conditions);
                columnWiseConditionList.add(columnWiseCondition);
                updation.setColumnWiseConditions(columnWiseConditionList);
                entity.setUpdation(updation);
                System.out.println(entity);
            } else {
                // add deletion part here
            }
            entityList.add(entity);
            System.out.println(entityList);
            baseConfig.setEntities(entityList);
            jsonLoader.loadBaseConfig(baseConfig);
            return true;
        }
        return false;
    }
}
