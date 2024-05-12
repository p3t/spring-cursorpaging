# API layer support

This project contains classes to support implementation of an API layer.
I.e. for transferring the cursor (state) to the client and back, as well as defining a initial page request.

* [EntitySerializer](src/main/java/io/vigier/cursorpaging/api/EntitySerializer.java): Supports translation of an
  PageRequest into a binary/base64 representation and back, including encryption.
    * An entity serializer must be created per entity type, in order to simplify this there is an
      EntitySerializerFactory available.
* [DtoPageRequest](src/main/java/io/vigier/cursorpaging/api/DtoPageRequest.java): Represents a initial page request from
  the client, can be translated into a PageRequest.
* [StringToBase64Converter](src/main/java/io/vigier/cursorpaging/api/StringToBase64Converter.java): Converter for Spring
  to use the serializer result directly as controller method parameter.

Have a look in the test-app controller in order to see how to use these classes.

# Examples

## Usage of EntitySerializer

```java

@Configuration
public class EntitySerializerConfig {

    @Value( "${cursorpaging.jpa.serializer.encrypter.secret:1234567890ABCDEFGHIJKlmnopqrst--}" )
    private String encrypterSecret;

    @Bean
    public EntitySerializerFactory entitySerializerFactory( final ConversionService conversionService ) {
        return EntitySerializerFactory.builder()
                .conversionService( conversionService )
                .encrypter( Encrypter.getInstance( encrypterSecret ) )
                .build();
    }

    @Bean
    public EntitySerializer<DataRecord> dataRecordEntitySerializer( final EntitySerializerFactory serializerFactory ) {
        return serializerFactory.forEntity( DataRecord.class );
    }
}
```

Within the controller

```java
public class DataRecordController {

    // ...
    private final DataRecordRepository dataRecordRepository;
    private final DtoDataRecordMapper dtoDataRecordMapper;
    private final EntitySerializer<DataRecord> serializer;
    
    @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.OK )
    public CollectionModel<DtoDataRecord> getDataRecordPage( //
            @RequestParam final Optional<Base64String> cursor ) {

        cursor.ifPresent( c -> log.debug( "Cursor = {}", c ) );

        final PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.create( b -> b.asc( DataRecord_.name ).asc( DataRecord_.id ) ) )
                .withPageSize( 10 );

        final var page = dataRecordRepository.loadPage( request );
//...
    }
}
```