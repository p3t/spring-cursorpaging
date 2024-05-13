package io.vigier.cursorpaging.testapp.api.model;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.testapp.model.AuditInfo;
import io.vigier.cursorpaging.testapp.model.AuditInfo_;
import io.vigier.cursorpaging.testapp.model.DataRecord_;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DataRecordAttribute {
    // Not using the "lowercase" properties here, because there are tests where no EntityManager is instantiated
    // (i.e. the MockMVC test), but this one is needed to populate them :-(

    ID( Attribute.of( DataRecord_.NAME, UUID.class ) ),
    NAME( Attribute.of( DataRecord_.NAME, String.class ) ),
    CREATED_AT( Attribute.path( DataRecord_.AUDIT_INFO, AuditInfo.class, AuditInfo_.CREATED_AT, Instant.class ) ),
    MODIFIED_AT( Attribute.path( DataRecord_.AUDIT_INFO, AuditInfo.class, AuditInfo_.MODIFIED_AT, Instant.class ) );

    private final Attribute attribute;

    public static Attribute forName( final String name ) {
        return valueOf( name.toUpperCase() ).getAttribute();
    }
}
