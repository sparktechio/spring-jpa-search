package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import com.sparktechcode.springjpasearch.exceptions.InternalServerException;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.ParameterizedType;

import static com.sparktechcode.springjpasearch.exceptions.SparkError.UNABLE_TO_FIND_ENTITY_CLASS;

public interface SearchServiceParser<I, E extends BaseEntity<I>> extends SearchServiceConfig {

    default boolean requestedAllData(MultiValueMap<String, String> params) {
        var list = params.get(allDataParamName());
        if (list != null && !list.isEmpty()) {
            return Boolean.parseBoolean(list.get(0));
        }
        return false;
    }

    default int parseIntParam(MultiValueMap<String, String> params, String key, int defaultValue, int min, int max) {
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

    @SuppressWarnings("unchecked")
    default Class<E> getEntityClass() {
        try {
            return (Class<E>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
        } catch (Exception e) {
            throw new InternalServerException(UNABLE_TO_FIND_ENTITY_CLASS, "Unable to find entity class, you can provide entity class if you override getEntityClass() method.", e);
        }
    }
}
