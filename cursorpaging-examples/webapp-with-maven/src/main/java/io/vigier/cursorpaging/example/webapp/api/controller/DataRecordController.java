package io.vigier.cursorpaging.example.webapp.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vigier.cursorpaging.example.webapp.api.model.DataRecordAttribute;
import io.vigier.cursorpaging.example.webapp.api.model.DtoDataRecord;
import io.vigier.cursorpaging.example.webapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursorpaging.example.webapp.model.DataRecord;
import io.vigier.cursorpaging.example.webapp.model.DataRecord_;
import io.vigier.cursorpaging.example.webapp.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest;
import io.vigier.cursorpaging.jpa.serializer.Base64String;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializer;
import io.vigier.cursorpaging.jpa.validation.MaxSize;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RequiredArgsConstructor
@RestController
@Tag( name = "demo-api" )
@RequestMapping( DataRecordController.PATH )
@Slf4j
public class DataRecordController {

    public static final String PATH = "/api/v1/datarecord";
    public static final String COUNT = "/count";

    // Skipping the service layer ;-)
    private final DataRecordRepository dataRecordRepository;
    private final DtoDataRecordMapper dtoDataRecordMapper;
    private final RequestSerializer<DataRecord> serializer;

    @Operation( summary = "Get data records, page by page" )
    @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getDataRecordPage( //
            @Parameter @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @Parameter( description = "Serialized cursor, for requesting the desired page",
                    content = @Content( mediaType = MediaType.TEXT_PLAIN_VALUE ) ) @RequestParam( "cursor" ) final Optional<Base64String> cursor ) {

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

    /**
     * Just an example how a GET request might be mapped to transport all necessary information. Note that the
     * parameters are just there to be visible in swagger - all of them do more or less contain the same content.
     *
     * @param orderBy  order bx
     * @param filterBy filter by
     * @param pageSize page size
     * @param request  all request parameters - shouldn't be visible in swagger
     * @return a page of data records
     */
    @Operation( summary = "Get data records, (first page), specifying all order and filter parameters" )
    @GetMapping( path = "/first", name = "first", produces = "application/json" )
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getDataRecords( //
            @Parameter( style = ParameterStyle.DEEPOBJECT, example = """
                    {
                      "NAME": "ASC",
                      "ID": "ASC"
                    }""" ) @RequestParam final Map<DataRecordAttribute, Order> orderBy,
            @Parameter( style = ParameterStyle.DEEPOBJECT, example = """
                    {
                      "NAME": [
                        "Alpha", "Bravo"
                      ]
                    }""" ) @RequestParam final MultiValueMap<DataRecordAttribute, String> filterBy,
            @Parameter( style = ParameterStyle.DEEPOBJECT ) @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @RequestParam( required = false ) final MultiValueMap<String, String> request ) {
        log.debug( "request = {}, ", request );
        log.debug( "order = {}, ", orderBy );
        log.debug( "filter = {}", filterBy );
        log.debug( "pageSize = {}", pageSize );
        return CollectionModel.of( List.of() );
    }

    @Operation( summary = "Get a cursor on the first page of records" )
    @PostMapping( value = "/page",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.CREATED )
    public RepresentationModel<?> getCursor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody( content = @Content( //
                    mediaType = MediaType.APPLICATION_JSON_VALUE, //
                    schema = @Schema( implementation = DtoPageRequest.class, example = """
                            {
                                "orderBy": {
                                    "id": "ASC"
                                },
                                "filterBy": {
                                    "AND": [
                                        {
                                            "GT": { "beast": ["666"] }
                                        },
                                        {
                                            "EQ": { "color": ["black"] }
                                        }
                                    ]
                                },
                                "pageSize": 10,
                                "withTotalCount": false
                             }""" ) ) ) //
            @RequestBody final DtoPageRequest request ) {
        request.addOrderByIfAbsent( DataRecord_.ID, Order.ASC );
        final PageRequest<DataRecord> pageRequest = request.toPageRequest( DataRecordAttribute::forName );
        return RepresentationModel.of( request )
                .add( getLink( Optional.of( request.getPageSize() ), pageRequest, IanaLinkRelations.FIRST ) );
    }

    @GetMapping( COUNT )
    @ResponseStatus( HttpStatus.OK )
    public RepresentationModel<?> getCount(
            @Parameter( description = "Serialized cursor, for requesting the page and defining filter",
                    content = @Content( mediaType = MediaType.TEXT_PLAIN_VALUE ) ) //
            @RequestParam( "cursor" ) final Optional<Base64String> cursor ) {
        return RepresentationModel.of( Map.of( "totalElements", cursor.map( serializer::toPageRequest )
                        .map( dataRecordRepository::count )
                        .orElseGet( dataRecordRepository::count ) ) ) //
                .add( linkTo( methodOn( DataRecordController.class ).getCount( cursor ) ).withSelfRel() );
    }


    private Link getLink( final Optional<Integer> pageSize, final PageRequest<DataRecord> request,
            final LinkRelation rel ) {
        return linkTo( methodOn( DataRecordController.class ).getDataRecordPage( pageSize,
                Optional.of( serializer.toBase64( request ) ) ) ).withRel( rel ).expand();
    }
}
