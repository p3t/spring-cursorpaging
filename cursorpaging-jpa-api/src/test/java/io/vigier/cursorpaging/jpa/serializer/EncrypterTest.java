package io.vigier.cursorpaging.jpa.serializer;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncrypterTest {

    @Test
    @SneakyThrows
    void shouldEncrypt() {
        final var instance = Encrypter.getInstance();
        final var encrypted = instance.encrypt( "Hello".getBytes() );
        final var decrypted = instance.decrypt( encrypted );

        Assertions.assertEquals( "Hello", new String( decrypted ) );
    }
}
