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
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

/**
 * A simple data record for testing cursor pagination.
 */
@Entity
@Table( name = "datarecord" )
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode( onlyExplicitlyIncluded = true )
@FieldNameConstants
public class DataRecord {

    @EqualsAndHashCode.Include
    @Builder.Default
    @Id
    private UUID id = UUID.randomUUID();

    @Column( name = "name" )
    private String name;

    @ManyToOne( cascade = CascadeType.DETACH )
    @JoinColumn( name = "securityclass_id",
            referencedColumnName = "level",
            nullable = false,
            foreignKey = @ForeignKey( name = "fk_datarecord_securityclass_id" ) )
    private SecurityClass securityClass;

    @Builder.Default
    private AuditInfo auditInfo = new AuditInfo();

    @Column( name = "status" )
    @Enumerated( EnumType.STRING )
    @Builder.Default
    private Status status = Status.DRAFT;

    public static DataRecord create( final Consumer<DataRecordBuilder> c ) {
        final DataRecordBuilder b = DataRecord.builder();
        c.accept( b );
        return b.build();
    }

}
