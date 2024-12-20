package com.example.demo.listener;

import com.example.demo.config.JsonLoader;
import com.example.demo.dto.*;
import com.example.demo.service.NotificationService;
import com.example.demo.util.DynamicRepositoryResolver;
import com.example.demo.util.SpringContext;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;

import org.apache.commons.jexl3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EntityListener implements PostUpdateEventListener, PostInsertEventListener {
    static Logger logger = LogManager.getLogger(EntityListener.class);
    private BaseConfig baseConfig;
    private JsonLoader jsonLoader;
    private NotificationService notificationService;
    private DynamicRepositoryResolver repositoryResolver;
    private Object resolvedValue;
    @PersistenceContext
    private EntityManager entityManager;

//    private static final String REDIS_CHANNEL = "entity-status-channel";
//    private final Jedis jedis;
//    public EntityListener() {
//        this.jedis = new Jedis("localhost", 6379); // Redis connection
//    }

    //loading json config
    private void loadJsonConfig() {
        if(this.entityManager==null){
            this.entityManager= SpringContext.getBean(EntityManager.class);
        }
        if(this.repositoryResolver==null){
            this.repositoryResolver = SpringContext.getBean(DynamicRepositoryResolver.class);
        }
        if(this.notificationService==null){
            this.notificationService = SpringContext.getBean(NotificationService.class);
        }
        if(this.jsonLoader == null){
            this.jsonLoader = SpringContext.getBean(JsonLoader.class);
        }
        this.baseConfig = jsonLoader.getBaseConfig();
    }

    //on post insert methods
    @Override
    public void onPostInsert(PostInsertEvent postInsertEvent) {
        //load json file everytime
        loadJsonConfig();
        // Find configuration for insertion
        Object updatedEntity = postInsertEvent.getEntity();
        Entity configEntity = getConfigEntity(updatedEntity.getClass().getSimpleName());
        if (configEntity != null) {
            System.out.println("PostPersist for entity: " + updatedEntity.getClass().getSimpleName());
            logger.info("PostPersist for entity: " + updatedEntity.getClass().getSimpleName());
            Insertion insertion = configEntity.getInsertion();
            if("Yes".equalsIgnoreCase(insertion.getOnInsertion())){
                String message = "Row is added to "+ updatedEntity.getClass().getSimpleName()+" : "+ updatedEntity;
                System.out.println(message);
            }
            if(insertion.getRowwiseConditions() != null){
                checkRowWiseConditions(insertion.getRowwiseConditions(),updatedEntity);
            }
        }
    }

    private void checkRowWiseConditions(List<RowwiseCondition> rowwiseConditions, Object entity) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        JpaRepository<?, ?> repository = repositoryResolver.getRepositoryForEntity(entity.getClass());
        if (repository != null) {
            System.out.println("Repository found for entity: " + entity.getClass().getSimpleName());
        } else {
            System.err.println("No repository found for entity: " + entity.getClass().getSimpleName());
        }
        for (RowwiseCondition row: rowwiseConditions) {
            String columnName = row.getColumnName();
            List<Condition> conditions = row.getConditions();
            for (Condition condition:conditions) {
                String checkExpression = condition.getCheck();
                String delay = condition.getDelayInMin();
                String checkOnce = condition.getCheckOnce();
                Runnable task = () -> {
                    Object repoEntity = null;
                    try {
                        // Get the primary key value from the entity
                        Object primaryKeyValue = getPrimaryKeyValue(entity);
                        Optional<?> entityOptional = null;
                        // Get the primary key's type dynamically
                        Class<?> primaryKeyType = primaryKeyValue.getClass();
                        if (primaryKeyType.equals(Long.class)) {
                            Long pk = (Long) primaryKeyValue;
                            entityOptional = ((JpaRepository<Object, Long>) repository).findById(pk);
                        } else if (primaryKeyType.equals(String.class)) {
                            String pk = (String) primaryKeyValue;
                            entityOptional = ((JpaRepository<Object, String>) repository).findById(pk);
                        } else if (primaryKeyType.equals(Integer.class)) {
                            Integer pk = (Integer) primaryKeyValue;
                            entityOptional = ((JpaRepository<Object, Integer>) repository).findById(pk);
                        } else {
                            System.err.println("Unsupported primary key type: " + primaryKeyType.getSimpleName());
                        }
                        if(entityOptional!=null && entityOptional.isPresent()){
                            repoEntity= entityOptional.get();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Boolean result = replacePlaceholders(checkExpression,repoEntity, columnName);
                    if (result) {
                        try {
                            System.out.println(entity);
                            entityManager.persist(entity);
                            entityManager.clear();
                            sendingNotification(condition,entity);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }else{
                        scheduler.shutdown();
                    }
                };
                if (checkOnce != null){
                    if(checkOnce.equalsIgnoreCase("Yes")){
                        int delayInMinutes = delay != null && !delay.isEmpty() ? Integer.parseInt(delay) : 0;
                        if(delayInMinutes>0){
                            scheduler.schedule(task, delayInMinutes, TimeUnit.MINUTES);
                        }else{
                            task.run();
                        }
                    }// add code if checkOnce is "No"
                }else {
                    System.out.println("There is no check condition here");
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void sendingNotification(Condition condition, Object entity) throws IllegalAccessException {
        System.out.println("inside sending notification");
        System.out.println(entity);
        Request request = new Request();
        request.setSourceType(condition.getSourceType());
        //get the group by group name from group repository.
        String groupName = condition.getGroupName();
        System.out.println(groupName);
        request.setGroupName(condition.getGroupName());
        Map<String, String> notificationKeys = new HashMap<>();
        String fields = null;
        try {
            Class<?> groupClass = Class.forName("com.lnt.hmi.ffa.entities.Group", true, Thread.currentThread().getContextClassLoader());
            JpaRepository<?, ?> groupRepository = repositoryResolver.getRepositoryForEntity(groupClass);
            System.out.println(groupRepository.getClass().getName());
            // Resolve the repositories dynamically
            Class<?> subscriptionTemplateClass = Class.forName("com.lnt.hmi.ffa.entities.SubscriptionTemplate", true, Thread.currentThread().getContextClassLoader());
            JpaRepository<?, ?> subscriptionTemplateRepository = repositoryResolver.getRepositoryForEntity(subscriptionTemplateClass);
            System.out.println(subscriptionTemplateRepository.getClass().getName());
//            if(entity.getClass().getDeclaredField("id")==null){
//                entityManager.persist(entity);
//            }else{
//                entityManager.merge(entity);
//            }
            // Use reflection to invoke findByGroupName
            Method findByGroupNameMethod = groupRepository.getClass().getMethod("findByGroupName", String.class);
            Object group = findByGroupNameMethod.invoke(groupRepository, groupName);
            System.out.println(group);

            if (group!=null) {
                // Use reflection to get the template ID from the group
                Method getTemplateIdMethod = group.getClass().getMethod("getLntdsEmailTemplateId");
                Object templateId = getTemplateIdMethod.invoke(group);
                System.out.println(templateId);
                // Get the repository's primary key type
                Class<?> idType = subscriptionTemplateRepository.getClass()
                        .getDeclaredMethod("findById", Object.class)
                        .getParameterTypes()[0]; // Get the expected ID type

                // Ensure that templateId is cast to the correct type
                Object typedTemplateId = idType.cast(templateId);

                // Fetch the subscription template by ID dynamically
                Method findByIdMethod = subscriptionTemplateRepository.getClass().getMethod("findById", idType); // Use the correct ID type
                Optional<?> templateOptional = (Optional<?>) findByIdMethod.invoke(subscriptionTemplateRepository, typedTemplateId);
                System.out.println(templateOptional);
                if (templateOptional.isPresent()) {
                    Object subscriptionTemplate = templateOptional.get();

                    // Use reflection to get the fields from the subscription template
                    Method getFieldsMethod = subscriptionTemplate.getClass().getMethod("getFields");
                    fields = (String) getFieldsMethod.invoke(subscriptionTemplate);
                    System.out.println(fields);
                }
            }
        } catch (InvocationTargetException e) {
            System.err.println("InvocationTargetException: ");
            e.getCause().printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

//        Group group=groupRepository.findByGroupName(groupName);
//        System.out.println(group);
//        //get the keys to be sent from the subscription templates table or get from payload only.
//        SubscriptionTemplate template = subscriptionTemplateRepository.findById(group.getLntdsEmailTemplateId()).get();
//        System.out.println(template);
//        String result = template.getFields();
//        System.out.println(result);
        if(fields!=null){
            fields = fields.replaceAll("[\\[\\]]", "");
            System.out.println(fields);
            if(fields.contains(",")){
                List<String> keys = Arrays.asList(fields.split(","));
                try {
                    for (String key : keys) {
                        //traverse through all the fields and get the values of it for eg:, {{id}},{{priority.codeDisplayText}}
                        for (Field field : entity.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            if (key.startsWith(field.getName())) {
                                System.out.println(key);
                                Object fieldValue = null;
                                if (key.contains(".")) {
                                    String nestedFields = key.substring(field.getName().length() + 1); // Skip the mainField and dot
                                    fieldValue = resolveNestedFieldValue(field.get(entity), nestedFields);
                                } else {
                                    fieldValue = field.get(entity);
                                }
                                if (fieldValue != null || !fieldValue.toString().isEmpty()) {
                                    if (fieldValue instanceof String) {
                                        notificationKeys.put(fields, (String) fieldValue);
                                    } else {
                                        notificationKeys.put(fields, fieldValue.toString()); // Convert other types to String
                                    }
                                }
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }else{
                for (Field field : entity.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (fields.startsWith(field.getName())) {
                        Object fieldValue = null;
                        if (fields.contains(".")) {
                            String nestedFields = fields.substring(field.getName().length() + 1); // Skip the mainField and dot
                            fieldValue = resolveNestedFieldValue(field.get(entity), nestedFields);
                        } else {
                            fieldValue = field.get(entity);
                        }
                        if (fieldValue != null || !fieldValue.toString().isEmpty()) {
                            if (fieldValue instanceof String) {
                                notificationKeys.put(fields, (String) fieldValue);
                            } else {
                                notificationKeys.put(fields, fieldValue.toString()); // Convert other types to String
                            }
                        }
                    }
                }
            }
        }
        request.setNotificationKeys(notificationKeys);
        List<Map<String, Object>> toList = new ArrayList<>();
        //configure the toList users to send the notification.
        String subscriber = condition.getSubscriber();
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object resolvedValue = null;
            if (subscriber.startsWith(field.getName())) {
                if(subscriber.contains(".")){
                    // Extract the nested fields after the mainField
                    String nestedFields = subscriber.substring(field.getName().length() + 1); // Skip the mainField and dot
                    try {
                        resolvedValue = resolveNestedFieldValue(field.get(entity), nestedFields);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    resolvedValue= field.get(entity);
                }
                // Replace the expression and evaluate
                if (resolvedValue != null) {
                    System.out.println(resolvedValue);
                    try {
                        // Get class of the resolvedValue
                        Class<?> clazz = resolvedValue.getClass();

                        // Create a map to store the actual field values
                        Map<String, Object> map = new HashMap<>();

                        // Retrieve and store the values of the fields
                        map.put("userId", getFieldValue(clazz, resolvedValue, "userId"));
                        map.put("firstName", getFieldValue(clazz, resolvedValue, "firstName"));
                        map.put("lastName", getFieldValue(clazz, resolvedValue, "lastName"));
                        map.put("emailAddress", getFieldValue(clazz, resolvedValue, "emailAddress"));
                        map.put("phoneNumber", getFieldValue(clazz, resolvedValue, "phoneNumber"));

                        // Add the map to the list
                        toList.add(map);

                        // Debugging: Print the extracted map
                        System.out.println("Extracted Map: " + map);
                    }catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        request.setToList(toList);
        System.out.println(request);
        // request for notification
        notificationService.sendNotification(request);
    }
    private static Object getFieldValue(Class<?> clazz, Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName); // Get field
        field.setAccessible(true); // Allow access to private fields
        return field.get(instance); // Retrieve the value from the instance
    }

    private Boolean replacePlaceholders(String expression, Object entity, String mainField) {
        try {
            // Resolve the main field value
            Field mainFieldRef = entity.getClass().getDeclaredField(mainField);
            mainFieldRef.setAccessible(true);
            Object mainFieldValue = mainFieldRef.get(entity);
            if (expression.contains(".") && mainFieldValue!=null) {
                String regex = "(==|!=|<=|>=|<|>)";
                // Handle nested fields
                String[] parts = expression.split(regex); // Split into field and expected value
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid expression format: " + expression);
                }
                String fieldChain = parts[0].trim(); // The chain of fields, e.g., "workOrder.priority.codeDisplayText"
                if (fieldChain.startsWith(mainField)) {
                    // Extract the nested fields after the mainField
                    String nestedFields = fieldChain.substring(mainField.length() + 1); // Skip the mainField and dot
                    Object resolvedValue = resolveNestedFieldValue(mainFieldValue, nestedFields);

                    // Replace the expression and evaluate
                    if (resolvedValue != null) {
                        expression = expression.replace(parts[0], resolvedValue.toString());
                        return evaluateExpression(expression); // Evaluate the final expression
                    }
                }
            }
            else if(expression.contains(":")){
                String regex = "(==|!=|<=|>=|<|>)";
                // Handle nested fields
                String[] parts = expression.split(regex);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid expression format: " + expression);
                }
                String field = parts[0].trim();
                if(field.startsWith(mainField) && mainFieldValue!=null && field.equalsIgnoreCase(mainField + ":Time")){
                    LocalTime timeToCheck = extractTimeFromValue((String) mainFieldValue);
                    if(timeToCheck!=null){
                        expression = expression.replace(parts[0], timeToCheck.toString());
                        return evaluateExpression(expression);
                    }
                }
            }
            else {
                // Handle simple fields
                for (Field field : entity.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (expression.contains(field.getName())) {
                        Object fieldValue = field.get(entity);
                        if (fieldValue == null || fieldValue.toString().isEmpty()) {
                            return evaluateExpression(expression.replace(field.getName(), ""));
                        } else {
                            expression = expression.replace(field.getName(), fieldValue.toString());
                            return evaluateExpression(expression);
                        }
                    }
                }
            }

            System.out.println(expression); // Debug log
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    private LocalTime extractTimeFromValue(String value) {
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss.S"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format: " + value, e);
        }
    }

    //on post update methods
    @Override
    public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
        //load config file everytime
        loadJsonConfig();
        Object updatedEntity = postUpdateEvent.getEntity();
        Entity configEntity = getConfigEntity(updatedEntity.getClass().getSimpleName());
        if (configEntity != null) {
            System.out.println("PostUpdate for entity: " + postUpdateEvent.getClass().getSimpleName());
            logger.info("PostUpdate for entity: " + postUpdateEvent.getClass().getSimpleName());
            System.out.println(configEntity.toString());
            Updation updation = configEntity.getUpdation();
            if(updation.getColumnWiseConditions()!=null){
                checkColumnWiseConditions(updation.getColumnWiseConditions(),updatedEntity);
            }
        }
        if (configEntity != null) {
            Updation updation = configEntity.getUpdation();
            String onUpdation = updation.getOnUpdation();
            if(onUpdation.equalsIgnoreCase("Yes")){
                String[] updatedProperties = postUpdateEvent.getPersister().getPropertyNames();
                Object[] previousState = postUpdateEvent.getOldState();
                StringBuilder message = new StringBuilder();
                for (int i = 1; i < updatedProperties.length; i++) {
                    Object oldValue = previousState[i];
                    Object newValue = getUpdatedValue(updatedEntity, updatedProperties[i]);
                    if ((oldValue != null && !oldValue.equals(newValue)) || (oldValue == null && newValue != null)) {
                        message.append("   The field '")
                                .append(updatedProperties[i])
                                .append("'  updated from  ")
                                .append(oldValue != null ? oldValue.toString() : "null")
                                .append("  to  ")
                                .append(newValue != null ? newValue.toString() : "null");
                    }
                }
//                jedis.publish(REDIS_CHANNEL, message.toString());
            }
        }
    }
    private Object getUpdatedValue(Object entity, String propertyName) {
        try {
            return entity.getClass().getMethod("get" + capitalize(propertyName)).invoke(entity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void checkColumnWiseConditions(List<ColumnWiseCondition> columnWiseConditions, Object entity) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        JpaRepository<?, ?> repository = repositoryResolver.getRepositoryForEntity(entity.getClass());
        if (repository != null) {
            System.out.println("Repository found for entity: " + entity.getClass().getSimpleName());
        } else {
            System.err.println("No repository found for entity: " + entity.getClass().getSimpleName());
        }
        for (ColumnWiseCondition column:columnWiseConditions) {
            String columnName = column.getColumnName();
            List<Condition> conditions = column.getConditions();
            try {
                Field columnField = entity.getClass().getDeclaredField(columnName);
                columnField.setAccessible(true);
                for (Condition condition : conditions) {
                    String check = condition.getCheck();
                    String delayInMin = condition.getDelayInMin();
                    String checkOnce = condition.getCheckOnce();
                    Runnable task = () -> {
                        Object repoEntity = null;
                        try {
                            // Get the primary key value from the entity
                            Object primaryKeyValue = getPrimaryKeyValue(entity);
                            Optional<?> entityOptional = null;
                            // Get the primary key's type dynamically
                            Class<?> primaryKeyType = primaryKeyValue.getClass();
                            if (primaryKeyType.equals(Long.class)) {
                                Long pk = (Long) primaryKeyValue;
                                entityOptional = ((JpaRepository<Object, Long>) repository).findById(pk);
                            } else if (primaryKeyType.equals(String.class)) {
                                String pk = (String) primaryKeyValue;
                                entityOptional = ((JpaRepository<Object, String>) repository).findById(pk);
                            } else if (primaryKeyType.equals(Integer.class)) {
                                Integer pk = (Integer) primaryKeyValue;
                                entityOptional = ((JpaRepository<Object, Integer>) repository).findById(pk);
                            } else {
                                System.err.println("Unsupported primary key type: " + primaryKeyType.getSimpleName());
                            }
                            if(entityOptional!=null && entityOptional.isPresent()){
                                repoEntity= entityOptional.get();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        boolean value = checkCondition(check, repoEntity, columnName);
                        if(value){
                            try {
                                sendingNotification(condition,entity);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            scheduler.shutdown();
                        }
                    };
                    if(check!=null) {
                        if(checkOnce.equalsIgnoreCase("Yes")) {
                            int delayInMinutes = delayInMin != null && !delayInMin.isEmpty() ? Integer.parseInt(delayInMin) : 0;
                            if(delayInMinutes>0) {
                                scheduler.schedule(task,delayInMinutes,TimeUnit.MINUTES);
                            }else{
                                task.run();
                            }
                        }else{
                            int delayInMinutes = delayInMin != null && !delayInMin.isEmpty() ? Integer.parseInt(delayInMin) : 0;
                            if(delayInMinutes>0) {
                                scheduler.scheduleAtFixedRate(task,0,delayInMinutes,TimeUnit.MINUTES);
                            }else{
                                task.run();
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkCondition(String check, Object entity, String columnName) {
        try {
            Field columnField = entity.getClass().getDeclaredField(columnName);
            columnField.setAccessible(true); // Allow access to private fields
            Object columnValue = columnField.get(entity);// Retrieve the field value
            if (check.contains(",") && columnValue!=null) {
                String[] parts = check.split(",");
                // Handle the case where there are multiple values in the check condition
                if(parts.length<=2){
                    if (parts[0].contains("Any") && !parts[1].contains("Any")) {
                        if (columnValue.toString().equalsIgnoreCase(parts[1])) {
                            return true;
                        }else{
                            if(parts[1].contains(".")){
                                String regex = "(==|!=|<=|>=|<|>)";
                                // Handle nested fields
                                String[] innerParts = parts[1].split(regex); // Split into field and expected value
                                if (innerParts.length != 2) {
                                    throw new IllegalArgumentException("Invalid expression format: " + parts[1]);
                                }
                                String fieldChain = innerParts[0].trim(); // The chain of fields, e.g., "workOrder.priority.codeDisplayText"
                                if (fieldChain.startsWith(columnName)) {
                                    // Extract the nested fields after the mainField
                                    String nestedFields = fieldChain.substring(columnName.length() + 1); // Skip the mainField and dot
                                    Object resolvedValue = resolveNestedFieldValue(columnValue, nestedFields);

                                    // Replace the expression and evaluate
                                    if (resolvedValue != null) {
                                        parts[1] = parts[1].replace(innerParts[0], resolvedValue.toString());
                                        return evaluateExpression(innerParts[1]); // Evaluate the final expression
                                    }
                                }
                            }
                        }
                    } else if (!parts[0].contains("Any") && parts[1].contains("Any")) {
                        if (columnValue.toString().equalsIgnoreCase(parts[0])) {
                            return true;
                        }else{
                            if(parts[0].contains(".")){
                                String regex = "(==|!=|<=|>=|<|>)";
                                // Handle nested fields
                                String[] innerParts = parts[0].split(regex); // Split into field and expected value
                                if (innerParts.length != 2) {
                                    throw new IllegalArgumentException("Invalid expression format: " + parts[0]);
                                }
                                String fieldChain = innerParts[0].trim(); // The chain of fields, e.g., "workOrder.priority.codeDisplayText"
                                if (fieldChain.startsWith(columnName)) {
                                    // Extract the nested fields after the mainField
                                    String nestedFields = fieldChain.substring(columnName.length() + 1); // Skip the mainField and dot
                                    Object resolvedValue = resolveNestedFieldValue(columnValue, nestedFields);

                                    // Replace the expression and evaluate
                                    if (resolvedValue != null) {
                                        parts[0] = parts[0].replace(innerParts[0], resolvedValue.toString());
                                        return evaluateExpression(innerParts[1]); // Evaluate the final expression
                                    }
                                }
                            }
                        }
                    } else if (parts[0].contains("Any") && parts[1].contains("Any")) {
                        return true;
                    } else {
                        if (parts[1].contains("/")) {
                            String[] innerParts = parts[1].split("/");
                            return !columnValue.toString().equalsIgnoreCase(innerParts[0]) && !columnValue.toString().equalsIgnoreCase(innerParts[1]);
                        } else {
                            return !columnValue.toString().equalsIgnoreCase(parts[1]);
                        }
                    }
                }
            } else {
                // Handle the single value condition
                if (columnValue.toString().equalsIgnoreCase(check.trim())) {
                    return true;
                } else {
                    for (Field field : entity.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        if (check.contains(field.getName())) {
                            Object fieldValue = field.get(entity);
                            if (fieldValue != null) {
                                check = check.replace(field.getName(), fieldValue.toString());
                            }
                        }
                    }
                    return evaluateExpression(check);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister entityPersister) {
        return false;
    }

    //get primary key value method used in both post update and post insert methods
    private Object getPrimaryKeyValue(Object entity) throws IllegalAccessException {
        Class<?> entityClass = entity.getClass();
        // Loop through all fields of the entity class
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                // Found the primary key field
                field.setAccessible(true); // Ensure we can access private fields
                return field.get(entity); // Return the primary key value
            }
        }
        throw new IllegalStateException("No primary key (@Id) field found in entity: " + entityClass.getSimpleName());
    }

    // resolve nested field value method used both in post update and post insert methods

    /**
     * Recursively resolves the value of a nested field chain.
     *
     * @param entity The root object to start from.
     * @param fieldChain The chain of fields separated by dots.
     * @return The resolved value, or null if any field in the chain is null or not found.
     */
    private Object resolveNestedFieldValue(Object entity, String fieldChain) {
        try {
            if (entity == null || fieldChain == null || fieldChain.isEmpty()) {
                return null; // Base case
            }

            String[] fields = fieldChain.split("\\.", 2); // Split into current field and remaining chain
            String currentField = fields[0];
            String remainingChain = fields.length > 1 ? fields[1] : null;

            Field field = entity.getClass().getDeclaredField(currentField);
            field.setAccessible(true);
            Object fieldValue = field.get(entity);

            // Recurse if there's more in the chain; otherwise, return the resolved value
            return (remainingChain != null) ? resolveNestedFieldValue(fieldValue, remainingChain) : fieldValue;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error resolving field chain: " + fieldChain);
            e.printStackTrace();
        }
        return null;
    }

    //expression evaluation method
    public boolean evaluateExpression(String expression) {
        try {
            System.out.println("Original Expression: " + expression);

            expression = replaceTimestamps(expression);
            expression = replaceTimes(expression);
            expression = quoteStrings(expression);

            System.out.println("Transformed Expression: " + expression);
            JexlEngine jexl = new JexlBuilder().create();
            JexlExpression jexlExpression = jexl.createExpression(expression);
            JexlContext context = new MapContext();
            Object result = jexlExpression.evaluate(context);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error evaluating expression: " + expression);
            e.printStackTrace();
            return false;
        }
    }

    private String replaceTimestamps(String expression) {
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+");
        Matcher matcher = pattern.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String timestamp = matcher.group();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
                long epochMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                matcher.appendReplacement(result, String.valueOf(epochMillis));
            } catch (Exception ex) {
                System.err.println("Error parsing timestamp: " + timestamp);
                ex.printStackTrace();
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String quoteStrings(String expression) {
        Pattern pattern = Pattern.compile("\\b([a-zA-Z]+)\\b");
        Matcher matcher = pattern.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            matcher.appendReplacement(result, "\"" + token + "\"");
        }
        matcher.appendTail(result);
        return result.toString();
    }
    private String replaceTimes(String expression) {
        Pattern pattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}\\.\\d+");
        Matcher matcher = pattern.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String time = matcher.group();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.S");
                LocalTime localTime = LocalTime.parse(time, formatter);
                long secondsSinceMidnight = localTime.toSecondOfDay();
                matcher.appendReplacement(result, String.valueOf(secondsSinceMidnight));
            } catch (Exception ex) {
                System.err.println("Error parsing time: " + time);
                ex.printStackTrace();
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    //message printing method
    private void replacePlaceholdersInMessage(String message, Object entity) {
        try {
            // Replace placeholders like ${id}, ${workOrderId}, etc.
            Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
            Matcher matcher = pattern.matcher(message);

            while (matcher.find()) {
                // Extract the substring inside ${}
                String fieldChain = matcher.group(1);
                Object resolvedValue = resolveNestedFieldValue(entity, fieldChain);
                message = message.replace("${" + fieldChain + "}", resolvedValue != null ? resolvedValue.toString() : "null");
            }
//            for (Field field : entity.getClass().getDeclaredFields()) {
//                field.setAccessible(true);
//                String placeholder = "${" + field.getName() + "}";
//                if (message.contains(placeholder)) {
//                    Object fieldValue = field.get(entity);
//                    message = message.replace(placeholder, fieldValue != null ? fieldValue.toString() : "null");
//                }
//
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //get entity config method
    private Entity getConfigEntity(String entityName) {
        for (Entity configEntity : baseConfig.getEntities()) {
            if (configEntity.getName().equals(entityName)) {
                return configEntity;
            }
        }
        return null;
    }

    //get entity by field --- if needed in future
    private Object getEntityByField(Object entity,String entityName, String fieldName, Object fieldValue) throws IllegalAccessException {
        // Get the entity class from the entity name
        Class<?> targetClass = entity.getClass();
        // Get the package name dynamically from the entity's class
        String packageName = targetClass.getPackage().getName();
        Class<?> entityClass;
        try {
            entityClass = Class.forName(packageName+ "." + entityName); // Adjust package name as needed
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Entity class not found for name: " + entityName, e);
        }
        // Get the repository for the entity
        JpaRepository<?, ?> repository = repositoryResolver.getRepositoryForEntity(entityClass);
        if (repository == null) {
            throw new IllegalStateException("No repository found for entity: " + entityName);
        }
        // Find the field in the entity class
        Field targetField = null;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                targetField = field;
                break;
            }
        }
        if (targetField == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in entity: " + entityName);
        }
        List<?> results;
        try {
            // Assume the repository supports a `findBy<FieldName>` method
            String methodName = "findBy" + capitalize(fieldName);
            Method findByFieldMethod = repository.getClass().getMethod(methodName, targetField.getType());
            results = (List<?>) findByFieldMethod.invoke(repository, fieldValue);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException(
                    "Repository does not support finding by field: " + fieldName, e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Error invoking repository method for finding by field: " + fieldName, e);
        }
        // Return the first result or null if no matches
        return results.isEmpty() ? null : results.get(0);
    }


}
