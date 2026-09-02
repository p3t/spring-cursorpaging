# RSQL / FIQL Filtering (`cursorpaging-jpa-rsql`)

The `cursorpaging-jpa-rsql` module lets clients pass filter expressions as compact,
URL-friendly [RSQL / FIQL](https://github.com/jirutka/rsql-parser) query strings.

## Dependency

```xml

<dependency>
  <groupId>io.vigier.cursorpaging</groupId>
  <artifactId>cursorpaging-jpa-rsql</artifactId>
  <version>${cursorpaging.version}</version>
</dependency>
```

## Setup

Create an `RsqlFilterFactory` bean for each entity. It needs an `EntityManager` so that attribute types (including
embedded/related paths) are resolved automatically via the JPA metamodel:

```java

@Configuration
public class RsqlConfig {

    @Bean
    public RsqlFilterFactory<DataRecord> dataRecordRsqlFilterFactory( final EntityManager entityManager ) {
        return new RsqlFilterFactory<>( entityManager, entityManager.getMetamodel()
                .entity( DataRecord.class ) );
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
    public CollectionModel<DtoDataRecord> queryDataRecords( @RequestParam( "q" ) final String rsqlQuery,
            @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
            @RequestParam( value = "sort", required = false ) final Optional<List<String>> sort,
            @RequestParam( value = "cursor", required = false ) final Optional<Base64String> cursor ) {

        final var filter = rsqlFilterFactory.toFilter( rsqlQuery );

        PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )
                .orElseGet( () -> PageRequest.<DataRecord>builder()
                        .filter( filter )
                        .apply( b -> applySort( sort.orElse( List.of() ), b ) )
                        .build() )
                .withPageSize( pageSize.orElse( 10 ) );

        final var page = dataRecordRepository.loadPage( request );
        // … build response with self/next links
    }
}
```

## RSQL Syntax Quick Reference

The complete default operator set of `rsql-parser` is supported:

| Operator      | Meaning                  | Example                             |
|---------------|--------------------------|-------------------------------------|
| `==`          | equal                    | `name==John`                        |
| `!=`          | not equal                | `name!=John`                        |
| `=in=`        | in (multi-value)         | `name=in=(Alice,Bob,Charlie)`       |
| `=out=`       | not in (multi-value)     | `name=out=(Alice,Bob)`              |
| `=gt=` / `>`  | greater than             | `age=gt=30`, `age>30`               |
| `=ge=` / `>=` | greater than or equal to | `age=ge=18`, `age>=18`              |
| `=lt=` / `<`  | less than                | `age=lt=50`, `age<50`               |
| `=le=` / `<=` | less than or equal to    | `age=le=65`, `age<=65`              |
| `;`           | AND                      | `name==John;age=gt=25`              |
| `,`           | OR                       | `name==Alice,name==Bob`             |
| `()`          | grouping                 | `(name==Alice,name==Bob);age=gt=20` |

### Wildcards

In the arguments of `==` and `!=` on **string** attributes, `*` acts as a wildcard and is translated into an SQL
`LIKE` / `NOT LIKE` condition:

| Expression      | Resulting condition          |
|-----------------|------------------------------|
| `name==Jo*`     | `name LIKE 'Jo%'`            |
| `name==*ohn*`   | `name LIKE '%ohn%'`          |
| `name!=Jo*`     | `name NOT LIKE 'Jo%'`        |
| `name==Jo\*hn`  | `name = 'Jo*hn'` (escaped)   |

Notes:

- Wildcards are only interpreted for `==` and `!=`, not for `=in=` / `=out=`, and only for `String` attributes.
- `%` and `_` are passed through to the SQL `LIKE` unchanged and therefore keep their SQL wildcard meaning.

### Semantics of the negating operators

`!=` and `=out=` map to the `NOT_EQUAL_TO` filter type, which mirrors the `EQUAL_TO` / `=in=` handling: with a single
value it becomes `<> value`, with several values a `not in (…)` condition. As in SQL, rows where the attribute is
`NULL` do **not** match a negated condition.

### Dotted Paths

Paths are resolved through the JPA metamodel, e.g.:

```
auditInfo.createdAt=gt=2024-01-01T00:00:00Z
```

## Credits

This module uses the open-source [`rsql-parser`](https://github.com/jirutka/rsql-parser)
library by Jakub Jirutka to parse RSQL/FIQL query strings.

---

Back: [Filtering](filtering.md) · [Serialization & API](serialization.md)

