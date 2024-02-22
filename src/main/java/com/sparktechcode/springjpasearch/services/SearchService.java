package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import com.sparktechcode.springjpasearch.exceptions.InternalServerException;
import com.sparktechcode.springjpasearch.repositories.SparkJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.sparktechcode.springjpasearch.exceptions.SparkError.UNABLE_TO_FIND_ENTITY_CLASS;
import static java.util.stream.Collectors.toList;

public interface SearchService<I, E extends BaseEntity<I>> extends SearchPredicateGenerator<E> {

    SparkJpaRepository<I, E> getRepository();
    EntityManager getEntityManager();

    String FILTER_NAME = "filter";
    String ORDER_NAME = "order";
    String PAGE_NAME = "page";
    String LIMIT_NAME = "limit";
    String ALL_DATA_NAME = "allData";
    Pattern FILTER_PATTERN = Pattern.compile("([\\w.].*)([:</>~!]{2})(.*)", Pattern.CASE_INSENSITIVE);
    Pattern ORDER_PATTERN = Pattern.compile("([\\w.]*):([a,d])", Pattern.CASE_INSENSITIVE);

    default boolean isSearchAllDataAllowed() {
        return false;
    }

    default String getIdFieldName() {
        return "id";
    }

    default Page<E> search(MultiValueMap<String, String> params) {
        return search(params, null);
    }

    default Page<E> search(MultiValueMap<String, String> params, Specification<E> specification) {
        if (this.isSearchAllDataAllowed()) {
            if (getAllData(params)) {
                return getRepository().findAll(toDataSpecification(params, specification), Pageable.unpaged());
            }
        }
        var limit = getInt(params, LIMIT_NAME, 12, 1, 100);
        var page = getInt(params, PAGE_NAME, 0, 0, Integer.MAX_VALUE);
        var pageable = Pageable.ofSize(limit).withPage(page);
        var dataQuery = toDataSpecification(params, specification);
        var countQuery = toCountSpecification(params, specification);
        var ids = getIds(dataQuery, pageable);
        var data = getRepository().findAllById(ids);
        return new PageImpl<>(data, pageable, getTotalCount(countQuery));
    }

    @SuppressWarnings("unchecked")
    default Class<E> getEntityClass() {
        try {
            return (Class<E>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
        } catch (Exception e) {
            throw new InternalServerException(UNABLE_TO_FIND_ENTITY_CLASS, "Unable to find entity class, you can provide entity class if you override getEntityClass() method.", e);
        }
    }

    private List<String> getIds(Specification<E> specification, Pageable pageable) {
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

    private Long getTotalCount(Specification<E> specification) {
        var builder = getEntityManager().getCriteriaBuilder();
        var query = builder.createQuery(Long.class);
        var root = query.from(getEntityClass());
        query.select(builder.count(root.get(getIdFieldName())));
        query.where(specification.toPredicate(root, query, builder));
        return getEntityManager()
                .createQuery(query)
                .getSingleResult();
    }

    private int getInt(MultiValueMap<String, String> params, String key, int defaultValue, int min, int max) {
        var list = params.get(key);
        if (list != null && !list.isEmpty()) {
            var value =  Integer.parseInt(list.get(0));
            if (value < min) {
                return defaultValue;
            }
            if (value > max) {
                return defaultValue;
            }
            return value;
        }
        return defaultValue;
    }

    private boolean getAllData(MultiValueMap<String, String> params) {
        var list = params.get(ALL_DATA_NAME);
        if (list != null && !list.isEmpty()) {
            return Boolean.parseBoolean(list.get(0));
        }
        return false;
    }

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
        if (params.containsKey(ORDER_NAME)) {
            var list = params.get(ORDER_NAME);
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
        if (params.containsKey(FILTER_NAME)) {
            params.get(FILTER_NAME).forEach(field -> predicates.add(queryToPredicate(field, root, builder)));
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
