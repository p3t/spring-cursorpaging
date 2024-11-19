package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.Filters.FilterCreator;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Set;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith( MockitoExtension.class )
class FilterTest {

    @Mock
    private SingularAttribute<DataRecord, String> nameAttrMock;

    @Test
    void shouldCreateFilterFromJpaAttribute() {
        final var value = "test";
        Mockito.when( nameAttrMock.getName() ).thenReturn( "name" );
        Mockito.when( nameAttrMock.getJavaType() ).thenReturn( String.class );
        DataRecord_.name = nameAttrMock;
        final var sut = Filter.create( f -> f.attribute( DataRecord_.name ).equalTo( value ) );
        verifyNameAndValue( sut, DataRecord_.NAME, String.class, value );
    }

    @Test
    void shouldCreateFilterFromManualSpecifiedAttribute() {
        final var value = "test";
        final var sut = Filter.create(
                f -> f.attribute( Attribute.of( DataRecord_.NAME, String.class ) ).equalTo( value ) );
        verifyNameAndValue( sut, DataRecord_.NAME, String.class, value );
    }

    @Test
    void shouldCreateFilterFromManualSpecifiedAttributeWithMultipleValues() {
        final String[] values = { "test", "test2" };
        final var sut = Filter.create(
                f -> f.attribute( Attribute.of( DataRecord_.NAME, String.class ) ).in( values ) );
        verifyNameAndValue( sut, DataRecord_.NAME, String.class, values );
    }

    @Test
    void shouldCreateFilterFromPath() {
        final Long value = 4711L;
        final var sut = Filter.create(
                f -> f.attribute( Attribute.of( DataRecord_.TAGS, Set.class, Tag_.ID, Long.class ) ).equalTo( value ) );
        verifyNameAndValue( sut, DataRecord_.TAGS + "." + Tag_.ID, Long.class, value );
    }

    @SafeVarargs
    private static <E extends Comparable<? super E>> void verifyNameAndValue( final Filter sut, final String name,
            final Class<E> type, final E... values ) {
        assertThat( sut ).isNotNull().satisfies( f -> {
            assertThat( f.attribute() ).isNotNull().satisfies( a -> {
                assertThat( a.name() ).isEqualTo( name );
                assertThat( a.type() ).isEqualTo( type );
            } );
            assertThat( f.values( type ) ).containsExactly( values );
        } );
    }

    @Test
    void shouldBeEqual() {
        shouldBeEqual( f1 -> f1.like( "John" ), f2 -> f2.like( "John" ) );
        shouldBeEqual( f1 -> f1.equalTo( "John" ), f2 -> f2.equalTo( "John" ) );
        shouldBeEqual( f1 -> f1.greaterThan( "1L" ), f2 -> f2.greaterThan( "1L" ) );
        shouldBeEqual( f1 -> f1.lessThan( "1L" ), f2 -> f2.lessThan( "1L" ) );
    }

    private void shouldBeEqual( final Function<FilterCreator, Filter> f1, final Function<FilterCreator, Filter> f2 ) {
        final var name1 = Attribute.of( "name", String.class );
        final var name2 = Attribute.of( "name", String.class );

        Assertions.assertThat( f1.apply( Filters.attribute( name1 ) ) )
                .isEqualTo( f2.apply( Filters.attribute( name2 ) ) );
    }

    @Test
    void filterListsShouldBeEqual() {
        final var nameAndAge1 = Filters.and(
                Filters.attribute( Attribute.of( "name", String.class ) ).equalTo( "John" ),
                Filters.attribute( Attribute.of( "age", Long.class ) ).equalTo( 18 ) );
        final var nameAndAge2 = Filters.and(
                Filters.attribute( Attribute.of( "name", String.class ) ).equalTo( "John" ),
                Filters.attribute( Attribute.of( "age", Long.class ) ).equalTo( 18 ) );

        Assertions.assertThat( nameAndAge1 ).isEqualTo( nameAndAge2 );
        Assertions.assertThat( Filters.and( nameAndAge1 ) ).isEqualTo( Filters.and( nameAndAge2 ) );
        Assertions.assertThat( Filters.or( nameAndAge1 ) ).isEqualTo( Filters.or( nameAndAge2 ) );

        Assertions.assertThat( Filters.or( nameAndAge1 ) ).isNotEqualTo( Filters.and( nameAndAge2 ) );
    }
}