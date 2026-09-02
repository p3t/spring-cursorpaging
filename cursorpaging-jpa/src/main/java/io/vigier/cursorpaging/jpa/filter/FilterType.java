package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FilterType implements FilterOperation {

    EQUAL_TO( FilterType::equalTo ),
    /**
     * Negation of {@link #EQUAL_TO}: with a single value a {@code <> value}, with multiple values a
     * {@code not in (values)} condition (mirroring the {@code EQUAL_TO}/{@code in} handling).
     */
    NOT_EQUAL_TO( FilterType::notEqualTo ),
    GREATER_THAN( FilterType::greaterThan ),
    GREATER_THAN_OR_EQUAL_TO( FilterType::greaterThanOrEqualTo ),
    LESS_THAN( FilterType::lessThan ),
    LESS_THAN_OR_EQUAL_TO( FilterType::lessThanOrEqualTo ),
    LIKE( FilterType::like ),
    /**
     * Negation of {@link #LIKE}: matches when the attribute is like none of the given patterns.
     */
    NOT_LIKE( FilterType::notLike ),
    ALWAYS( FilterType::always ) {
        @Override
        public boolean isEmpty( final Filter filter ) {
            return false; // does not need values
        }
    };

    private final FilterOperation operation;

    public boolean isEmpty( final Filter filter ) {
        return filter.values().isEmpty();
    }

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

    private static Predicate notEqualTo( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        return qb.cb().not( equalTo( qb, attribute, values ) );
    }

    private static Predicate notLike( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        return qb.cb().not( like( qb, attribute, values ) );
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

    private static Predicate always( final QueryBuilder queryBuilder, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        final var value = !values.isEmpty() && Boolean.TRUE.equals( values.getFirst() );
        return queryBuilder.always( value );
    }


    @Override
    public Predicate apply( final QueryBuilder qb, final Attribute attribute,
            final List<? extends Comparable<?>> values ) {
        return operation.apply( qb, attribute, values );
    }
}
