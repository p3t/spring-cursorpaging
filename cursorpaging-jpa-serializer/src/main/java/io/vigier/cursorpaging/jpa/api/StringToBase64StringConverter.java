package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.serializer.Base64String;
import org.springframework.core.convert.converter.Converter;

public class StringToBase64StringConverter implements Converter<String, Base64String> {

    @Override
    public Base64String convert( final String source ) {
        return new Base64String( source );
    }
}
