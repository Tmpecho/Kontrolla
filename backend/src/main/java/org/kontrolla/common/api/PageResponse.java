package org.kontrolla.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Generic paginated API response wrapper.
 *
 * @param items the items returned for the current page
 * @param page the zero-based page index
 * @param size the requested page size
 * @param totalElements the total number of matching elements
 * @param totalPages the total number of result pages
 * @param <T> the item type exposed in the response
 */
public record PageResponse<T>(
    List<T> items, int page, int size, long totalElements, int totalPages) {

  /**
   * Maps a Spring Data page into the API pagination response format.
   *
   * @param source the source page to convert
   * @param mapper the mapper used to transform source items
   * @param <T> the source item type
   * @param <R> the response item type
   * @return a paginated response containing mapped items and page metadata
   */
  public static <T, R> PageResponse<R> from(Page<T> source, Function<T, R> mapper) {
    return new PageResponse<>(
        source.getContent().stream().map(mapper).toList(),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages());
  }
}
