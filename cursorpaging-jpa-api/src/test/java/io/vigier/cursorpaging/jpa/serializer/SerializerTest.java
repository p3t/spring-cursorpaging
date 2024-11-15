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
    private static class TestEntity {

        private Long id;
        private String name;
    }

    private static class TestEntity_ {

        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, Long> id = Mockito.mock( SingularAttribute.class );
        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, String> name = Mockito.mock( SingularAttribute.class );
    }

    @Mock
    private ConversionService conversionService;

    @BeforeAll
    static void setup() {
        when( TestEntity_.name.getJavaType() ).thenReturn( String.class );
        when( TestEntity_.name.getName() ).thenReturn( "name" );
        lenient().when( TestEntity_.id.getJavaType() ).thenReturn( Long.class );
        lenient().when( TestEntity_.id.getName() ).thenReturn( "id" );
    }

    @Test
    void shouldSerializePageRequests() {
        final PageRequest<TestEntity> pageRequest = PageRequest.create( b -> b.desc( TestEntity_.name ) );

        final RequestSerializer<TestEntity> serializer = RequestSerializer.create(
                b -> b.use( Attribute.of( TestEntity_.name ) ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
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

        final RequestSerializer<TestEntity> serializer = RequestSerializer.create(
                b -> b.use( Attribute.of( TestEntity_.name ) )
                        .use( Attribute.of( TestEntity_.id ) )
                        .conversionService( conversionService ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializePageRequestsWithMultipleAttributes() {
        final var attribute1 = Attribute.path( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        final var pageRequest = PageRequest.create( b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );

        final var serializer = RequestSerializer.create( b -> b.use( attribute1 ).use( attribute2 ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializeReversedPageRequests() {
        final var request = PageRequest.create( r -> r.position( Position.create(
                p -> p.reversed( true ).order( Order.ASC ).attribute( Attribute.of( "some_name", String.class ) ) ) ) );
        final RequestSerializer<Object> serializer = RequestSerializer.create();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest.isReversed() ).isTrue();
        assertThat( deserializedRequest ).isEqualTo( request );

    }

    @Test
    void shouldSerializeTotalCountIfPresent() {
        final var request = createPageRequest().copy( b -> b.enableTotalCount( true ).totalCount( 42L ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest ).isEqualTo( request ).satisfies( r -> {
            assertThat( r.totalCount() ).isPresent().get().isEqualTo( 42L );
            assertThat( r.enableTotalCount() ).isTrue();
        } );
    }

    @Test
    void shouldDeserializeAndFilter() {
        PageRequest<TestEntity> request = PageRequest.create( r -> r.filter(
                Filters.and( attribute( TestEntity_.id ).equalTo( 123L ),
                        attribute( TestEntity_.name ).like( "%bumlux%" ) ) ).asc( TestEntity_.id ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create();
        final var serializedRequest = serializer.toBytes( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializedRequest ).isEqualTo( request );
    }

    @Test
    void shouldSerializeParametersOfFilterRules() {
        Map<String, List<String>> parameters = Map.of( "Test1", List.of( "Value1" ) );
        final var request = createPageRequest().copy( b -> b.rule( newTestRule( "TestRule", parameters ) ) );
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create(
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
        final RequestSerializer<TestEntity> serializer = RequestSerializer.create();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest ).isEqualTo( request );
    }

    private PageRequest<TestEntity> createPageRequest() {
        final var attribute1 = Attribute.path( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        return PageRequest.create( b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );
    }

}
