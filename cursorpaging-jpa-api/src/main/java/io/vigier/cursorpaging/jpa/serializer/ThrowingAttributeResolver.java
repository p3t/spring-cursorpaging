package io.vigier.cursorpaging.jpa.serializer;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.AttributeResolver;
import lombok.NoArgsConstructor;

@NoArgsConstructor( access = lombok.AccessLevel.PRIVATE )
public class ThrowingAttributeResolver implements AttributeResolver {
    public static final ThrowingAttributeResolver INSTANCE = new ThrowingAttributeResolver();

    @Override
    public Attribute resolve( final String name ) {
        throw new SerializerException(
                "Attribute '%s' not present in cache and no entity manager configured to resolve attributes".formatted(
                        name ) );
    }
}
