package io.vigier.cursorpaging.jpa.itest.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToOne( cascade = CascadeType.DETACH )
    @JoinColumn( name = "integrityclass_id",
            referencedColumnName = "level",
            foreignKey = @ForeignKey( name = "fk_datarecord_integityclass_id" ) )
    private SecurityClass integrityClass;

    @ManyToMany( cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER )
    @JoinTable( name = "datarecord_tag",
            joinColumns = @JoinColumn( name = "datarecord_id", referencedColumnName = "id" ),
            inverseJoinColumns = @JoinColumn( name = "tag_id", referencedColumnName = "id" ) )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();


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
