package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FilterType implements FilterOperation {

    EQUAL_TO( FilterType::equalTo ),
    GREATER_THAN( FilterType::greaterThan ),
    GREATER_THAN_OR_EQUAL_TO( FilterType::greaterThanOrEqualTo ),
    LESS_THAN( FilterType::lessThan ),
    LESS_THAN_OR_EQUAL_TO( FilterType::lessThanOrEqualTo ),
    LIKE( FilterType::like );

    private final FilterOperation operation;

    private static Predicate equalTo( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        if ( values.size() > 1 ) {
            return qb.isIn( attribute, values );
        }
        return qb.equalTo( attribute, values.getFirst() );
    }

    private static Predicate greaterThan( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final List<Predicate> predicates = values.stream().map( v -> qb.greaterThan( attribute, v ) ).toList();
        if ( predicates.size() > 1 ) {
            return qb.cb().and( predicates.toArray( Predicate[]::new ) );
        }
        return predicates.getFirst();
    }

    private static Predicate lessThan( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final List<Predicate> predicates = values.stream().map( v -> qb.lessThan( attribute, v ) ).toList();
        if ( predicates.size() > 1 ) {
            return qb.cb().and( predicates.toArray( Predicate[]::new ) );
        }
        return predicates.getFirst();
    }

    private static Predicate greaterThanOrEqualTo( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final List<Predicate> predicates = values.stream().map( v -> qb.greaterThanOrEqualTo( attribute, v ) ).toList();
        if ( predicates.size() > 1 ) {
            return qb.cb().and( predicates.toArray( Predicate[]::new ) );
        }
        return predicates.getFirst();
    }

    private static Predicate lessThanOrEqualTo( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final List<Predicate> predicates = values.stream().map( v -> qb.lessThanOrEqualTo( attribute, v ) ).toList();
        if ( predicates.size() > 1 ) {
            return qb.cb().and( predicates.toArray( Predicate[]::new ) );
        }
        return predicates.getFirst();
    }

    private static Predicate like( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final var predicates = values.stream()
                .map( Object::toString )
                .map( v -> qb.isLike( attribute, v ) )
                .toArray( Predicate[]::new );
        if ( predicates.length > 1 ) {
            return qb.cb().or( predicates );
        }
        return predicates[0];
    }

    @Override
    public Predicate apply( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        return operation.apply( qb, attribute, values );
    }
}
