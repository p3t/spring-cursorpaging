package io.vigier.cursorpaging.jpa.rsql.filter;

import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.filter.AndFilter;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import io.vigier.cursorpaging.jpa.rsql.filter.model.TestEntity;
import io.vigier.cursorpaging.jpa.rsql.filter.model.TestMetaModel;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith( MockitoExtension.class )
class RsqlFilterFactoryTest {

    private RsqlFilterFactory<TestEntity> factory;

    @BeforeEach
    void setUp() {
        final var metaModel = TestMetaModel.init();
        factory = new RsqlFilterFactory<>( metaModel.entityManager(), metaModel.entityType() );
    }

    // -- parameterized: RSQL expression → expected attribute name, type, operation, values ---------

    static Stream<Arguments> rsqlExpressions() {
        return Stream.of( //
                // simple comparisons
                Arguments.of( "name==John", "name", String.class, FilterType.EQUAL_TO, List.of( "John" ) ),
                Arguments.of( "age=gt=30", "age", Integer.class, FilterType.GREATER_THAN, List.of( 30 ) ),
                Arguments.of( "age=ge=18", "age", Integer.class, FilterType.GREATER_THAN_OR_EQUAL_TO, List.of( 18 ) ),
                Arguments.of( "age=lt=50", "age", Integer.class, FilterType.LESS_THAN, List.of( 50 ) ),
                Arguments.of( "age=le=65", "age", Integer.class, FilterType.LESS_THAN_OR_EQUAL_TO, List.of( 65 ) ),
                // in operator
                Arguments.of( "name=in=(Alice,Bob,Charlie)", "name", String.class, FilterType.EQUAL_TO,
                        List.of( "Alice", "Bob", "Charlie" ) ),
                // integer type conversion
                Arguments.of( "age==42", "age", Integer.class, FilterType.EQUAL_TO, List.of( 42 ) ),
                // dotted / embedded attribute paths
                Arguments.of( "auditInfo.createdAt=gt=2024-01-01T00:00:00Z", "auditInfo.createdAt", Instant.class,
                        FilterType.GREATER_THAN, List.of( Instant.parse( "2024-01-01T00:00:00Z" ) ) ),
                Arguments.of( "auditInfo.modifiedAt=le=2025-06-15T12:00:00Z", "auditInfo.modifiedAt", Instant.class,
                        FilterType.LESS_THAN_OR_EQUAL_TO, List.of( Instant.parse( "2025-06-15T12:00:00Z" ) ) ) );
    }

    @ParameterizedTest
    @MethodSource( "rsqlExpressions" )
    void shouldParseSimpleRsqlExpression( final String rsql, final String expectedName,
            final Class<?> expectedType, final FilterType expectedOp, final List<Object> expectedValues ) {
        final QueryElement result = factory.toFilter( rsql );

        assertThat( result ).isInstanceOf( Filter.class );
        final Filter filter = (Filter) result;
        assertThat( filter.attribute().name() ).isEqualTo( expectedName );
        assertThat( filter.attribute().type() ).isEqualTo( expectedType );
        assertThat( filter.operation() ).isEqualTo( expectedOp );
        assertThat( values( filter ) ).isEqualTo( expectedValues );
    }

    // -- AND combination --

    @Test
    void shouldCreateAndFilter() {
        final QueryElement result = factory.toFilter( "name==John;age=gt=25" );

        assertThat( result ).isInstanceOf( AndFilter.class );
        final AndFilter andFilter = (AndFilter) result;
        assertThat( andFilter.filters() ).hasSize( 2 );

        final Filter nameFilter = (Filter) andFilter.filters().getFirst();
        assertThat( nameFilter.attribute().name() ).isEqualTo( "name" );
        assertThat( nameFilter.operation() ).isEqualTo( FilterType.EQUAL_TO );
        assertThat( values( nameFilter ) ).containsExactly( "John" );

        final Filter ageFilter = (Filter) andFilter.filters().get( 1 );
        assertThat( ageFilter.attribute().name() ).isEqualTo( "age" );
        assertThat( ageFilter.operation() ).isEqualTo( FilterType.GREATER_THAN );
        assertThat( values( ageFilter ) ).containsExactly( 25 );
    }

    // -- OR combination --

