package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.QueryElement;
import jakarta.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors( fluent = true )
@EqualsAndHashCode( callSuper = true )
@ToString(callSuper = true)
public class OrFilter extends FilterList {

    private OrFilter( final List<QueryElement> filters ) {
        super( filters );
    }

    @Override
    public Predicate toPredicate( final QueryBuilder cqb ) {
        if ( !isEmpty() ) {
            if ( size() > 1 ) {
                return cqb.cb().or( filters().stream().map( f -> f.toPredicate( cqb ) ).toArray( Predicate[]::new ) );
            }
            return filters().get( 0 ).toPredicate( cqb );
        }
        return cqb.cb().and();
    }

    public static OrFilter of( final QueryElement... filters ) {
        return new OrFilter( List.of( filters ) );
    }

    public static OrFilter of( final Collection<QueryElement> filters ) {
        return new OrFilter( List.copyOf( filters ) );
    }
}
