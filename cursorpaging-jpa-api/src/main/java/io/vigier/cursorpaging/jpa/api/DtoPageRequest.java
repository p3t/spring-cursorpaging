package io.vigier.cursorpaging.jpa.api;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.QueryElement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/**
 * {@link PageRequest} representation, which can be used in a REST API as request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude( Include.NON_NULL )
public class DtoPageRequest {

    private Map<String, Order> orderBy;

    @JsonTypeInfo( use = Id.NAME, include = As.WRAPPER_OBJECT )
    @JsonSubTypes( { @Type( value = DtoAndFilter.class, name = "AND" ), @Type( value = DtoOrFilter.class, name = "OR" ),
            @Type( value = DtoEqFilter.class, name = "EQ" ), @Type( value = DtoGtFilter.class, name = "GT" ),
            @Type( value = DtoLtFilter.class, name = "LT" ), @Type( value = DtoLikeFilter.class, name = "LIKE" ) } )
    public interface DtoFilterElement {

    }

    @Builder.Default
    private final DtoFilterList filterBy = new DtoAndFilter();

    @Data
    @NoArgsConstructor
    @SuperBuilder
    public abstract static class DtoFilter implements DtoFilterElement {

        @JsonIgnore
        private String attribute;
        @JsonIgnore
        @Singular
        private List<String> values;

        public abstract Filter create( Attribute attribute, List<? extends Comparable<?>> values );

        @JsonAnyGetter
        public Map<String, List<String>> getJson() {
            return Map.of( attribute, values );
        }

        @JsonAnySetter
        public void setAttribute( String name, Object value ) {
            this.attribute = name;
            this.values = value instanceof List<?> l ? l.stream().map( Objects::toString ).toList() : List.of();
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "EQ" )
    public static class DtoEqFilter extends DtoFilter {

        @Override
        public Filter create( final Attribute attribute, final List<? extends Comparable<?>> values ) {
            return Filters.attribute( attribute ).equalTo( values );
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "GT" )
    public static class DtoGtFilter extends DtoFilter {

        @Override
        public Filter create( final Attribute attribute, final List<? extends Comparable<?>> values ) {
            return Filters.attribute( attribute ).greaterThan( values );
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "LT" )
    public static class DtoLtFilter extends DtoFilter {

        @Override
        public Filter create( final Attribute attribute, final List<? extends Comparable<?>> values ) {
            return Filters.attribute( attribute ).lessThan( values );
        }

    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "LIKE" )
    public static class DtoLikeFilter extends DtoFilter {

        @Override
        public Filter create( final Attribute attribute, final List<? extends Comparable<?>> values ) {
            return Filters.attribute( attribute ).like( values );
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public abstract static class DtoFilterList implements DtoFilterElement, Iterable<DtoFilterElement> {

        @Singular
        private List<DtoFilterElement> filters;

        @JsonValue
        public List<DtoFilterElement> getFilters() {
            return filters;
        }

        @Override
        public Iterator<DtoFilterElement> iterator() {
            return filters.iterator();
        }

        @Override
        public void forEach( final Consumer<? super DtoFilterElement> action ) {
            filters.forEach( action );
        }

        public int size() {
            return filters.size();
        }
    }

    public abstract static class DtoFilterListDeserializer extends StdDeserializer<DtoFilterList> {

        protected DtoFilterListDeserializer( Class<? extends DtoFilterList> vc ) {
            super( vc );
        }

        @Override
        public DtoFilterList deserialize( JsonParser jp, DeserializationContext ctxt ) throws IOException {
            JsonNode node = jp.getCodec().readTree( jp );
            List<DtoFilterElement> filterList = new LinkedList<>();
            for ( JsonNode n : node ) {
                filterList.add( jp.getCodec().treeToValue( n, DtoFilterElement.class ) );
            }
            return create( filterList );
        }

        protected abstract DtoFilterList create( List<DtoFilterElement> filters );
    }

    public static class DtoAndFilterListDeserializer extends DtoFilterListDeserializer {

        public DtoAndFilterListDeserializer() {
            super( DtoAndFilter.class );
        }

        @Override
        protected DtoFilterList create( List<DtoFilterElement> filters ) {
            return DtoAndFilter.builder().filters( filters )
                    .build();
        }
    }

    public static class DtoOrFilterListDeserializer extends DtoFilterListDeserializer {

        public DtoOrFilterListDeserializer() {
            super( DtoOrFilter.class );
        }

        @Override
        protected DtoFilterList create( List<DtoFilterElement> filters ) {
            return DtoOrFilter.builder().filters( filters )
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "AND" )
    @JsonDeserialize( using = DtoAndFilterListDeserializer.class )
    public static class DtoAndFilter extends DtoFilterList {

        @JsonAnySetter
        public void setContent( Map<String, List<DtoFilterElement>> filters ) {
            setFilters( filters.entrySet().iterator().next().getValue() );
        }
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode( callSuper = true )
    @SuperBuilder
    @JsonTypeName( "OR" )
    @JsonDeserialize( using = DtoOrFilterListDeserializer.class )
    public static class DtoOrFilter extends DtoFilterList {

    }


    @Min( 1 )
    @Max( 100 )
    @Builder.Default
    private int pageSize = 100; // Must not be final!

    private boolean withTotalCount;

    public static DtoPageRequest create( Consumer<DtoPageRequestBuilder> c ) {
        final DtoPageRequestBuilder builder = DtoPageRequest.builder();
        c.accept( builder );
        return builder.build();
    }

    public <T> PageRequest<T> toPageRequest( final Function<String, Attribute> attributeProvider ) {
        return PageRequest.create( b -> {
            orderBy.forEach( ( name, order ) -> {
                final Attribute attribute = attributeProvider.apply( name );
                switch ( order ) {
                    case ASC -> b.asc( attribute );
                    case DESC -> b.desc( attribute );
                }
            } );
            b.pageSize( pageSize ).enableTotalCount( withTotalCount );
            filterBy.forEach( f -> b.filter( filterOf( f, attributeProvider ) ) );
        } );
    }

    private QueryElement filterOf( final DtoFilterElement f, final Function<String, Attribute> attributeProvider ) {
        if ( f instanceof DtoFilter filter ) {
            final Attribute attribute = attributeProvider.apply( filter.getAttribute() );
            return filter.create( attribute, filter.getValues() );
        } else if ( f instanceof DtoAndFilter list ) {
            return Filters.and( list.getFilters().stream().map( e -> filterOf( e, attributeProvider ) ).toList() );
        } else if ( f instanceof DtoOrFilter list ) {
            return Filters.or( list.getFilters().stream().map( e -> filterOf( e, attributeProvider ) ).toList() );
        }
        throw new IllegalStateException( "Unknown filter element: " + (f != null ? f.getClass().getName() : "null") );
    }

    public void addOrderByIfAbsent( final String name, final Order order ) {
        if ( !orderBy.containsKey( name ) ) {
            orderBy.put( name, order );
        }
    }
}
