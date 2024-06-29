package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;


/**
 * A custom rule to filter the query.
 */
public interface FilterRule {

    default void applyQuery( final QueryBuilder cqb ) {
        cqb.addWhere( getPredicate( cqb ) );
    }

    default void applyCount( final QueryBuilder cqb ) {
        cqb.addWhere( getCountPredicate( cqb ) );
    }

    /**
     * Sometimes the query-predicate must be different than the count-predicate due to the criteria API
     *
     * @param cqb Query, Builder and Root
     * @return the predicate which should be applied to the where-clause
     */
    default Predicate getCountPredicate( final QueryBuilder cqb ) {
        return getPredicate( cqb );
    }

    /**
     * Get the predicate for the query.
     *
     * @param cqb Query, Builder and Root
     * @return the predicate which should be applied to the where-clause
     */
    Predicate getPredicate( final QueryBuilder cqb );
}
