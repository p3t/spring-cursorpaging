package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

public class LikeFilter extends Filter {

    public LikeFilter( final Attribute attribute, final List<? extends Comparable<?>> values ) {
        super( attribute, values );
    }

    @Override
    protected Predicate toFilterPredicate( final QueryBuilder qb, final List<? extends Comparable<?>> cleanValues ) {
        final var predicates = cleanValues.stream()
                .map( Object::toString )
                .map( v -> qb.isLike( attribute(), v ) )
                .toArray( Predicate[]::new );
        if ( predicates.length > 1 ) {
            return qb.cb().or( predicates );
        }
        return predicates[0];
    }

}
