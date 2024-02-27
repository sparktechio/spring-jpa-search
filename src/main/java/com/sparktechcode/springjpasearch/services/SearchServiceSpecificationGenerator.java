package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public interface SearchServiceSpecificationGenerator<I, E extends BaseEntity<I>> extends SearchServicePredicateGenerator<E>, SearchServiceConfig {


    default Specification<E> toDataSpecification(MultiValueMap<String, String> params, Specification<E> specification) {
        return (root, query, builder) -> {
            var filter = getWherePredicates(params, root, builder);
            if (specification != null) {
                filter.add(specification.toPredicate(root, query, builder));
            }
            query.orderBy(getOrderPredicates(params, root, builder));
            return builder.and(filter.toArray(new Predicate[0]));
        };
    }

    default Specification<E> toCountSpecification(MultiValueMap<String, String> params, Specification<E> specification) {
        return (root, query, builder) -> {
            var filter = getWherePredicates(params, root, builder);
            if (specification != null) {
                filter.add(specification.toPredicate(root, query, builder));
            }
            return builder.and(filter.toArray(new Predicate[0]));
        };
    }

    private List<Order> getOrderPredicates(MultiValueMap<String, String> params, Root<E> root, CriteriaBuilder builder) {
        if (params.containsKey(orderParamName())) {
            var list = params.get(orderParamName());
            return list.stream()
                    .map(order -> queryToOrder(order, root, builder))
                    .filter(Objects::nonNull)
                    .collect(toList());
        } else {
            return new ArrayList<>();
        }
    }

    private Order queryToOrder(String order, Root<E> root, CriteriaBuilder builder) {
        if (order == null) {
            return null;
        } else {
            var matcher = ORDER_PATTERN.matcher(order);
            if (matcher.matches() && matcher.groupCount() > 1) {
                var field = matcher.group(1);
                var direction = matcher.group(2);
                var path = field.contains(".") ? joinTables(field, root) : getPath(root, field);
                return direction.equals("a") ? builder.asc(path) : builder.desc(path);
            } else {
                return null;
            }
        }
    }

    default List<Predicate> getWherePredicates(MultiValueMap<String, String> params, Root<E> root, CriteriaBuilder builder) {
        var predicates = new ArrayList<Predicate>();
        if (params.containsKey(filterParamName())) {
            params.get(filterParamName()).forEach(field -> predicates.add(queryToPredicate(field, root, builder)));
        }
        return predicates;
    }

    private Predicate queryToPredicate(String field, Root<E> root, CriteriaBuilder builder) {
        if (field != null) {
            if (field.contains("|")) {
                return builder.or(Arrays.stream(field.split("\\|"))
                        .map(item -> plainQueryToPredicate(item, root, builder))
                        .toArray(Predicate[]::new));
            } else {
                return plainQueryToPredicate(field, root, builder);
            }
        }
        return null;
    }
}
