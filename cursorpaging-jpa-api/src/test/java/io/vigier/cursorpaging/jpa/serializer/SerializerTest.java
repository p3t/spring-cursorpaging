package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.Position;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.SingleAttribute;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import static io.vigier.cursorpaging.jpa.Filters.attribute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class SerializerTest {

    @Data
    private static class ValueClass implements Comparable<ValueClass> {
        private final String theValue;

        @Override
        public int compareTo( final ValueClass o ) {
            return theValue.compareTo( o.theValue );
        }
    }

    @Data
    private static class TestEntity {
        private Long id;
        private String name;
        private ValueClass value;
        private Instant time;
    }

    private static class TestEntity_ {
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, Long> id = Mockito.mock( SingularAttribute.class );
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, String> name = Mockito.mock( SingularAttribute.class );
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, ValueClass> value = Mockito.mock(
                SingularAttribute.class );
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, Instant> time = Mockito.mock( SingularAttribute.class );
    }

    private static class ValueClass_ {
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<ValueClass, String> theValue = Mockito.mock( SingularAttribute.class );
    }

    @Mock
    private ConversionService conversionService;

    @BeforeAll
    static void setup() {
        when( TestEntity_.name.getJavaType() ).thenReturn( String.class );
        when( TestEntity_.name.getName() ).thenReturn( "name" );
        lenient().when( TestEntity_.value.getJavaType() ).thenReturn( ValueClass.class );
        lenient().when( TestEntity_.value.getName() ).thenReturn( "value" );
        lenient().when( TestEntity_.id.getJavaType() ).thenReturn( Long.class );
        lenient().when( TestEntity_.id.getName() ).thenReturn( "id" );
        lenient().when( ValueClass_.theValue.getJavaType() ).thenReturn( String.class );
        lenient().when( ValueClass_.theValue.getName() ).thenReturn( "theValue" );
        lenient().when( TestEntity_.time.getJavaType() ).thenReturn( Instant.class );
        lenient().when( TestEntity_.time.getName() ).thenReturn( "time" );
    }

    @Test
    void shouldSerializePageRequests() {
        final PageRequest<TestEntity> pageRequest = PageRequest.create( b -> b.desc( TestEntity_.name ) );

        final var deserializeRequest = serializeAndDeserialize( pageRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializePageRequestsWithPosition() {
        final PageRequest<TestEntity> pageRequest = PageRequest.create( b -> b.position( Position.create(
                p -> p.order( Order.ASC ).attribute( Attribute.of( TestEntity_.id ) ).value( 4711L ) ) ) );

        final var deserializeRequest = serializeAndDeserialize( pageRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
        assertThat( deserializeRequest.isFirstPage() ).isFalse();
        assertThat( deserializeRequest.positions() ).first()
                .satisfies( p -> assertThat( p.value() ).isEqualTo( 4711L ) );
    }

    @Test
    void shouldThrowExceptionWhenNotDeserializeable() {
        // The 'value' in the position of type ValueClass is can be serialized (via toString)
        // but not converted back (no converter configured)
        final PageRequest<TestEntity> pageRequest = PageRequest.create( b -> b.position( Position.create(
                p -> p.order( Order.ASC )
                        .attribute( Attribute.of( TestEntity_.value ) )
                        .value( new ValueClass( "123" ) ) ) ) );

        Assertions.assertThatThrownBy( () -> serializeAndDeserialize( pageRequest ) )
                .isInstanceOf( SerializerException.class );
    }

    @Test
    void shouldDeserializePositionsWithPathAttributes() {
        final PageRequest<TestEntity> request = PageRequest.create( r -> r.position( Position.create(
                pos -> pos.attribute( Attribute.of( TestEntity_.value, ValueClass_.theValue ) )
                        .value( "123" )
                        .order( Order.ASC ) ) ) );

        final var deserializeRequest = serializeAndDeserialize( request );

        assertThat( deserializeRequest.positions() ).hasSize( 1 );
        final var pos = deserializeRequest.positions().getFirst();
        assertThat( pos.attribute().attributes() ).hasSize( 2 );
    }

    @Test
    void shouldAcceptNullAsCursorString() {
        assertThat( getRequestSerializer().stringToPageRequest( null ) ).isEmpty();
    }

    @Test
    void shouldDeserializeFromCursorString() {
        final PageRequest<TestEntity> request = PageRequest.create( r -> r.asc( TestEntity_.id ).pageSize( 42 ) );
        final var requestSerializer = getRequestSerializer();
        final String cursor = requestSerializer.toBase64( request ).toString();
        assertThat( requestSerializer.stringToPageRequest( cursor ) ).isPresent()
                .get()
                .isEqualTo( request )
                .satisfies( r -> {
                    assertThat( r.pageSize() ).isEqualTo( 42 );
                    assertThat( r.positions() ).isNotEmpty();
                } );
    }

    private static PageRequest<TestEntity> serializeAndDeserialize( final PageRequest<TestEntity> pageRequest ) {
        final var serializer = getRequestSerializer();
        final var serializedRequest = serializer.toBase64( pageRequest );
        return serializer.toPageRequest( serializedRequest );
    }

    private static RequestSerializer<TestEntity> getRequestSerializer() {
        return RequestSerializer.create( TestEntity.class )
                .apply( b -> b.use( Attribute.of( TestEntity_.id ) )
                        .use( Attribute.of( TestEntity_.name ) )
                        .use( Attribute.of( TestEntity_.value ) )
                        .use( Attribute.of( ValueClass_.theValue ) ) );
    }

    @Test
    void shouldSerializePageRequestsWithOrFilter() {
        final PageRequest<TestEntity> pageRequest = PageRequest.create(
                b -> b.desc( TestEntity_.name ).filter( Filters.or( //
                        attribute( TestEntity_.name ).equalTo( "Name-1" ), //
                        attribute( TestEntity_.id ).greaterThan( 1L ) //
                ) ) );
        when( conversionService.convert( anyString(), eq( Long.class ) ) ).thenAnswer(
                i -> Long.valueOf( (String) i.getArguments()[0] ) );
        when( conversionService.convert( anyString(), eq( String.class ) ) ).thenAnswer( i -> i.getArguments()[0] );

        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class,
                b -> b.use( Attribute.of( TestEntity_.name ) )
                        .use( Attribute.of( TestEntity_.id ) )
                        .conversionService( conversionService ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializePageRequestsWithMultipleAttributes() {
        final var attribute1 = Attribute.of( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        final var pageRequest = PageRequest.<TestEntity>create(
                b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );

        final var serializer = RequestSerializer.create( TestEntity.class, b -> b.use( attribute1 ).use( attribute2 ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializeReversedPageRequests() {
        final var request = PageRequest.create( r -> r.position( Position.create(
                p -> p.reversed( true ).order( Order.ASC ).attribute( Attribute.of( "some_name", String.class ) ) ) ) );
        final RequestSerializer<Object> serializer = RequestSerializer.create( Object.class ).withDefaults();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest.isReversed() ).isTrue();
        assertThat( deserializedRequest ).isEqualTo( request );

    }

    @Test
    void shouldSerializeTotalCountIfPresent() {
        final var request = createPageRequest().copy( b -> b.enableTotalCount( true ).totalCount( 42L ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class ).withDefaults();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest ).isEqualTo( request ).satisfies( r -> {
            assertThat( r.totalCount() ).isPresent().get().isEqualTo( 42L );
            assertThat( r.enableTotalCount() ).isTrue();
        } );
    }

    @Test
    void shouldDeserializeAndFilter() {
        final PageRequest<TestEntity> request = PageRequest.create( r -> r.filter(
                Filters.and( attribute( TestEntity_.id ).equalTo( 123L ),
                        attribute( TestEntity_.name ).like( "%bumlux%" ) ) ).asc( TestEntity_.id ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class ).withDefaults();
        final var serializedRequest = serializer.toBytes( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializedRequest ).isEqualTo( request );
    }

    @Test
    void shouldSerializeParametersOfFilterRules() {
        final Map<String, List<String>> parameters = Map.of( "Test1", List.of( "Value1" ) );
        final var request = createPageRequest().copy( b -> b.rule( newTestRule( "TestRule", parameters ) ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class,
                c -> c.filterRuleFactory( "TestRule", p -> newTestRule( "TestRule", p ) ) );
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializedRequest.rules() ).hasSize( 1 ).first().satisfies( r -> {
            assertThat( r.name() ).isEqualTo( "TestRule" );
            assertThat( r.parameters() ).containsEntry( "Test1", List.of( "Value1" ) );
        } );
    }

    private FilterRule newTestRule( final String name, final Map<String, List<String>> parameters ) {
        return new FilterRule() {
            @Override
            public Predicate toPredicate( final QueryBuilder cqb ) {
                return null;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public Map<String, List<String>> parameters() {
                return parameters;
            }
        };
    }

    @Test
    void shouldLearnAttributesBySerializing() {
        final var request = createPageRequest();
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class, b -> {} );
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest ).isEqualTo( request );
    }

    private PageRequest<TestEntity> createPageRequest() {
        final var attribute1 = Attribute.of( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        return PageRequest.create( b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );
    }

    @Test
    void shouldSerializeAndDeserializeNanosOfInstants() {
        final var positionTime = "2022-01-01T12:34:56.123456789Z";
        final var request = PageRequest.<TestEntity>create( r -> r.filter( Filters.attribute( "time", Instant.class )
                        .greaterThan( Instant.parse( "2021-01-01T12:34:56.123456789Z" ) ) )
                .position( Position.create( p -> p.attribute( Attribute.of( TestEntity_.time ) )
                        .order( Order.ASC )
                        .value( Instant.parse( positionTime ) ) ) ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create( TestEntity.class, b -> {} );
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializedRequest ).isEqualTo( request );
        assertThat( deserializedRequest.positions().getFirst().value().toString() ).isEqualTo( positionTime );
    }
}
