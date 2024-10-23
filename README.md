[![Java CI with Gradle](https://github.com/p3t/spring-curserpaging/actions/workflows/build.yml/badge.svg)](https://github.com/p3t/spring-curserpaging/actions/workflows/build.yml)

# Spring Data repository support for cursor based paging

_Spring-CursorPaging_ is a library, supporting an efficient way of creating pages of large data sets, by avoiding
performance issues of DBMS and Spring Data build-in paging concepts.

# Introduction
Cursor based paging is an alternative to the page/offset based paging provided by Spring and SQL.
It eliminates the need to provide an offset or a page-number which can cause a lot of load on a database in case of a
large amount of records in a table. It also avoids the often not needed total count query per page.

In order to avoid the DBMS internal fetch of the former records, the page start is defined by a "cursor", created by one
or more unique attributes (ideally they should be indexed columns) and by ordering the records by this attributes (s.
detail concept below). A page can then queried by an expression like:
`select * from records r where r.attribute(s) > cursorposition order by r limit page-size`.

In order to facilitate this paging strategy this library wants to blend into the repository concept of Spring Data,
providing a respective repository-facet interface.

# Features provided by Spring-CursorPaging

- Query of pages of records by cursor position without limit/offset via JPA
- Filtering of records:
    - by arbitrary attributes (equals, like, in, greater-than, less-than)
    - with and/or-conditions
    - with ignore-case (equals, like, in)
- Ordering by multiple attributes
- Custom filter rules based on the JPA criteria API
- On request total-count calculation and caching or the result
- Reversing the page request to get the previous page (content)
- Serialization of the page request for stateless server-client communication
- Encryption of the serialized request to avoid information disclosure

# Quickstart / how to use it

Please check also the example/webapp sourcecode and README, as well as the courserpaging-jpa-api/README.md.

## Include the Spring-CursorPaging library in you maven pom / build.gradle

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

Current published version is available here: https://mvnrepository.com/artifact/io.vigier.cursorpaging

Note: The library is also available on gitHub-packages:

- Repository-URL: https://maven.pkg.github.com/p3t/spring-cursorpaging
- You need a personal-access token (classic) to read from any github-package
  repo [see documentation](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages)
   - Add the token to your `~/.m2/settings.xml` or `~/.gradle/gradle.properties` file as password
   - username is you github-user

## Generate the JPA metamodel

The Spring-CursorPaging library is most easy to use, when the JPA metamodel is generated to define the available
attributes.
This is done by the `hibernate-jpamodelgen` annotation processor (in case you are using eclipse-link or another ORM
there should be a similar one available).

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

The definition of the attributes should be possible as name/type combination. It might be helpful to use
lombok's `@FieldNameConstants` annotation to get the attribute names as constants. Still the attributes type information
has to be added manually.

## Register the `CursorPageRepositoryFactoryBean`

In order to use the repository interface a modified `JpaRepFactoryBean` is needed.
This is done via `@EnableJpaRepositories` annotation in the Spring Boot Application class,
or (maybe better due to fewer side effects for `@SpringBootTest`s) on an extra configuration
class (annotated with `@Configuration`)

```java
/**
 * Moved the {@link EnableJpaRepositories} annotation to a separate configuration class, because otherwise a MockMvcTest
 * would require an entity manager factory (bean).
 */
@Configuration
@EnableJpaRepositories( basePackageClasses = SomeRepositoryClassHere.class,
        repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class JpaConfig {

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
    // Using JPA Metamodel class: DataRecord_
    final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 ).asc( DataRecord_.id ) );
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
    // Not using JPA Metamodel
    final PageRequest<DataRecord> request = PageRequest.create(
            b -> b.pageSize( 10 ).asc( Attribute.of( DataRecord.Fields.id, UUID.class ) ) );

    final var firstPage = dataRecordRepository.loadPage( request );
    final var next = firstPage.next();
    assertThat( next ).isPresent();
    final var nextPage = dataRecordRepository.loadPage( next.get().withPageSize( 20 ) );

    // or simpler:
    final var next = firstPage.next( 20 );
    final var nextPage = dataRecordRepository.loadPage( next.orElseThrow() );
}
```

### Example: Multiple order definitions, sort by embedded entity attributes

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
Otherwise, you will get unexpected results! It is recommended to order by the primary key of the entity (at least as
secondary/last attribute.

### Example: Filter results / Conditions

```java
public void queryData() {
    final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( DataRecord_.id ).filter( attribute( DataRecord_.name ).equalTo( "Alpha" ) ) );
    // Note that `equalTo` and `in` are the same operation, but provided for better readability

    final Page<DataRecord> page = dataRecordRepository.findPage( request );
    page.forEach( System.out::println );
}
```

This will only return `DataRecords` with name "Alpha". It is possible to add multiple filters for different attributes or to provide multiple values for one attribute (one must match).

### Example: Use a filter for searching/ignore-case

By default, a `Filter` is using equals, or when multiple values are provided an `IN`-clause.
In case you want to use a `LIKE`-clause (in case of multiple values, an "or-like" syntax will be used),
you can construct the filter by providing the match-values with the `like`-method:
```java
public void queryData() {
    // Note the `withIgnoreCase` toggle on the attribute
    final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).withIgnoreCase().like( "%r%" ) );
    final PageRequest<DataRecord> request = PageRequest.create(
            b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );
    // `like` does also accept multiple like-expressions, resulting in an like-in operation

    final var firstPage = dataRecordRepository.loadPage( request );

    assertThat( firstPage.getContent() ).allMatch( e -> e.getName().indexOf( 'r' ) > 0 );
}
```

### Filter with greater-than, less-than

```java
public void queryData() {
    final var all = dataRecordRepository.findAllOrderByCreatedAtAsc();
    final var createdAt = Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt );
    final var firstCreatedAt = all.get( 0 ).getAuditInfo().getCreatedAt();

    assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
            .filter( Filter.create( f -> f.attribute( createdAt ).greaterThan( firstCreatedAt ) ) ) ) ) ) //
            .hasSize( all.size() - 1 );
    assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
            .filter( Filter.create( f -> f.attribute( createdAt ).lessThan( firstCreatedAt ) ) ) ) ) ) //
            .isEmpty();
}
```

### `Filters` utility

`Filters` is a class for supporting/simplifying creation of `Filter` instances:

```java
void someExamples() {
    var f1 = Filters.ignoreCase( DataRecord_.tags, Tag_.name ).in( values );
    var f2 = Filters.attribute( DataRecord_.name ).equalTo( "Test" );
    var f3 = Filters.ignoreCase( DataRecord_.name ).like( "ALPH%" );
    var f4 = Filters.ignoreCase( DataRecord_.name ).like( "ALPH%", "BRA%" );

    var redAlpha = Filters.and( Filters.attribute( DataRecord_.name ).equalTo( NAME_ALPHA ),
            Filters.attribute( DataRecord_.tags, Tag_.name ).equalTo( red.getName() ) );
    var greenBravo = Filters.and( Filters.attribute( DataRecord_.name ).equalTo( NAME_BRAVO ),
            Filters.attribute( DataRecord_.tags, Tag_.name ).equalTo( green.getName() ) );
    var request = PageRequest.<DataRecord>create(
            r -> r.desc( DataRecord_.id ).filter( Filters.or( redAlpha, greenBravo ) ) );
}
```

### There is no total count in the Page - by default

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

It is also possible to instruct the repository to do the count for the page request. Still, the total-count will be stored within the returned self & next-page requests, and will not be recalculated for the subsequent requests!

```java
public void queryData() {
    final PageRequest<DataRecord> request = PageRequest.create(
            b -> b.pageSize( 5 ).withTotalCount( true ).asc( DataRecord_.id ) );

    final var page = dataRecordRepository.loadPage( request );
    log( "Total records: " + page.totalCount() );

    // insert some data...

    final var page2 = dataRecordRepository.loadPage( page.next().orElseThrow() );
    log( "Total records: " + page2.totalCount() == page.totalCount() ); // will output true

    // force recalculating the total count
    final var page3 = dataRecordRepository.loadPage( secondPage.next().orElseThrow().withEnableTotalCount( true ) );
    log( "Total records: " + page3.totalCount() ); // new total count
}
```

### Example: Extend filter with custom rules

In some cases you might want to filter the results by defining your own custom rules. One example could be, when you
need to implement sophisticated access rights per record (ACLs).

In order to support this, `cursorpaging-jpa` provides an interface: `FilterRule` which can be used to extend the internally executed criteria query with custom `Predicates`.

```java

@RequiredArgsConstructor
@Builder
private static class AclCheckFilterRule implements FilterRule {

    private final String subject;
    private final AccessEntry.Action action;

    @Override
    public Predicate toPredicate( final QueryBuilder cqb ) {
        final var builder = cqb.cb();
        final var root = cqb.root();
        root.join( DataRecord_.SECURITY_CLASS, JoinType.LEFT );

        final var subquery = cqb.query().subquery( Long.class );
        final var ae = subquery.from( AccessEntry.class );
        subquery.select( builder.max( ae.get( AccessEntry_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ) ) )
                .where( builder.equal( ae.get( AccessEntry_.SUBJECT ), subject ),
                        builder.equal( ae.get( AccessEntry_.ACTION ), action ) );
        return builder.equal( root.get( DataRecord_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ), subquery );
    }
}

@Test
public void shouldUseMoreComplicateFilterRulesForAclChecks() {
    generateData( 100 );
    final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( DataRecord_.id )
            .rule( new AclCheckFilterRule( SUBJECT_READ_STANDARD, READ ) ) );

    final var firstPage = dataRecordRepository.loadPage( request );

    assertThat( firstPage ).isNotNull();
    assertThat( firstPage.getContent() ).allMatch( e -> e.getSecurityClass().getLevel() <= 1 );

    final PageRequest<DataRecord> request2 = PageRequest.create( b -> b.pageSize( 100 )
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( DataRecord_.id )
            .rule( new AclCheckFilterRule( "does not exist", READ ) ) );

    final var shouldBeEmpty = dataRecordRepository.loadPage( request2 );
    assertThat( shouldBeEmpty ).isNotNull();
    assertThat( shouldBeEmpty.getContent() ).isEmpty();
    assertThat( dataRecordRepository.count( request2 ) ).isEqualTo( 0 );
}
```

Important: `FilterRules` will
*not* be serialized to the client, due to their unknown nature, and must be re-added for each subsequent page request!
The `PageRequestSerializer` (s. api-part of the library) has support for serializing "rule-parameters" and a name which
can be used to trigger a call-back on deserializing the page-request.

## Using the page request in a controller

In order to keep the server stateless, the information within a `PageRequest` have to be passed to the client and send
back to the server, to get the next page. In order to do that, a serializer implementation is provided in the "API"
package together with some other useful classes.

### Configuration

Each entity needs its own serializer, there is a factory simplifying the creation within a Spring config-class:

```java
@Configuration
public class RequestSerializerConfig {

    @Value( "${cursorpaging.jpa.serializer.encrypter.secret:1234567890ABCDEFGHIJKlmnopqrst--}" )
    private String encrypterSecret;

    @Bean
    public RequestSerializerFactory requestSerializerFactory( final ConversionService conversionService ) {
        return RequestSerializerFactory.builder()
                .conversionService( conversionService )
                .encrypter( Encrypter.getInstance( encrypterSecret ) )
                .build();
    }

    @Bean
    public RequestSerializer<DataRecord> dataRecordRequestSerializer(
            final RequestSerializerFactory serializerFactory ) {
        return serializerFactory.forEntity( DataRecord.class );
    }
}
```

1. `encrypterSecret`: The serializer does also encryption in order to avoid unwanted insights to the implementation and
   code injection attacks. If no secret is provided, a random one is generated. The secret must be the same for all
   instances of the service, in case it is running behind a load-balancer.
2. `requestSerializerFactory`: The factory is used to create the entity-specific request serializer. Actually it just
   passes the common parts of the configuration to the specific serializers.
3. `dataRecordRequestSerializer`: The serializer for requests of `DataRecord` entities. This bean is used to serialize
   the page request and to deserialize it back.

### Controller

```java
public class DataRecordController {

  public static final String PATH = "/api/v1/datarecord";
  public static final String COUNT = "/count";

  private final DataRecordRepository dataRecordRepository;
  private final DtoDataRecordMapper dtoDataRecordMapper;
    private final RequestSerializer<DataRecord> serializer;

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
There might be situations where it could be useful to pre-configure the attributes used to filter and order the
records.

# Background: Concept description
## Basic idea

A Cursor is nothing else than a position in a list of records.
The content of the page is just the next n-records after the cursor.
It is important, that the records do have a well-defined order, to make this return predictable results.

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

In real life this is a little more complicated as the desired order of the records depends on the use case (could e.g. creation time or a status-field), and there is no guarantee that this order doesn't change in case of record insertion or deletion from query to query.

## Design model(s)
![Basic concept of cursor/positions and pages](media/basic-concept.png "Basic concept of cursor/positions and pages")

# Making things more complicate
Potentially, a cursor can be reversed, meaning  the query direction can be changed. This can be used add the feature to the cursor page to not only point to the next page but also to the previous result-set. Still - this must not be misunderstood as the previous page! This is not easily possible, because for doing this it would require to keep all previous pages in memory or at least keep them somehow stored with the cursor (making the serialized size constantly growing).
Therefor, a reversed cursor is: Changing the direction of the query, but not the sort-order.

![Reversed cursor](media/reversed-cursor.png "Reversed cursor")

There is only value in this feature, if the client is not able to cache the previous requested pages by himself, i.e. forgets them while moving forward. This might be the case in a very memory limited client scenario which is usually _not_ a web application/web browser.

## API for reverting page-requests (=cursor positions)
There is an API (`PageReqiest::toReversed`) for getting a page-request from an existing page-request, which will traverse the results in the opposite direction - while maintaining the sort order.
Example:
```java
        // Extracted from test-case
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 ).asc( DataRecord_.name )
                .asc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        final var secondPage = dataRecordRepository.loadPage( firstPage.next().orElseThrow() );
        final var reversedFirstPage = dataRecordRepository.loadPage( secondPage.self().toReversed() );

        // everything before the second page (self-pointer) is the first page:
        assertThat( reversedFirstPage ).containsExactlyElementsOf( firstPage );
        
        // no next before the "first page":
        assertThat( reversedFirstPage.next() ).isNotPresent(); 
```

## Limitations
- Side-effects (duplicate records in two pages, or missing ones) cannot be avoided, in case records are deleted or inserted.


