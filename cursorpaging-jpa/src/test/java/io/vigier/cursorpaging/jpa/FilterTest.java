package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import jakarta.persistence.metamodel.SingularAttribute;
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
        final var sut = Filter.attributeIs( DataRecord_.name, value );
        verifyNameAndValue( sut, DataRecord_.NAME, value );
    }

    @Test
    void shouldCreateFilterFromManualSpecifiedAttribute() {
        final var value = "test";
        final var sut = Filter.attributeIs( Attribute.of( DataRecord_.NAME, String.class ), value );
        verifyNameAndValue( sut, DataRecord_.NAME, value );
    }

    @Test
    void shouldCreateFilterFromManualSpecifiedAttributeWithMultipleValues() {
        final String[] values = { "test", "test2" };
        final var sut = Filter.attributeIs( Attribute.of( DataRecord_.NAME, String.class ), values );
        verifyNameAndValue( sut, DataRecord_.NAME, values );
    }


    private static void verifyNameAndValue( final Filter sut, final String name, final String... values ) {
        assertThat( sut ).isNotNull().satisfies( f -> {
            assertThat( f.attribute() ).isNotNull().satisfies( a -> {
                assertThat( a.name() ).isEqualTo( name );
                assertThat( a.type() ).isEqualTo( String.class );
            } );
            assertThat( f.values( String.class ) ).containsExactly( values );
        } );
    }
}