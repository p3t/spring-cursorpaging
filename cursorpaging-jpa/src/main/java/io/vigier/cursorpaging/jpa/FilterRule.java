package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;


/**
 * A custom rule to filter the query.
 */
public interface FilterRule {

    default void apply( final QueryBuilder cqb ) {
        cqb.addWhere( getPredicate( cqb ) );
    }

    Predicate getPredicate( final QueryBuilder cqb );
}
