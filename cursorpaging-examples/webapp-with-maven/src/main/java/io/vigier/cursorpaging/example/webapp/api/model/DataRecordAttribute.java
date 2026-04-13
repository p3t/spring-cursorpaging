package io.vigier.cursorpaging.example.webapp.api.model;

import io.vigier.cursorpaging.example.webapp.model.AuditInfo;
import io.vigier.cursorpaging.example.webapp.model.AuditInfo_;
import io.vigier.cursorpaging.example.webapp.model.DataRecord;
import io.vigier.cursorpaging.example.webapp.model.DataRecord_;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest.PageRequestBuilder;
import io.vigier.cursorpaging.jpa.Position;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static io.vigier.cursorpaging.jpa.Order.from;

@RequiredArgsConstructor
@Getter
public enum DataRecordAttribute {
    // Not using the "lowercase" properties here, because there are tests where no EntityManager is instantiated
    // (i.e. the MockMVC test), but this one is needed to populate them :-(

    ID( Attribute.of( DataRecord_.ID, UUID.class ) ),
    NAME( Attribute.of( DataRecord_.NAME, String.class ) ),
    CREATED_AT( Attribute.of( DataRecord_.AUDIT_INFO, AuditInfo.class, AuditInfo_.CREATED_AT, Instant.class ) ),
    MODIFIED_AT( Attribute.of( DataRecord_.AUDIT_INFO, AuditInfo.class, AuditInfo_.MODIFIED_AT, Instant.class ) );

    private final Attribute attribute;

    public static Attribute forName( final String name ) {
        return valueOf( name.toUpperCase() ).getAttribute();
    }

    public static Position sort( final String orderSpec ) {
        final var parts = orderSpec.split( ":" );
        final var attribute = forName( parts[0] );
        final var order = from( parts[1] );
        return Position.create( p -> p.attribute( attribute ).order( order ) );
    }

    public static PageRequestBuilder<DataRecord> applySort( final List<String> orderSpecs,
            final PageRequestBuilder<DataRecord> builder ) {
        if ( orderSpecs == null || orderSpecs.isEmpty() ) {
            builder.desc( MODIFIED_AT.getAttribute() );
        } else {
            orderSpecs.forEach( s -> {
                final var sort = sort( s );
                builder.position( sort );
            } );
        }
        return builder.asc( ID.getAttribute() );
    }
}
