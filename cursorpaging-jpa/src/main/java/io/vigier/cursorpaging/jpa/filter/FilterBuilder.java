package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterBuilder {

    public enum FilterType {
        EQUAL, GREATER_THAN, LESS_THAN, LIKE
    }

    private FilterType filterType;
    private Attribute attribute;
    private List<Comparable<?>> values = new ArrayList<>();

    public FilterBuilder attribute( final SingularAttribute<?, ? extends Comparable<?>> attribute ) {
        this.attribute = Attribute.of( attribute );
        return this;
    }

    public FilterBuilder attribute( final Attribute attribute ) {
        this.attribute = attribute;
        return this;
    }

    /**
     * Creates an attribute as path to an embedded/related entity's property.
     *
     * @param attributes the of to the property
     * @return the builder
     */
    @SafeVarargs
    public final FilterBuilder path(
            final jakarta.persistence.metamodel.Attribute<?, ? extends Comparable<?>>... attributes ) {
        this.attribute = Attribute.of( attributes );
        return this;
    }

    public FilterBuilder equalTo( final Comparable<?> value ) {
        this.values( value );
        this.filterType = FilterType.EQUAL;
        return this;
    }

    /**
     * Same as {@linkplain #in(List)} )}
     *
     * @param values List of allowed values to be equal to
     * @return the builder
     */
    public FilterBuilder equalTo( final List<? extends Comparable<?>> values ) {
        return in( values );
    }

    public FilterBuilder in( final Comparable<?>... values ) {
        return in( Arrays.asList( values ) );
    }

    public FilterBuilder in( final List<? extends Comparable<?>> values ) {
        values( values );
        this.filterType = FilterType.EQUAL;
        return this;
    }

    public FilterBuilder like( final Comparable<?>... values ) {
        return like( Arrays.asList( values ) );
    }

    public FilterBuilder like( final List<? extends Comparable<?>> values ) {
        this.values( values );
        this.filterType = FilterType.LIKE;
        return this;
    }

    public FilterBuilder greaterThan( final Comparable<?>... values ) {
        return greaterThan( Arrays.asList( values ) );
    }

    public FilterBuilder greaterThan( final List<? extends Comparable<?>> values ) {
        this.values( values );
        this.filterType = FilterType.GREATER_THAN;
        return this;
    }

    public FilterBuilder lessThan( final Comparable<?>... values ) {
        return lessThan( Arrays.asList( values ) );
    }

    public FilterBuilder lessThan( final List<? extends Comparable<?>> values ) {
        this.values( values );
        this.filterType = FilterType.LESS_THAN;
        return this;
    }

    public FilterBuilder values( final Comparable<?>... values ) {
        return values( Arrays.asList( values ) );
    }

    public FilterBuilder values( final List<? extends Comparable<?>> values ) {
        this.values = (values != null ? new ArrayList<>( values ) : List.of());
        return this;
    }

    public FilterBuilder type( final FilterType filterType ) {
        this.filterType = filterType;
        return this;
    }

    public Filter build() {
        if ( filterType == null ) {
            throw new IllegalStateException( "No operation/value specificed" );
        }
        return switch ( filterType ) {
            case EQUAL -> new EqualFilter( attribute, values );
            case GREATER_THAN -> new GreaterThanFilter( attribute, values );
            case LESS_THAN -> new LessThanFilter( attribute, values );
            case LIKE -> new LikeFilter( attribute, values );
        };
    }
}
