package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;


/**
 * A custom rule to filter the query.
 */
public interface FilterRule extends QueryElement {

    /**
     * Sometimes the query-predicate must be different than the count-predicate due to the criteria API
     *
     * @param cqb Query, Builder and Root
     * @return the predicate which should be applied to the where-clause
     */
    default Predicate toCountPredicate( final QueryBuilder cqb ) {
        return toPredicate( cqb );
    }

    @Override
    default List<Attribute> attributes() {
        return List.of();
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    default String name() {
        return this.getClass().getName();
    }

    default Map<String, List<String>> parameters() {
        return Map.of();
    }
}
