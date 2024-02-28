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

    default Page<E> search(MultiValueMap<String, String> params, Specification<E> specification) {
        if (this.isSearchAllDataAllowed()) {
            if (requestedAllData(params)) {
                return findAll(toDataSpecification(params, specification), Pageable.unpaged());
            }
        }
        var limit = parseIntParam(params, limitParamName(), 12, 1, 100);
        var page = parseIntParam(params, pageParamName(), 0, 0, Integer.MAX_VALUE);
        var pageable = Pageable.ofSize(limit).withPage(page);
        var dataQuery = toDataSpecification(params, specification);
        var countQuery = toCountSpecification(params, specification);
        var ids = getIds(dataQuery, pageable);
        var data = findAllById(ids);
        data.sort(Comparator.comparing(item -> {
            var index = ids.indexOf((String) item.getId());
            return index == -1 ? Integer.MAX_VALUE : index;
        }));
        return new PageImpl<>(data, pageable, countBy(countQuery));
    }
}
