package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        var data = typedQuery.getResultList();
        return new PageImpl<>(data, pageable, data.size());
    }

    default Optional<E> findOne(Specification<E> specification) {
        try {
            var typedQuery = createQuery(specification, false);
            var data = typedQuery.getSingleResult();
            return Optional.ofNullable(data);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    default List<I> getIds(Specification<E> specification, Pageable pageable) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createTupleQuery();
        var root = query.from(getEntityClass());
        query.where(specification.toPredicate(root, query, builder));
        var selection = new ArrayList<Expression<?>>();
        selection.add(root.get(getIdFieldName()));
        selection.addAll(query.getOrderList().stream().map(Order::getExpression).toList());
        query.multiselect((List<Selection<?>>) ((List<?>) selection));
        query.groupBy(selection);
        if (pageable.isUnpaged()) {
            return getEntityManager()
                    .createQuery(query)
                    .getResultList()
                    .stream()
                    .map(item -> (I) item.toArray()[0])
                    .toList();
        } else {
            return getEntityManager()
                    .createQuery(query)
                    .setFirstResult((int) pageable.getOffset())
                    .setMaxResults(pageable.getPageSize())
                    .getResultList()
                    .stream()
                    .map(item -> (I) item.toArray()[0])
                    .toList();
        }
    }

    default Long countBy(Specification<E> specification) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        var root = query.from(getEntityClass());
        query.select(builder.countDistinct(root.get(getIdFieldName())));
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
