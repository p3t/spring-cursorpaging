package io.vigier.cursor.repository;

import io.vigier.cursor.Page;
import io.vigier.cursor.PageRequest;
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

}
