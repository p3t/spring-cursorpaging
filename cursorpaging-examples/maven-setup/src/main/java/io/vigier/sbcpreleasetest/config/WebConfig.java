package io.vigier.sbcpreleasetest.config;

import io.vigier.cursorpaging.jpa.api.StringToBase64StringConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableHypermediaSupport( type = { EnableHypermediaSupport.HypermediaType.HAL } )
public class WebConfig implements WebMvcConfigurer {

    /**
     * Optional configuration to auto-convert base64 containing strings into a value class
     *
     * @return converter
     */
    @Bean
    public StringToBase64StringConverter stringToBase64StringConverter() {
        return new StringToBase64StringConverter();
    }
}
