package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface SearchServiceFetcher<I, E extends BaseEntity<I>> extends SearchServiceParser<I, E> {

    EntityManager getEntityManager();

    default List<String> getSearchEntityGraphAttributes() {
        return List.of();
    }

    default List<E> findAllById(List<I> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return createQuery((root, query, builder) -> root.get(getIdFieldName()).in(ids)).getResultList();
    }

    default List<E> findAll() {
        return createQuery(null).getResultList();
    }

    default Page<E> findAll(Specification<E> specification, Pageable pageable) {
        var typedQuery = createQuery(specification);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        var data = typedQuery.getResultList();
        return new PageImpl<>(data, pageable, data.size());
    }

    default Optional<E> findOne(Specification<E> specification) {
        var typedQuery = createQuery(specification, false);
        var data = typedQuery.getSingleResult();
        return Optional.ofNullable(data);
    }

    @SuppressWarnings("unchecked")
    default List<I> getIds(Specification<E> specification, Pageable pageable) {
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
                .map(item -> (I) item)
                .toList();
    }

    default Long countBy(Specification<E> specification) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        var root = query.from(getEntityClass());
        query.select(builder.count(root.get(getIdFieldName())));
        query.where(specification.toPredicate(root, query, builder));
        return getEntityManager()
                .createQuery(query)
                .getSingleResult();
    }


    private TypedQuery<E> createQuery(Specification<E> specification) {
        return createQuery(specification, true);
    }

    private TypedQuery<E> createQuery(Specification<E> specification, boolean fetchAttributes) {
        var criteriaBuilder = getEntityManager().getCriteriaBuilder();
        var criteriaQuery = criteriaBuilder.createQuery(getEntityClass());
        var root = criteriaQuery.from(getEntityClass());
        criteriaQuery.select(root);
        if (specification != null) {
            criteriaQuery.where(specification.toPredicate(root, criteriaQuery, criteriaBuilder));
        }
        var query = getEntityManager().createQuery(criteriaQuery);
        if (fetchAttributes && !getSearchEntityGraphAttributes().isEmpty()) {
            var entityGraph = getEntityManager().createEntityGraph(getEntityClass());
            getSearchEntityGraphAttributes().forEach(entityGraph::addAttributeNodes);
            entityGraph.addAttributeNodes(getSearchEntityGraphAttributes().toArray(String[]::new));
            query.setHint("javax.persistence.fetchgraph", entityGraph);
        }
        return query;
    }
}
