package io.vigier.cursor.testapp.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vigier.cursor.jpa.Page;
import io.vigier.cursor.jpa.PageRequest;
import io.vigier.cursor.jpa.serializer.EntitySerializer;
import io.vigier.cursor.jpa.validation.MaxSize;
import io.vigier.cursor.testapp.api.model.DtoDataRecord;
import io.vigier.cursor.testapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursor.testapp.model.DataRecord;
import io.vigier.cursor.testapp.model.DataRecord_;
import io.vigier.cursor.testapp.repository.DataRecordRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag( name = "demo-api" )
@RequestMapping( DataRecordController.ROOT_PATH )
@Slf4j
public class DataRecordController {

    public static final String ROOT_PATH = "/api/v1/datarecord";

    // Skipping the service layer ;-)
    private final DataRecordRepository dataRecordRepository;
    private final DtoDataRecordMapper dtoDataRecordMapper;
    private final EntitySerializer<DataRecord> serializer;


    @Operation( summary = "Get data records, page by page" )
    @GetMapping
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getAllDataRecords( //
            @Parameter @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @Parameter @RequestParam( "cursor" ) final Optional<String> cursor ) {

        cursor.ifPresent( c -> log.debug( "Cursor = {}", c ) );
        final PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.create( b -> b.asc( DataRecord_.name ).asc( DataRecord_.id ) ) )
                .withPageSize( pageSize.orElse( 10 ) );

        final var page = dataRecordRepository.loadPage( request );
        return CollectionModel.of( page.content( dtoDataRecordMapper::toDto ) ) //
                .add( getSelfRel( pageSize, page ) ) //
                .addIf( page.next().isPresent(), () -> getNextRel( pageSize, page ) );
    }

    private Link getSelfRel( final Optional<Integer> pageSize, final Page<DataRecord> page ) {
        return linkTo( methodOn( DataRecordController.class ).getAllDataRecords( pageSize,
                Optional.of( serializer.toBase64( page.self() ) ) ) ).withSelfRel().expand();
    }

    private Link getNextRel( final Optional<Integer> pageSize, final Page<DataRecord> page ) {
        return linkTo( methodOn( DataRecordController.class ).getAllDataRecords( pageSize,
                Optional.of( serializer.toBase64( page.next().orElseThrow() ) ) ) ).withRel( IanaLinkRelations.NEXT )
                .expand();
    }
}
