package io.vigier.cursorpaging.jpa.itest.model;

import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Comparator;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class AuditInfo implements Comparable<AuditInfo>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @CreatedDate
    @Builder.Default
    private final Instant createdAt = Instant.now();

    @CreatedBy
    @Builder.Default
    private final String createdBy = "system"; // IRL this would be taken from Spring's security context

    @LastModifiedDate
    @Builder.Default
    private final Instant modifiedAt = Instant.now();

    @LastModifiedBy
    @Builder.Default
    private final String modifiedBy = "user"; // IRL this would be taken from Spring's security context

    // Only for testing purposes, should be set by PreInsert/PreUpdate listener
    public static AuditInfo create( final Instant createdAt, final Instant modifiedAt ) {
        return create( b -> b.createdAt( createdAt ).modifiedAt( modifiedAt ) );
    }

    // Only for testing purposes, should be set by PreInsert/PreUpdate listener
    public static AuditInfo create( final Consumer<AuditInfoBuilder> c ) {
        final AuditInfoBuilder b = AuditInfo.builder();
        c.accept( b );
        return b.build();
    }

    @Override
    public int compareTo( @NonNull final AuditInfo o ) {
        return Comparator.comparing( AuditInfo::getModifiedAt )
                .thenComparing( AuditInfo::getCreatedAt )
                .thenComparing( AuditInfo::getCreatedBy )
                .thenComparing( AuditInfo::getModifiedBy )
                .compare( this, o );
    }
}
