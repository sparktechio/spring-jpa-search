package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.exceptions.BadRequestException;
import com.sparktechcode.springjpasearch.exceptions.SparkError;
import jakarta.persistence.criteria.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;

import static com.sparktechcode.springjpasearch.exceptions.SparkError.UNSUPPORTED_OPERATION;

public interface SearchServicePredicateGenerator<E> {

    default  Predicate plainQueryToPredicate(String field, Root<E> root, CriteriaBuilder builder) {
        var matcher = SearchService.FILTER_PATTERN.matcher(field);
        if (matcher.matches() && matcher.groupCount() > 2) {
            var name = matcher.group(1);
            var operation = matcher.group(2);
            var value = matcher.group(3);
            return fieldToPredicate(name, operation, value, root, builder);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    default <Y extends Comparable<? super Y>> Predicate fieldToPredicate(String name, String operation, String value, Root<E> root, CriteriaBuilder builder) {
        var operations = name.split("`");
        var fieldPath = operations[0];
        var function = operations.length > 1 ? operations[1] : null;
        var path = fieldPath.contains(".") ? joinTables(fieldPath, root) : getPath(root, fieldPath);
        return fieldToPredicate((Expression<Y>) evaluateFunction(path, function, builder), operation, value, function, builder);
    }

    default Expression<?> evaluateFunction(Expression<?> path, String function, CriteriaBuilder builder) {
        if (function != null) {
            return builder.function("date_part", Integer.class, builder.literal(function), path);
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    default <Y extends Comparable<? super Y>> Predicate fieldToPredicate(Expression<Y> property, String operation, String value, String function, CriteriaBuilder builder) {
        return switch (operation) {
            case "::" -> builder.equal(property, prepareValue(property, value, function));
            case "!:" -> builder.notEqual(property, prepareValue(property, value, function));
            case ":>" -> builder.greaterThan(property, prepareValue(property, value, function));
            case ":<" -> builder.lessThan(property, prepareValue(property, value, function));
            case ">:" -> builder.greaterThanOrEqualTo(property, prepareValue(property, value, function));
            case "<:" -> builder.lessThanOrEqualTo(property, prepareValue(property, value, function));
            case ":~" -> builder.like(builder.upper((Expression<String>) property), "%" + value.toUpperCase() + "%");
            case "!~" -> builder.notLike(builder.upper((Expression<String>) property), "%" + value.toUpperCase() + "%");
            case "/:" -> property.in(Arrays.stream(value.split(",")).map(part -> prepareValue(property, part, function)).toList());
            case "!/" -> builder.not(property.in(Arrays.stream(value.split(",")).map(part -> prepareValue(property, part, function)).toList()));
            case "!!" -> builder.isNotNull(property);
            case "<>" -> builder.isNull(property);
            default -> throw new BadRequestException(UNSUPPORTED_OPERATION, "Unsupported operation: " + operation);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <Y extends Comparable<? super Y>> Y prepareValue(Expression<Y> property, String value, String function) {
        try {
            if (function != null) {
                return (Y) Integer.valueOf(value);
            } else {
                var type = property.getJavaType();
                if (String.class.isAssignableFrom(type)) return (Y) value;
                else if (Boolean.class.isAssignableFrom(type)) return (Y) Boolean.valueOf(value);
                else if (type.isEnum()) return (Y) Enum.valueOf((Class) type, value);
                else if (Double.class.isAssignableFrom(type)) return (Y) Double.valueOf(value);
                else if (Float.class.isAssignableFrom(type)) return (Y) Float.valueOf(value);
                else if (BigDecimal.class.isAssignableFrom(type)) return (Y) BigDecimal.valueOf(Double.parseDouble(value));
                else if (Long.class.isAssignableFrom(type)) return (Y) Long.valueOf(value);
                else if (Integer.class.isAssignableFrom(type)) return (Y) Integer.valueOf(value);
                else if (LocalDate.class.isAssignableFrom(type)) return (Y) LocalDate.parse(value);
                else if (LocalTime.class.isAssignableFrom(type)) return (Y) LocalTime.parse(value);
                else if (LocalDateTime.class.isAssignableFrom(type)) return (Y) LocalDateTime.parse(value);
                else if (OffsetDateTime.class.isAssignableFrom(type)) return (Y) OffsetDateTime.parse(value);
                else if (Instant.class.isAssignableFrom(type)) return (Y) Instant.parse(value);
                else if (Serializable.class.isAssignableFrom(type)) return (Y) value;
            }
        } catch (Exception e) {
            throw new BadRequestException(SparkError.UNEXPECTED_QUERY_PARAMETER, e.getMessage(), e);
        }
        throw new BadRequestException(SparkError.UNEXPECTED_QUERY_PARAMETER, "Expected field pattern: " + SearchService.FILTER_PATTERN);
    }

    default <X> Path<X> getPath(Path<X> path, String field) {
        if (path == null) {
            return null;
        }
        return path.get(field);
    }

    default  <Y> Path<Y> joinTables(String field, Root<E> root) {
        var associations = field.split("\\.");
        Path<Y> path = null;
        var last = associations[associations.length - 1];
        var associationPath = "";
        for (var association : associations) {
            associationPath = associationPath.isEmpty() ? association : associationPath + "." + association;
            if (!Objects.equals(association, last)) {
                if (path == null) {
                    path = root.get(association);
                } else {
                    path = path.get(association);
                }
            }
        }
        return getPath(path, last);
    }
}
