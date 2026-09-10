package org.civicops.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** Stable, framework-independent external representation of a paged collection. */
@Schema(
    name = "PageResponse",
    description = "Stable metadata and content for a paginated collection")
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public static <T> PageResponse<T> from(Page<T> source) {
    return new PageResponse<>(
        List.copyOf(source.getContent()),
        source.getNumber(),
        source.getSize(),
        source.getTotalElements(),
        source.getTotalPages(),
        source.isFirst(),
        source.isLast());
  }
}
