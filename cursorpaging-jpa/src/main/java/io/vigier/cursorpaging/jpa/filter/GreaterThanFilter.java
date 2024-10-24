package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

public class GreaterThanFilter extends Filter {

    public GreaterThanFilter( final Attribute attribute, final List<? extends Comparable<?>> values ) {
        super( attribute, values );
    }

    @Override
    protected Predicate toFilterPredicate( final QueryBuilder qb, final List<? extends Comparable<?>> cleanValues ) {
        final List<Predicate> predicates = cleanValues.stream().map( v -> qb.greaterThan( attribute(), v ) ).toList();
        if ( predicates.size() > 1 ) {
            return qb.cb().and( predicates.toArray( Predicate[]::new ) );
        }
        return predicates.get( 0 );
    }

}
