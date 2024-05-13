package io.vigier.sbcpreleasetest.api.model;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.sbcpreleasetest.model.AuditInfo_;
import io.vigier.sbcpreleasetest.model.DataRecord_;

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
