package com.sparktechcode.springjpasearch.services;

import com.sparktechcode.springjpasearch.entities.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.MultiValueMap;

import java.util.Comparator;

public interface SearchService<I, E extends BaseEntity<I>> extends
        SearchServiceSpecificationGenerator<I, E>,
        SearchServiceFetcher<I, E> {

    default Page<E> search(MultiValueMap<String, String> params) {
        return search(params, null);
    }

    default Long count(MultiValueMap<String, String> params) {
        return count(params, null);
    }

    default Page<I> searchIdentifiers(MultiValueMap<String, String> params) {
        return searchIdentifiers(params, null);
    }

    default Page<E> search(MultiValueMap<String, String> params, Specification<E> specification) {
        if (fetchAllData(params)) {
            return findAll(toDataSpecification(params, specification), Pageable.unpaged());
        }
        var ids = searchIdentifiers(params, specification);
        var data = findAllById(ids.getContent());
        data.sort(Comparator.comparing(item -> {
            var index = ids.getContent().indexOf(item.getId());
            return index == -1 ? Integer.MAX_VALUE : index;
        }));
        return new PageImpl<>(data, ids.getPageable(), ids.getTotalElements());
    }

    default Long count(MultiValueMap<String, String> params, Specification<E> specification) {
        return countBy(toCountSpecification(params, specification));
    }

    default Page<I> searchIdentifiers(MultiValueMap<String, String> params, Specification<E> specification) {
        var pageable = getPageable(params);
        var dataQuery = toDataSpecification(params, specification);
        var countQuery = toCountSpecification(params, specification);
        var ids = getIds(dataQuery, pageable);
        return new PageImpl<>(ids, pageable, countBy(countQuery));
    }

    private boolean fetchAllData(MultiValueMap<String, String> params) {
        if (this.isSearchAllDataAllowed()) {
            return requestedAllData(params);
        }
        return false;
    }

    private Pageable getPageable(MultiValueMap<String, String> params) {
        if (fetchAllData(params)) {
            return Pageable.unpaged();
        } else {
            var limit = parseIntParam(params, limitParamName(), 12, 1, 100);
            var page = parseIntParam(params, pageParamName(), 0, 0, Integer.MAX_VALUE);
            return Pageable.ofSize(limit).withPage(page);
        }
    }
}
