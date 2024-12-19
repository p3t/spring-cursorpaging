package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.Position;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializer;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializerFactory;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class PageLinksTest {

    @Data
    @AllArgsConstructor
    static final class TestEntity {
        private String id;
        private String name;
    }

    @org.springframework.stereotype.Controller
    @RequestMapping( Controller.BASE_PATH )
    public static class Controller {
        public static final String BASE_PATH = "/api/v1";

        @RequestMapping( "/page" )
        ResponseEntity<Page<TestEntity>> getEntities( @RequestParam( "cursor" ) final String cursor,
                @RequestParam( "pageSize" ) final Integer pageSize,
                @RequestParam( "names[]" ) final List<String> names ) {
            log.trace( "Cursor= {}, pageSize= {}; names= {}", cursor, pageSize, names );
            return ResponseEntity.ok( createPage() );
        }
    }

    private final RequestSerializerFactory requestSerializerFactory = RequestSerializerFactory.create(
            b -> b.serialalizer( RequestSerializer.create( TestEntity.class ).apply( r -> r //
                    .use( Attribute.of( "id", String.class ) ) //
                    .use( Attribute.of( "name", String.class ) ) ) ) );

    @Test
    void shouldGenerateLinksWithoutTemplate() {
        final Page<TestEntity> page = createPage();
        final var links = PageLinks.of( Controller.class, requestSerializerFactory );
        final Link selfLink = links.self( page )
                .on( ( cursor, controller ) -> controller.getEntities( cursor, null, null ) );
        final Link nextLink = links.next( page )
                .on( ( cursor, controller ) -> controller.getEntities( cursor, null, null ) );

        log.info( "Self link: {}", selfLink.getHref() );
        log.info( "next link: {}", nextLink.getHref() );

        assertThat( selfLink.getHref() ).contains( "?cursor=" );
        assertThat( nextLink.getHref() ).contains( "?cursor=" );
        assertThat( selfLink.getHref() ).doesNotContain( "pageSize", "names[]", "{", "}" );
        assertThat( nextLink.getHref() ).doesNotContain( "pageSize", "names[]", "{", "}" );
    }

    @Test
    void shouldRemoveTemplateVariables() {
        final var url = "/api/some/v1/what?cursor=RabarbarBarbar{&tag[],lastModifiedAt[gt],some[],}";
        final Link result = PageLinks.expand( Link.of( url, "self" ) );
        assertThat( result.getHref() ).doesNotContain( "&tag[]", "lastModifiedAt[gt]", ",", "{", "}" );
    }

    @Test
    void shouldGenerateLinksWithoutTemplateButWithVariablesProvided() {
        final Page<TestEntity> page = createPage();
        final var links = PageLinks.of( Controller.class, requestSerializerFactory );
        final Link selfLink = links.self( page )
                .on( ( cursor, controller ) -> controller.getEntities( cursor, 10, null ) );
        final Link nextLink = links.next( page )
                .on( ( cursor, controller ) -> controller.getEntities( cursor, 15, null ) );

        assertThat( selfLink.getHref() ).contains( "?cursor=" );
        assertThat( nextLink.getHref() ).contains( "?cursor=" );
        assertThat( selfLink.getHref() ).contains( "pageSize=10" );
        assertThat( nextLink.getHref() ).contains( "pageSize=15" );
        assertThat( selfLink.getHref() ).doesNotContain( "names[]", "{", "}" );
        assertThat( nextLink.getHref() ).doesNotContain( "names[]", "{", "}" );
    }

    private static Page<TestEntity> createPage() {
        return Page.create( p -> p.content( List.of( new TestEntity( "1", "One" ) ) )
                .self( PageRequest.create(
                        r -> r.asc( Attribute.of( "id", String.class ) ).pageSize( 1 ).totalCount( 2L ) ) )
                .next( PageRequest.create( r -> r.position( Position.create(
                                pos -> pos.attribute( Attribute.of( "id", String.class ) ).value( 1 ).order( Order.ASC ) ) )
                        .pageSize( 1 )
                        .totalCount( 2L ) ) ) //
                .entityType( TestEntity.class ) );
    }
}
