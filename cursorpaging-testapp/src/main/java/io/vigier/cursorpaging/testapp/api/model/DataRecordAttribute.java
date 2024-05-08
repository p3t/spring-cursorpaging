package io.vigier.cursorpaging.testapp.api.model;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.testapp.model.AuditInfo_;
import io.vigier.cursorpaging.testapp.model.DataRecord_;

public enum DataRecordAttribute {
    ID( Attribute.of( DataRecord_.id ) ),
    NAME( Attribute.of( DataRecord_.name ) ),
    CREATED_AT( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) ),
    MODIFIED_AT( Attribute.path( DataRecord_.auditInfo, AuditInfo_.modifiedAt ) );
    private final Attribute attribute;

    DataRecordAttribute( final Attribute attribute ) {
        this.attribute = attribute;
    }

    public Attribute getAttribute() {
        return attribute;
    }
}
