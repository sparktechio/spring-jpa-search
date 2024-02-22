package com.sparktechcode.springjpasearch.repositories;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SparkJpaRepository<I, E extends BaseEntity<I>> extends JpaRepository<E, String>, JpaSpecificationExecutor<E> {
}
