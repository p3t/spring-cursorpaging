package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializerFactory;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.ResponseEntity;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor( staticName = "of" )
public class PageLinks<T> {
    private final Class<T> controllerClass;
    private final RequestSerializerFactory requestSerializerFactory;

    @RequiredArgsConstructor
    public class LinkBuilder {
        private final String cursor;
        private final LinkRelation rel;

        /**
         * Creates a link for the provided controller method
         *
         * @param onMethod The function to provide the link by calling the controller method
         * @param <R>      Response entity type
         * @return The link or {@code null} in case the cursor is null (not present in the page)
         */
        public <R> Link on( final BiFunction<String, T, ResponseEntity<R>> onMethod ) {
            return (cursor == null) ? null : expand(
                    linkTo( onMethod.apply( cursor, methodOn( controllerClass ) ) ).withRel( rel ) );
        }
    }

    public <E> LinkBuilder self( final Page<E> page ) {
        final var requestSerializer = requestSerializerFactory.forEntity( page.entityType() );
        final String selfCursor = requestSerializer.toBase64( page.self() ).toString();
        return new LinkBuilder( selfCursor, IanaLinkRelations.SELF );
    }

    public <E> LinkBuilder next( final Page<E> page ) {
        final var requestSerializer = requestSerializerFactory.forEntity( page.entityType() );
        final String nextCursor = page.next()
                .map( next -> requestSerializer.toBase64( next ).toString() )
                .orElse( null );
        return new LinkBuilder( nextCursor, IanaLinkRelations.NEXT );
    }

    // actually the Link#expand method should do this, but there seems to be an issue,
    // even when setting relaxed-query-chars: "[,],{,},|" in application.yml
    static Link expand( final Link link ) {
        final var template = link.getHref();
        if ( template.indexOf( "&" ) > 0 ) {
            final var href = template.replaceAll( "&[^=]+=\\{[^}]+\\}|\\{&[^}]+\\}", "" );
            return Link.of( href, link.getRel() )
                    .withName( link.getName() )
                    .withTitle( link.getTitle() )
                    .withType( link.getType() )
                    .withMedia( link.getMedia() )
                    .withProfile( link.getProfile() );
        }
        return link;
    }
}
