package io.vigier.cursorpaging.testapp.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.serializer.Base64String;
import io.vigier.cursorpaging.jpa.serializer.EntitySerializer;
import io.vigier.cursorpaging.jpa.validation.MaxSize;
import io.vigier.cursorpaging.testapp.api.model.DtoDataRecord;
import io.vigier.cursorpaging.testapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursorpaging.testapp.model.DataRecord;
import io.vigier.cursorpaging.testapp.model.DataRecord_;
import io.vigier.cursorpaging.testapp.repository.DataRecordRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag( name = "demo-api" )
@RequestMapping( DataRecordController.PATH )
@Slf4j
public class DataRecordController {

    public static final String PATH = "/api/v1/datarecord";

    // Skipping the service layer ;-)
    private final DataRecordRepository dataRecordRepository;
    private final DtoDataRecordMapper dtoDataRecordMapper;
    private final EntitySerializer<DataRecord> serializer;


    @Operation( summary = "Get data records, page by page" )
    @GetMapping
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getAllDataRecords( //
            @Parameter @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @Parameter @RequestParam( "cursor" ) final Optional<Base64String> cursor ) {

        cursor.ifPresent( c -> log.debug( "Cursor = {}", c ) );

        final PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.create( b -> b.asc( DataRecord_.name ).asc( DataRecord_.id ) ) )
                .withPageSize( pageSize.orElse( 10 ) );

        final var page = dataRecordRepository.loadPage( request );

        return CollectionModel.of( page.content( dtoDataRecordMapper::toDto ) ) //
                .add( getLink( pageSize, page.self(), IanaLinkRelations.SELF ) ) //
                .addIf( page.next().isPresent(),
                        () -> getLink( pageSize, page.next().orElseThrow(), IanaLinkRelations.NEXT ) );
    }

    private Link getLink( final Optional<Integer> pageSize, final PageRequest<DataRecord> request,
            final LinkRelation rel ) {
        return linkTo( methodOn( DataRecordController.class ).getAllDataRecords( pageSize,
                Optional.of( serializer.toBase64( request ) ) ) ).withRel( rel ).expand();
    }
}
