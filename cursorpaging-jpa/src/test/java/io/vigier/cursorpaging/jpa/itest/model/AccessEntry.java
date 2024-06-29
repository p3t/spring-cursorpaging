package io.vigier.cursorpaging.jpa.itest.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

/**
 * Entity for demonstrating custom filter rules for cursors. <p>The scenarios should be that the access to the
 * data-records is limited by an access control list (ACL) where each entry defines to which security class access
 * should be allowed</p>
 */
@Entity
@Table( name = "accesscontrollist" )
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode( onlyExplicitlyIncluded = true )
@FieldNameConstants
public class AccessEntry {

    public static enum Action {
        READ, WRITE, DELETE
    }

    @EqualsAndHashCode.Include
    @Builder.Default
    @Id
    private UUID id = UUID.randomUUID();

    /**
     * Subject can be e.g. a user-id, group, OAuth2 scope or clientID etc. Usually extracted from the spring security
     * context
     */
    @Column( name = "subject" )
    private String subject;

    @ManyToOne( cascade = CascadeType.DETACH )
    @JoinColumn( name = "securityclass_id",
            referencedColumnName = "level",
            nullable = false,
            foreignKey = @ForeignKey( name = "fk_accessentry_securityclass_id" ) )
    private SecurityClass securityClass;

    @Column( name = "action" )
    @Enumerated( EnumType.STRING )
    private Action action;
}
