package io.vigier.sbcpreleasetest.api.model.mapper;

import io.vigier.sbcpreleasetest.api.model.DtoDataRecord;
import io.vigier.sbcpreleasetest.config.MapstructConfig;
import io.vigier.sbcpreleasetest.model.DataRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper( config = MapstructConfig.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR )
public abstract class DtoDataRecordMapper {

    @Mapping( target = "id" )
    public String toId( final UUID id ) {
        return id.toString();
    }

    public OffsetDateTime toOffsetDateTime( final Instant source ) {
        if ( source == null ) {
            return null;
        }
        return source.atOffset( ZoneOffset.UTC );
    }

    @Mapping( target = "createdAt", source = "auditInfo.createdAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
    @Mapping( target = "modifiedAt", source = "auditInfo.modifiedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
    public abstract DtoDataRecord toDto( DataRecord source );
}
