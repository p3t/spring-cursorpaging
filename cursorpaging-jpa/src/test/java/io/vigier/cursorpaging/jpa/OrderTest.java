package io.vigier.cursorpaging.jpa;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

    @ParameterizedTest
    @ValueSource( strings = { "ASC", "asc", "DESC", "desc" } )
    void shouldConvertStringsToEnum( final String sort ) {
        final Order order = Order.from( sort );
        Assertions.assertThat( order ).isNotNull();
    }

    @Test
    void shouldNotAcceptUnknownSort() {
        Assertions.assertThatThrownBy( () -> Order.from( "UNKNOWN" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Unknown order" );
    }
}