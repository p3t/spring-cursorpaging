package io.vigier.cursor.testapp.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.vigier.cursor.testapp.api.model.DtoDataRecord;
import io.vigier.cursor.testapp.api.model.DtoPageRequest;
import java.util.Optional;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag( name = "demo-api" )
@RequestMapping( DataRecordController.ROOT_PATH )
public class DataRecordController {

    public static final String ROOT_PATH = "/api/v1/datarecord";

    @GetMapping
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getAllDataRecords( final Optional<DtoPageRequest> pageRequest ) {
        return null;
    }
}
