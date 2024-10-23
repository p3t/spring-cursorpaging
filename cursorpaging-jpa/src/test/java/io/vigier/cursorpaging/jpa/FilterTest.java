package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Set;
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
        Long value = 4711L;
        final var sut = Filter.create(
                f -> f.attribute( Attribute.path( DataRecord_.TAGS, Set.class, Tag_.ID, Long.class ) )
                        .equalTo( value ) );
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
}