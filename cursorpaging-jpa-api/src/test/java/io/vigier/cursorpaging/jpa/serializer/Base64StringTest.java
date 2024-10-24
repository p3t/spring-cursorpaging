package io.vigier.cursorpaging.jpa.serializer;

import jakarta.validation.ValidationException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base64StringTest {

    @Test
    void shouldCreateValidBase64String() {
        final var encoded = Base64.getEncoder().encodeToString( "test".getBytes() );
        final var base64String = new Base64String( encoded );
        assertThat( base64String ).hasToString( encoded );
    }

    @Test
    void shouldCreateValidBase64StringWithoutEqualsAtEnd() {
        final var encoded = Base64.getEncoder().encodeToString( "test".getBytes() ).replace( "=","" );
        final var base64String = new Base64String( encoded );
        assertThat( base64String ).hasToString( encoded );
    }

    @Test
    void shouldNotAcceptInvalidString() {
        assertThatThrownBy( () -> new Base64String( "invalid\\ncd..\\..\\..\n" ) )
                .isInstanceOf( ValidationException.class )
                .hasMessageContaining( "must be a valid Base64 string" );
    }
}
