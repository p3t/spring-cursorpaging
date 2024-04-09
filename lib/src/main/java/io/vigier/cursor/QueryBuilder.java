package io.vigier.cursor;

import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;

public interface QueryBuilder<E> {

    <V extends Comparable<? super V>> void lessThan( SingularAttribute<E, V> attribute,
            V value );

    <V extends Comparable<? super V>> void greaterThan( SingularAttribute<E, V> attribute,
            V value );

    <V extends Comparable<? super V>> void orderBy( SingularAttribute<E, V> attribute,
            Order order );

    <V extends Comparable<? super V>> void isIn( SingularAttribute<E, V> attribute, Collection<?> value );

    <V extends Comparable<? super V>> void isEqual( SingularAttribute<E, V> attribute, V value );
}
