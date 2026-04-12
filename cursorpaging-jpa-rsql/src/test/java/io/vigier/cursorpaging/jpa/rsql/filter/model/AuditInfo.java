package io.vigier.cursorpaging.jpa.rsql.filter.model;

import java.time.Instant;

/**
 * Embeddable model class for testing nested/dotted RSQL selectors.
 */
public class AuditInfo {

    private Instant createdAt;
    private Instant modifiedAt;
}

