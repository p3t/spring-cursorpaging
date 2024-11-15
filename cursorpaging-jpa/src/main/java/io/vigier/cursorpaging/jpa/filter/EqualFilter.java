package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.ToString;


@ToString(callSuper = true)
public class EqualFilter extends Filter {

    public EqualFilter( final Attribute attribute, final List<? extends Comparable<?>> values ) {
        super( attribute, values );
    }

    @Override
    protected Predicate toFilterPredicate( final QueryBuilder qb, final List<? extends Comparable<?>> cleanValues ) {
        if ( cleanValues.size() > 1 ) {
            return qb.isIn( attribute(), cleanValues );
        }
        return qb.equalTo( attribute(), cleanValues.get( 0 ) );
    }
}
