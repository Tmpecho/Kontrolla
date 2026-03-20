package org.kontrolla.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static <T, R> PageResponse<R> from(Page<T> source, Function<T, R> mapper) {
		return new PageResponse<>(
				source.getContent().stream().map(mapper).toList(),
				source.getNumber(),
				source.getSize(),
				source.getTotalElements(),
				source.getTotalPages()
		);
	}
}
