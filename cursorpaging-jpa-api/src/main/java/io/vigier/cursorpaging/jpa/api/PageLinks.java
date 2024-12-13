package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializer;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.ResponseEntity;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor( staticName = "of" )
public class PageLinks<T, E> {
    private final Class<T> controllerClass;
    private final RequestSerializer<E> requestSerializer;

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

    public LinkBuilder self( final Page<E> page ) {
        final String selfCursor = requestSerializer.toBase64( page.self() ).toString();
        return new LinkBuilder( selfCursor, IanaLinkRelations.SELF );
    }

    public LinkBuilder next( final Page<E> page ) {
        final String nextCursor = page.next()
                .map( next -> requestSerializer.toBase64( next ).toString() )
                .orElse( null );
        return new LinkBuilder( nextCursor, IanaLinkRelations.NEXT );
    }

    // actually the Link#expand method should do this, but there seems to be an issue,
    // even when setting relaxed-query-chars: "[,],{,},|" in application.yml
    private static Link expand( final Link link ) {
        final var template = link.getHref();
        final StringBuilder href = new StringBuilder();
        int idx = template.indexOf( "&" );
        if ( idx > 0 ) {
            href.append( template, 0, idx );
            while ( idx > 0 ) {
                final int idy = template.indexOf( "=", idx );
                if ( idy > 0 && template.charAt( idy + 1 ) != '{' ) {
                    int idz = template.indexOf( "&", idy );
                    idz = idz < 0 ? template.length() : idz;
                    href.append( template.substring( idx, idz ) );
                }
                idx = template.indexOf( "&", idx + 1 );
            }
            return Link.of( href.toString(), link.getRel() )
                    .withName( link.getName() )
                    .withTitle( link.getTitle() )
                    .withType( link.getType() )
                    .withMedia( link.getMedia() )
                    .withProfile( link.getProfile() );
        }
        return link;
    }
}