    @Test
    void shouldCreateOrFilter() {
        final QueryElement result = factory.toFilter( "name==Alice,name==Bob" );

        assertThat( result ).isInstanceOf( OrFilter.class );
        final OrFilter orFilter = (OrFilter) result;
        assertThat( orFilter.filters() ).hasSize( 2 );

        final Filter first = (Filter) orFilter.filters().getFirst();
        assertThat( first.attribute().name() ).isEqualTo( "name" );
        assertThat( values( first ) ).containsExactly( "Alice" );

        final Filter second = (Filter) orFilter.filters().get( 1 );
        assertThat( second.attribute().name() ).isEqualTo( "name" );
        assertThat( values( second ) ).containsExactly( "Bob" );
    }

    // -- Nested AND / OR --

    @Test
    void shouldCreateNestedAndOrFilter() {
        // (name==Alice OR name==Bob) AND age=gt=20
        final QueryElement result = factory.toFilter( "(name==Alice,name==Bob);age=gt=20" );

        assertThat( result ).isInstanceOf( AndFilter.class );
        final AndFilter andFilter = (AndFilter) result;
        assertThat( andFilter.filters() ).hasSize( 2 );

        assertThat( andFilter.filters().getFirst() ).isInstanceOf( OrFilter.class );
        final OrFilter orPart = (OrFilter) andFilter.filters().getFirst();
        assertThat( orPart.filters() ).hasSize( 2 );

        assertThat( andFilter.filters().get( 1 ) ).isInstanceOf( Filter.class );
        final Filter agePart = (Filter) andFilter.filters().get( 1 );
        assertThat( agePart.attribute().name() ).isEqualTo( "age" );
        assertThat( agePart.operation() ).isEqualTo( FilterType.GREATER_THAN );
    }

    // -- Complex expression: AND of multiple conditions with type conversion --

    @Test
    void shouldHandleComplexAndExpression() {
        final QueryElement result = factory.toFilter(
                "name==John;age=ge=18;auditInfo.createdAt=gt=2024-01-01T00:00:00Z" );

        assertThat( result ).isInstanceOf( AndFilter.class );
        final AndFilter andFilter = (AndFilter) result;
        assertThat( andFilter.filters() ).hasSize( 3 );

        final Filter nameFilter = (Filter) andFilter.filters().getFirst();
        assertThat( nameFilter.attribute().name() ).isEqualTo( "name" );
        assertThat( nameFilter.operation() ).isEqualTo( FilterType.EQUAL_TO );
        assertThat( values( nameFilter ) ).containsExactly( "John" );

        final Filter ageFilter = (Filter) andFilter.filters().get( 1 );
        assertThat( ageFilter.attribute().name() ).isEqualTo( "age" );
        assertThat( ageFilter.operation() ).isEqualTo( FilterType.GREATER_THAN_OR_EQUAL_TO );
        assertThat( values( ageFilter ) ).containsExactly( 18 );

        final Filter auditFilter = (Filter) andFilter.filters().get( 2 );
        assertThat( auditFilter.attribute().name() ).isEqualTo( "auditInfo.createdAt" );
        assertThat( auditFilter.attribute().type() ).isEqualTo( Instant.class );
        assertThat( auditFilter.operation() ).isEqualTo( FilterType.GREATER_THAN );
        assertThat( values( auditFilter ) ).containsExactly( Instant.parse( "2024-01-01T00:00:00Z" ) );
    }

    // -- Unsupported / invalid RSQL --

    @Test
    void shouldThrowForUnsupportedOperator() {
        assertThatThrownBy( () -> factory.toFilter( "name!=John" ) ).isInstanceOf( UnsupportedOperationException.class )
                .hasMessageContaining( "Operator not supported" );
    }

    @Test
    void shouldRejectNotInOperator() {
        assertThatThrownBy( () -> factory.toFilter( "name=out=(A,B)" ) ).isInstanceOf(
                UnsupportedOperationException.class ).hasMessageContaining( "Operator not supported" );
    }

    @Test
    void shouldRejectInvalidRsql() {
        assertThatThrownBy( () -> factory.toFilter( "==invalid" ) ).isInstanceOf(
                RsqlSyntaxException.class );
    }

    // -- Helpers --

    /**
     * Narrows the wildcard-typed value list so that AssertJ's {@code containsExactly} can infer types.
     */
    private static List<Object> values( final Filter filter ) {
        return filter.values().stream().map( v -> (Object) v ).toList();
    }

}

