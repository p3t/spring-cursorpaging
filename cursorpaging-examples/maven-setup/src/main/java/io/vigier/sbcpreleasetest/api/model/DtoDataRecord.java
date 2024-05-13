package io.vigier.sbcpreleasetest.api.model;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class DtoDataRecord {

    private String id;

    private String name;

    private OffsetDateTime createdAt;

    private OffsetDateTime modifiedAt;
}
