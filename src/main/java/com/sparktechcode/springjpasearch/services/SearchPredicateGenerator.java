package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.exceptions.BadRequestException;
import com.sparktechcode.springjpasearch.exceptions.SparkError;
import jakarta.persistence.criteria.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.Arrays;
import java.util.Objects;

public interface SearchPredicateGenerator<E> {

    @SuppressWarnings("unchecked")
    default  <Y extends Comparable<? super Y>> Predicate plainQueryToPredicate(String field, Root<E> root, CriteriaBuilder builder) {
        var matcher = SearchService.FILTER_PATTERN.matcher(field);
        if (matcher.matches() && matcher.groupCount() > 2) {
            var name = matcher.group(1);
            var operation = matcher.group(2);
            var value = matcher.group(3);
            var path = name.contains(".") ? joinTables(name, root) : getPath(root, name);
            return fieldToPredicate((Expression<Y>) path, operation, value, builder);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    default <Y extends Comparable<? super Y>> Predicate fieldToPredicate(Expression<Y> property, String operation, String value, CriteriaBuilder builder) {
        if (value.equals("null")) {
            return switch (operation) {
                case "::" -> builder.isNull(property);
                case "!:" -> builder.isNotNull(property);
                default -> null;
            };
        }
        return switch (operation) {
            case "::" -> builder.equal(property, prepareValue(property, value));
            case "!:" -> builder.notEqual(property, prepareValue(property, value));
            case ":>" -> builder.greaterThan(property, prepareValue(property, value));
            case ":<" -> builder.lessThan(property, prepareValue(property, value));
            case ">:" -> builder.greaterThanOrEqualTo(property, prepareValue(property, value));
            case "<:" -> builder.lessThanOrEqualTo(property, prepareValue(property, value));
            case ":~" -> builder.like(builder.upper((Expression<String>) property), "%" + value.toUpperCase() + "%");
            case "!~" -> builder.notLike(builder.upper((Expression<String>) property), "%" + value.toUpperCase() + "%");
            case "/:" -> property.in(Arrays.stream(value.split(",")).map(part -> prepareValue(property, part)).toList());
            default -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <Y extends Comparable<? super Y>> Y prepareValue(Expression<Y> property, String value) {
        try {
            var type = property.getJavaType();
            if (String.class.isAssignableFrom(type)) return (Y) value;
            else if (Serializable.class.isAssignableFrom(type)) return (Y) value;
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

    default  <X, Y> Path<Y> joinTables(String field, Root<E> root) {
        var associations = field.split("\\.");
        Join<X, Y> path = null;
        var last = associations[associations.length - 1];
        var associationPath = "";
        for (var association : associations) {
            associationPath = associationPath.isEmpty() ? association : associationPath + "." + association;
            if (!Objects.equals(association, last)) {
                if (path == null) {
                    path = root.join(association);
                } else {
                    path = path.join(association);
                }
            }
        }
        return getPath(path, last);
    }
}
