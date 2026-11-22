package com.cs308.store.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * The JSON shape of every paged list. Spring's own Page object is not a stable JSON format,
 * so the API always answers with this record instead.
 *
 * @param page zero-based, like Spring's page numbers
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
