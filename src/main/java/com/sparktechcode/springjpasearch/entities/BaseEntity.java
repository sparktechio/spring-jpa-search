package com.sparktechcode.springjpasearch.entities;

public interface BaseEntity<T> extends IdHolder<T> {

    T getId();
}
