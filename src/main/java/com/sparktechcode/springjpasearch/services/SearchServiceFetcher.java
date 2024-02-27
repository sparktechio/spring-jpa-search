package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public interface SearchServiceFetcher<I, E extends BaseEntity<I>> extends SearchServiceParser<I, E> {

    EntityManager getEntityManager();

    default List<E> findAllById(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        var criteriaBuilder = getEntityManager().getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(getEntityClass());
        var root = criteriaQuery.from(getEntityClass());
        criteriaQuery.select(root);
        criteriaQuery.where(root.get(getIdFieldName()).in(ids));
        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }

    default Page<E> findAll(Specification<E> spec, Pageable pageable) {
        var criteriaBuilder = getEntityManager().getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(getEntityClass());
        var root = criteriaQuery.from(getEntityClass());
        criteriaQuery.select(root);
        criteriaQuery.where(spec.toPredicate(root, criteriaQuery, criteriaBuilder));
        var typedQuery = getEntityManager().createQuery(criteriaQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        var data = typedQuery.getResultList();
        return new PageImpl<>(data, pageable, data.size());
    }

    default List<String> getIds(Specification<E> specification, Pageable pageable) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createQuery(Serializable.class);
        var root = query.from(getEntityClass());
        query.select(root.get(getIdFieldName()));
        query.where(specification.toPredicate(root, query, builder));
        query.groupBy(root.get(getIdFieldName()));
        return getEntityManager()
                .createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(Object::toString)
                .toList();
    }

    default Long getTotalCount(Specification<E> specification) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        var root = query.from(getEntityClass());
        query.select(builder.count(root.get(getIdFieldName())));
        query.where(specification.toPredicate(root, query, builder));
        return getEntityManager()
                .createQuery(query)
                .getSingleResult();
    }
}
