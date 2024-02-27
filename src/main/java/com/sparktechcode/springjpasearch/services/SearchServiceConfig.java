package com.sparktechcode.springjpasearch.services;

import java.util.regex.Pattern;

public interface SearchServiceConfig {

    Pattern FILTER_PATTERN = Pattern.compile("([\\w.].*)([:</>~!]{2})(.*)", Pattern.CASE_INSENSITIVE);
    Pattern ORDER_PATTERN = Pattern.compile("([\\w.]*):([a,d])", Pattern.CASE_INSENSITIVE);

    default String filterParamName() {
        return "filter";
    };

    default String orderParamName() {
        return "order";
    };

    default String pageParamName() {
        return "page";
    };

    default String limitParamName() {
        return "limit";
    };

    default String allDataParamName() {
        return "allData";
    };

    default boolean isSearchAllDataAllowed() {
        return false;
    }

    default String getIdFieldName() {
        return "id";
    }
}
