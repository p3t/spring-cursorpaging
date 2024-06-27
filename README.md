[![Java CI with Gradle](https://github.com/p3t/spring-curserpaging/actions/workflows/build.yml/badge.svg)](https://github.com/p3t/spring-curserpaging/actions/workflows/build.yml)

# Spring Data Support for Cursor based Paging

Library supporting an efficient way for paging with large data sets and avoiding to count all records with each page.

# Introduction
Cursor based paging is an alternative to the page/offset based paging provided by Spring and SQL.
It eliminates the need to provide an offset or a page-number which can cause a lot of load on a database in case of very
large amount of records in a table. It also avoids the often not needed total count query per page.

This is done by defining a "cursor", which identifies the page by one or more unique attributes of the record and by ordering the records (s. detail concept below).

# Considered Requirements

- The implementation should follow the repository concept from spring data.
- Ordering by arbitrary columns should be possible.
- A filtering mechanism should be provided
- Total count of records is not part of the page response and not executed while retrieving the page
- No SQL limit/offset and no DB-cursor should be used
- State is/can send to the client and returned to the server for the next page (stateless behaviour)

# Quickstart / how to use it

Please check also the example/webapp sourcecode and README, as well as the courserpaging-jpa-api/README.md.

## Include the cursorpaging library in you maven pom / build.gradle

There are two dependencies:

1. The repository part, containing the Spring fragment interface and the repository implementation
2. The API part, containing logic about serialization of page requests and some useful classes for API implementation

```xml

<dependencies>
  <dependency>
    <groupId>io.vigier.cursorpaging</groupId>
    <artifactId>cursorpaging-jpa</artifactId>
    <version>${cursorpaging.version}</version>
  </dependency>
  <dependency>
    <groupId>io.vigier.cursorpaging</groupId>
    <artifactId>cursorpaging-jpa-api</artifactId>
    <version>${cursorpaging.version}</version>
  </dependency>
</dependencies>
```

Current published version is `0.8.0-RC1`

Note: The library is also available on gitHub-packages:

- Repository-URL: https://maven.pkg.github.com/p3t/spring-cursorpaging
- You need an personal-access token (classic) to read from any github-package
  repo [see docu](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages)
   - Add the token to your `~/.m2/settings.xml` or `~/.gradle/gradle.properties` file as password
   - username is you github-user

## Generate the JPA meta-model

The cursorpaging library is easier to use, when the JPA metamodel is generated to define the available attributes.
This is done by the `hibernate-jpamodelgen` annotation processor (in case you are using eclipse-link or another ORM
there should be a similar one available.

### Maven configuration

```xml

<project>

  <!-- ... -->
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>org.hibernate.orm</groupId>
          <artifactId>hibernate-jpamodelgen</artifactId>
          <version>${hibernate.version}</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
  <!-- ... -->

</project>
```

### Gradle configuration

```kotlin
dependencies {
    annotationProcessor("org.hibernate:hibernate-jpamodelgen:6.4.4.Final")
}
```

### Not using the JPA metamodel

The definition of the attributes should be possible as name/type combination. It might be a help to use
lombok's `@FieldNameConstants` annotation to get the attribute names as constants. Still the attributes type information
has to be added manually.

Currently, this has not really been tested, so there might be places which need adoption to support this fully.

## Register the CursorPageRepositoryFactoryBean

In order to use the repository interface an modified `JpaRepFactoryBean` is needed.
This is done via `@EnableJpaRepositories` annotation in the Spring Boot Application class,
or (maybe better due to less side-effects for `@SpringBootTest`s) on an extra configuration
class (annotated with `@Configuration`)

```java
@SpringBootApplication
@EnableJpaRepositories( repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class TestApplication {

    public static void main( String[] args ) {
        SpringApplication.run( TestApplication.class, args );
    }
}
```

The implementation checks, whether there is a fragment interface of the repository to be instantiated is
a `CursorPageRepository` and if so, it will create a `CursorPageRepositoryImpl` and add it to the repository fragments. Then it delegates to the default implementation.

An alternative, which works without an extra factory implementation, would be to derive for each entity an additional repository-interface and create a repository-impl aside, passing the required arguments (entity class & entity-manager) to the constructor of the `CursorPageRepositoryImpl`.

## Define a repository interface

As used with spring-data, create a repository for each of the data-entities:

```java

@Repository
public interface DataRecordRepository
        extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {

}
```

## Query data / build page requests

From your service you can now use the JPA Metamodel in order to define which properties are relevant for the positions
and how the results should be ordered.

### Example: Use id, order records ascending

There are various shortcut APIs available to make the creation of the requests as easy as possible

```java
public void queryData() {
    final PageRequest<DataRecord> request = PageRequest.firstAsc( DataRecord_.id );
    final Page<DataRecord> page = dataRecordRepository.findPage( request );
    page.forEach( System.out::println );

    // get the next page:
    final var nextPage = dataRecordRepository.loadPage( firstPage.next().orElseThrow() );
    nextPage.forEach( System.out::println );
}
```

Each returned page contains a "next-request" which can be used to query the following records.
The next request will be absent, in case there are no more records available.
The page-size is the same as the one used for the first request, but could be adjusted:

```java 
public void queryData() {
    // ...
    final var next = firstPage.next();
    assertThat( next ).isPresent();
    final var nextPage = dataRecordRepository.loadPage( next.get().withPageSize( 20 ) );
}
```

### Example: Multiple order defintions, sort by embedded entity attributes

```java
public void queryData() {
    final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.modifiedAt ) )
            .asc( DataRecord_.id ) );

    final Page<DataRecord> page = dataRecordRepository.findPage( request );
    page.forEach( System.out::println );
}
```

The first given attribute is the primary sort order, the second the secondary and so on.

*Important*: The combination of all attributes defined in the request must uniquely identify one single entity.
Otherwise you will get unexpected results! Most easy is to use the primary key of the entity (at least as secondary
attribute, if you want to get the records ordered e.g. by a name or creation data)

### Example: Use a filter

```java
public void queryData() {
    final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( DataRecord_.id )
            .filter( Filter.attributeIs( DataRecord_.name, "Alpha" ) ) );

    final Page<DataRecord> page = dataRecordRepository.findPage( request );
    page.forEach( System.out::println );
}
```

This will only return `DataRecords` with name "Alpha". It is possible to add multiple filters for different attributes or to provide multiple values for one attribute (one must match).

### There is no total count in the Page...

Executing a count operation can be a quite expensive operation! Therefore, the total count of records is not part of the page response. It is also usually not required to re-count all records with each page request!
So, if you need to know the total count of records, you can execute a count query on the repository:

```java

@Cacheable( "datarecord.count" )
public long queryCount( PageRequest<DataRecord> request ) {
  final long count = dataRecordRepository.count( request );
  System.out.println( "Total records: " + count );
  return count;
}
```

The request (if available) should be passed as it might contain filters which reduce returned count.

## Using the page request in a controller

In order to keep the server stateless, the information within a `PageRequest` have to be passed to the client and send
back to the server, to get the next page.
In order to do that, a serializer implementation is provided in the "API" package together with some other useful
classes.

### Configuration

Each entity needs it's own serializer, there is a factory simplifying the creation within a Spring config-class:

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

1. `encrypterSecret`: The serializer does also encryption in order to avoid unwanted insights to the implementation and
   code injection attacks. If no secret is provided, a random one is generated. The secret must be the same for all
   instances of the service, in case it is running behind a load-balancer.
2. `entitySerializerFactory`: The factory is used to create the entity-specific serializer. Actually it just passes the
   common parts to the specific serializers.
3. `dataRecordEntitySerializer`: The serializer for a `DataRecord` entity. This is used to serialize the page request
   and to deserialize it back.

### Controller

```java
public class DataRecordController {

  public static final String PATH = "/api/v1/datarecord";
  public static final String COUNT = "/count";

  private final DataRecordRepository dataRecordRepository;
  private final DtoDataRecordMapper dtoDataRecordMapper;
  private final EntitySerializer<DataRecord> serializer;

  @GetMapping( produces = MediaType.APPLICATION_JSON_VALUE )
  @ResponseStatus( HttpStatus.OK )
  public CollectionModel<DtoDataRecord> getDataRecordPage( //
          @RequestParam @MaxSize( 20 ) final Optional<Integer> pageSize,
          @RequestParam( "cursor" ) final Optional<Base64String> cursor ) {

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
    return linkTo( methodOn( DataRecordController.class ).getDataRecordPage( pageSize,
            Optional.of( serializer.toBase64( request ) ) ) ).withRel( rel ).expand();
  }
}
```

The serializer is used:

1. To get the requested page (`PageRequest<DataRecord> request = cursor.map( serializer::toPageRequest )`)
2. To provide links on the current and the next
   page (`linkTo( methodOn( DataRecordController.class ).getDataRecordPage( pageSize, Optional.of( serializer.toBase64( request ) ) ) )`)

The serializer is returning a `Base64String` in order to use this directly in the API a converter needs to be
configured:

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

The annotation: `@MaxSize(20)` is a shortcut for ` @RequestParam final Optional<@Min(1) @Max(20) Integer> pageSize`.

### Serializer Details

The serializer "learns" about the entity attributes by serializing them.
There might be situations where it could be usefule to pre-configure the attributes used to filter and order the
records.

# Background: Concept description
## Basic idea
A Cursor is nothing elsa than a position in a list of records.
The content of the page is just the next n-records after the cursor.
It is important, that the records do have a well defined order, to make this return predictable results.

Databases are very fast, when querying indexed columns or fields. 
Most DBs do have something like an ID/Primary Key (PK) to uniquely identify a record, still a cursor must not be restricted to only use PKs.

Assuming we use a numeric PK a query for the first page could look like this:
```sql
-- @formatter:off
SELECT * FROM some_table WHERE id > 0 ORDER BY id ASC LIMIT 10
```  
The next cursor is the last id of the result set. Page 2 would look like this:
```sql
-- @formatter:off
SELECT * FROM some_table WHERE id > 10 ORDER BY id ASC LIMIT 10                              
```
and so on. 

In real life this is a little more complicated as the desired order of the records depends on the use case (could e.g. creation time or a status-field), and there is no gurantee that this order doesn't change from query to query.

## Design model(s)
![Basic concept of cursor/positions and pages](media/basic-concept.png "Basic concept of cursor/positions and pages")

# Making things more complicate
Potentially, a curosr can be reversed, meaning  the query direction can be changed. This would add the feature to the cursor page to not only point to the next page but also to the previous result-set. Still - this must not be misunderstood as the privous page! This is not easily possible, because for doing this it would be needed to have all previous pages still in memory or somehow stored with the cursor.
Such an reversed cursor is, when used, changing the direction of the query.

![Reversed cursor](media/reversed-cursor.png "Reversed cursor")

There is only value in this feature, if the client is not able to cache the previous requested pages by himself, i.e. forgets them while moving forward. This might be the case in a very memory limited client scenario which is usually _not_ a web application/web browser.

## Limitations
- Such a cursor implementation is not transaction-safe. Which is good enough for most UIs and it is not so important, to miss a record or have a duplicate one in two page requests. This is i.e. the case when the PK is not an ascending numerical ID but maybe an UUID, so that it is possible that an inserted record apears before the page which a client is going to request. In case you need transaction-safe cursor queries, this is most likely a server-side use case and you can use DB-cursors.


