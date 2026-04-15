# RSQL / FIQL Filtering (`cursorpaging-jpa-rsql`)

The `cursorpaging-jpa-rsql` module lets clients pass filter expressions as compact, URL-friendly [RSQL / FIQL](https://github.com/jirutka/rsql-parser) query strings.

## Dependency

```xml
<dependency>
  <groupId>io.vigier.cursorpaging</groupId>
  <artifactId>cursorpaging-jpa-rsql</artifactId>
  <version>${cursorpaging.version}</version>
</dependency>
```

## Setup

Create an `RsqlFilterFactory` bean for each entity. It needs an `EntityManager` so that attribute types (including embedded/related paths) are resolved automatically via the JPA metamodel:

```java
@Configuration
public class RsqlConfig {

    @Bean
    public RsqlFilterFactory<DataRecord> dataRecordRsqlFilterFactory( final EntityManager entityManager ) {
        return new RsqlFilterFactory<>( entityManager,
                entityManager.getMetamodel().entity( DataRecord.class ) );
    }
}
```

## Using RSQL Filters in a Controller

Accept the RSQL expression as a query parameter and convert it with the factory:

```java
@RestController
@RequestMapping( "/api/v1/datarecord" )
@RequiredArgsConstructor
public class DataRecordController {

    private final DataRecordRepository dataRecordRepository;
    private final RequestSerializer<DataRecord> serializer;
    private final RsqlFilterFactory<DataRecord> rsqlFilterFactory;

    @GetMapping( path = "/rsql", produces = MediaType.APPLICATION_JSON_VALUE )
    public CollectionModel<DtoDataRecord> queryDataRecords(
            @RequestParam( "q" ) final String rsqlQuery,
            @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @RequestParam( value = "sort", required = false ) final Optional<List<String>> sort,
            @RequestParam( value = "cursor", required = false ) final Optional<Base64String> cursor ) {

        var filter = rsqlFilterFactory.toFilter( rsqlQuery );

        PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.<DataRecord>builder()
                        .filter( filter )
                        .apply( b -> applySort( sort.orElse( List.of() ), b ) )
                        .build() )
                .withPageSize( pageSize.orElse( 10 ) );

        var page = dataRecordRepository.loadPage( request );
        // … build response with self/next links
    }
}
```

## RSQL Syntax Quick Reference

| Operator | Meaning                  | Example                                      |
|----------|--------------------------|----------------------------------------------|
| `==`     | equal                    | `name==John`                                 |
| `=in=`   | in (multi-value)         | `name=in=(Alice,Bob,Charlie)`                |
| `=gt=`   | greater than             | `age=gt=30`                                  |
| `=ge=`   | greater than or equal to | `age=ge=18`                                  |
| `=lt=`   | less than                | `age=lt=50`                                  |
| `=le=`   | less than or equal to    | `age=le=65`                                  |
| `;`      | AND                      | `name==John;age=gt=25`                       |
| `,`      | OR                       | `name==Alice,name==Bob`                      |
| `()`     | grouping                 | `(name==Alice,name==Bob);age=gt=20`          |

### Dotted Paths

Paths are resolved through the JPA metamodel, e.g.:

```
auditInfo.createdAt=gt=2024-01-01T00:00:00Z
```

---

Back: [Filtering](filtering.md) · [Serialization & API](serialization.md)

