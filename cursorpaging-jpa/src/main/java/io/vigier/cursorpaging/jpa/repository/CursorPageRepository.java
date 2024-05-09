package io.vigier.cursorpaging.jpa.repository;

import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * A repository that can load pages of data by using a cursor based strategy
 *
 * @param <T> the type of the data.
 */
@NoRepositoryBean
public interface CursorPageRepository<T> {

    /**
     * Load a page of data.
     *
     * @param request the page request containing start position, order, and fetch size.
     * @return the page with the content data and the potential next fetch position.
     */
    Page<T> loadPage( final PageRequest<T> request );

    /**
     * Count the number of records on all pages, considering the defined filters.
     *
     * @param request the page request containing filter definitions
     * @return the number of records.
     */
    long count( PageRequest<T> request );

}
