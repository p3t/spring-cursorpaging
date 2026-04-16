# Serialization & API (`cursorpaging-jpa-api`)

To keep the server stateless, the cursor (i.e. `PageRequest`) must be serialized, sent to the client, and deserialized when the client requests the next page.

The `cursorpaging-jpa-api` module provides:

| Class | Purpose |
|-------|---------|
| `RequestSerializer<E>` | Serializes / deserializes a `PageRequest` to/from encrypted bytes or Base64 |
| `RequestSerializerFactory` | Creates and caches entity-specific serializers with shared config |
| `Encrypter` | Symmetric encryption (ChaCha20-Poly1305) to prevent information disclosure |
| `Base64String` | Value class wrapping an encoded cursor string |
| `StringToBase64StringConverter` | Spring `Converter` so `Base64String` can be used as a `@RequestParam` |
| `DtoPageRequest` | JSON-friendly DTO for initial page requests via POST |
| `PageLinks<T>` | HATEOAS link builder for page self/next links |

---

## Configuration

### 1. `RequestSerializerFactory` Bean

```java
@Configuration
public class RequestSerializerConfig {

    @Value( "${cursorpaging.jpa.serializer.encrypter.secret:1234567890ABCDEFGHIJKlmnopqrst--}" )
    private String encrypterSecret;

    @Bean
    public RequestSerializerFactory requestSerializerFactory(
            final ConversionService conversionService,
            final EntityManager entityManager ) {
        return RequestSerializerFactory.builder()
                .conversionService( conversionService )
                .encrypter( Encrypter.getInstance( encrypterSecret ) )
                .entityManager( entityManager )
                .build();
    }

    @Bean
    public RequestSerializer<DataRecord> dataRecordRequestSerializer(
            final RequestSerializerFactory serializerFactory ) {
        return serializerFactory.forEntity( DataRecord.class );
    }
}
```

**Key points:**

- **`encrypterSecret`** — Must be the same across all instances behind a load balancer. If not specified a random key is generated (single-instance only).
- **`entityManager`** — When provided, the factory automatically configures a JPA Metamodel-based `AttributeResolver` for each serializer. This enables reliable deserialization across service instances **without** needing to pre-register attributes via `.use()`.

### 2. `Base64String` Converter

Register the converter so Spring can bind cursor query parameters directly:

```java
@Configuration
@EnableHypermediaSupport( type = { EnableHypermediaSupport.HypermediaType.HAL } )
public class WebConfig {

    @Bean
    public StringToBase64StringConverter stringToBase64StringConverter() {
        return new StringToBase64StringConverter();
    }
}
```

---

## Using the Serializer in a Controller

```java
@RestController
@RequestMapping( "/api/v1/datarecord" )
@RequiredArgsConstructor
public class DataRecordController {

    private final DataRecordRepository dataRecordRepository;
    private final DtoDataRecordMapper dtoDataRecordMapper;
    private final RequestSerializer<DataRecord> serializer;

    @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE )
    public CollectionModel<DtoDataRecord> getPage(
            @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @RequestParam( "cursor" ) final Optional<Base64String> cursor ) {

        // Deserialize cursor or create initial request
        PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.create(
                        b -> b.asc( DataRecord_.name ).asc( DataRecord_.id ) ) )
                .withPageSize( pageSize.orElse( 10 ) );

        Page<DataRecord> page = dataRecordRepository.loadPage( request );

        return CollectionModel.of( page.content( dtoDataRecordMapper::toDto ) )
                .add( getLink( pageSize, page.self(), IanaLinkRelations.SELF ) )
                .addIf( page.next().isPresent(),
                        () -> getLink( pageSize, page.next().orElseThrow(), IanaLinkRelations.NEXT ) );
    }

    private Link getLink( Optional<Integer> pageSize, PageRequest<DataRecord> request,
            LinkRelation rel ) {
        return linkTo( methodOn( DataRecordController.class )
                .getPage( pageSize, Optional.of( serializer.toBase64( request ) ) ) )
                .withRel( rel ).expand();
    }
}
```

The serializer is used in two places:

1. **Deserializing** the incoming cursor: `serializer::toPageRequest`
2. **Serializing** for self/next links: `serializer.toBase64( request )`

---

## `PageLinks` Helper

`PageLinks` simplifies HATEOAS link generation when you use `RequestSerializerFactory`:

```java
void example() {
    PageLinks<DataRecordController> links = PageLinks.of( DataRecordController.class, serializerFactory );

    CollectionModel.of( page.content( mapper::toDto ) )
            .add( links.self( page ).on( DataRecordController::getPage ) )
            .addIf( page.next().isPresent(),
                    () -> links.next( page ).on( DataRecordController::getPage ) );
}
```

---

## Attribute Resolution on Deserialization

When the `RequestSerializerFactory` is created with an `EntityManager`, it automatically provides a `JpaMetamodelAttributeResolver`. This resolver uses the JPA metamodel to look up attribute types for dot-separated paths (e.g. `auditInfo.createdAt`) during deserialization — eliminating the need for manual `.use()` registration.

---

## Serializing `FilterRule`s

`FilterRule` instances are not directly serializable because they can contain arbitrary logic. The serializer supports them through a **name + parameters** mechanism:

1. Implement `name()` and `parameters()` on your `FilterRule`.
2. Register a `RuleFactory` on the serializer:

```java
void example() {
    RequestSerializer.create( DataRecord.class, b -> b
            .filterRuleFactory( "acl-check", params -> new AclCheckFilterRule( params ) ) );
}
```

On serialization the rule's name and parameters are stored; on deserialization the registered factory recreates the rule.

---

## `DtoPageRequest` — JSON Page Request

`DtoPageRequest` is a Jackson-annotated DTO that allows clients to define the initial page request via a POST body:

```json
{
    "orderBy": { "id": "ASC" },
    "filterBy": {
        "AND": [
            { "EQ": { "name": [ "Bravo" ] } },
            { "GT": { "created_at": [ "1999-01-30T10:15:30Z" ] } }
        ]
    },
    "pageSize": 10,
    "withTotalCount": false
}
```

Supported filter types in the DTO: `EQ`, `LIKE`, `GT`, `GE`, `LT`, `LE`, nested via `AND` / `OR`.

Convert to a `PageRequest`:

```java
void example() {
    PageRequest<DataRecord> pageRequest = dtoPageRequest.toPageRequest( DataRecordAttribute::forName );
}
```

---

Next: [RSQL Filtering](rsql.md)

