package com.example.demo.listener;

import javax.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.*;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.persistence.metamodel.EntityType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EntityEventListenerRegistrar {
    private final EntityManagerFactory entityManagerFactory;

    @Value("${entities.with.listeners}")
    private String entitiesWithListeners;

    // Keep track of registered listener classes
    private final Set<String> registeredListeners = new HashSet<>();

    public EntityEventListenerRegistrar(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    public void registerListeners() {
        if (entitiesWithListeners == null || entitiesWithListeners.isEmpty()) {
            throw new RuntimeException("No entities specified for event listeners in 'entities.with.listeners' property.");
        }

        // Parse the entities specified in the properties file
        List<String> allowedEntities = Arrays.asList(entitiesWithListeners.split(","));
        System.out.println("Entities with listeners: " + entitiesWithListeners);

        // Unwrap the SessionFactoryImpl
        SessionFactoryImpl sessionFactory;
        try {
            sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
            if (sessionFactory == null) {
                throw new RuntimeException("Failed to unwrap SessionFactoryImpl from EntityManagerFactory");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error unwrapping SessionFactoryImpl", e);
        }

        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        for (String entityName : allowedEntities) {
            try {
                Class<?> entityClass = getEntityClass(entityName.trim());

                // Register the listener if it is not already registered
//                registerListenerIfNotExists(registry.getEventListenerGroup(EventType.PERSIST),(PersistEventListener) listener);
//                registerListenerIfNotExists(registry.getEventListenerGroup(EventType.UPDATE),(SaveOrUpdateEventListener) listener);
//                registerListenerIfNotExists(registry.getEventListenerGroup(EventType.DELETE),(DeleteEventListener) listener);
                registerListenerIfNotExists(registry.getEventListenerGroup(EventType.POST_INSERT),(PostInsertEventListener) new EntityListener(),EventType.POST_INSERT);
                registerListenerIfNotExists(registry.getEventListenerGroup(EventType.POST_UPDATE),(PostUpdateEventListener) new EntityListener(),EventType.POST_UPDATE);


                System.out.println("Custom entity listener registered for: " + entityName.trim());

            } catch (ClassNotFoundException e) {
                System.err.println("Entity class not found: " + entityName.trim());
            }
        }
    }

//    private <T> void registerListenerIfNotExists(EventListenerGroup<T> listenerGroup, T listener) {
//        Class<?> listenerClass = listener.getClass();
//        // Check if the listener class has already been registered
//        if (!registeredListeners.contains(listenerClass)) {
//            listenerGroup.appendListener(listener);
//            registeredListeners.add(listenerClass);
//        } else {
//            System.out.println("Listener already registered: " + listenerClass.getSimpleName());
//        }
//    }
private <T> void registerListenerIfNotExists(EventListenerGroup<T> listenerGroup, T listener, EventType<?> eventType) {
    String listenerKey = listener.getClass().getName() + ":" + eventType.toString();
    // Check if the listener for this event type has already been registered
    if (!registeredListeners.contains(listenerKey)) {
        listenerGroup.appendListener(listener);
        registeredListeners.add(listenerKey);
        System.out.println("Registered listener: " + listenerKey);
    } else {
        System.out.println("Listener already registered for event type: " + listenerKey);
    }
}
    public Class<?> getEntityClass(String entityName) throws ClassNotFoundException {
        for (EntityType<?> entityType : entityManagerFactory.getMetamodel().getEntities()) {
            if (entityType.getName().equalsIgnoreCase(entityName.trim())) {
                return entityType.getJavaType();
            }
        }
        throw new ClassNotFoundException("Entity not found: " + entityName);
    }
}
