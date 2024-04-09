package io.vigier.cursor.testapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
public class DataRecord {

    @EqualsAndHashCode.Include
    @Builder.Default
    @Id
    private UUID id = UUID.randomUUID();

    @Column( name = "name" )
    private String name;

    @Builder.Default
    @Column( name = "createdat" )
    private Instant createdAt = Instant.now();

}
