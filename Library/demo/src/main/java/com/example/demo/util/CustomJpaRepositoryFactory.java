package com.example.demo.util;
import javax.persistence.EntityManager;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

public class CustomJpaRepositoryFactory extends JpaRepositoryFactory {
    public CustomJpaRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
    }

    public RepositoryMetadata getMetadataFor(Class<?> repositoryInterface) {
        return super.getRepositoryMetadata(repositoryInterface);
    }
}
