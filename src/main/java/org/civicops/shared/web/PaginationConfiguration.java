package org.civicops.shared.web;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

@Configuration
// PageResponseAdvice owns the external contract; services may continue to use Spring Data Page
// internally.
public class PaginationConfiguration {
  static {
    SpringDocUtils.getConfig().replaceWithClass(Page.class, PageResponse.class);
  }
}
