package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.QueryElement;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors( fluent = true )
@EqualsAndHashCode( callSuper = true )
public class AndFilter extends FilterList {

    protected AndFilter( final List<QueryElement> filters ) {
        super( filters );
    }

    @Override
    public Predicate toPredicate( final QueryBuilder cqb ) {
        if ( !filters().isEmpty() ) {
            if ( filters().size() > 1 ) {
                return cqb.cb().and( filters().stream().map( f -> f.toPredicate( cqb ) ).toArray( Predicate[]::new ) );
            }
            return filters().get( 0 ).toPredicate( cqb );
        }
        return cqb.cb().and();
    }

    public static AndFilter of( final QueryElement... filters ) {
        return new AndFilter( List.of( filters ) );
    }

    public static AndFilter of( final List<QueryElement> filters ) {
        return new AndFilter( filters );
    }
}
