package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import java.util.List;

public interface QueryElement {

    /**
     * Creates the predicate for this query element.
     *
     * @param cqb criteria query builder
     * @return the predicate for this query element
     */
    Predicate toPredicate( QueryBuilder cqb );

    /**
     * Returns the attributes used in this query element.
     *
     * @return list of attributes
     */
    List<Attribute> attributes();

    /**
     * @return true if this query element is empty (list without any query elements).
     */
    boolean isEmpty();
}
