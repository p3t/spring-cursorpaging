[![[ BUILD ] Build Project (Java/Maven)](https://github.com/p3t/spring-cursorpaging/actions/workflows/maven_build.yml/badge.svg)](https://github.com/p3t/spring-cursorpaging/actions/workflows/maven_build.yml) ![GitHub License](https://img.shields.io/github/license/p3t/spring-cursorpaging) ![GitHub Release](https://img.shields.io/github/v/release/p3t/spring-cursorpaging?include_prereleases)

# Spring Data Repository Support for Cursor-Based Paging

_Spring-CursorPaging_ is a library that provides efficient cursor-based paging for Spring Data JPA repositories,
avoiding the performance pitfalls of traditional offset/page-number paging.

## Key Features

- **Cursor-based page traversal** via JPA — no `LIMIT/OFFSET`
- **Filtering** by arbitrary attributes: `equals`, `like`, `in`, `greaterThan`, `lessThan`, `greaterThanOrEqualTo`,
  `lessThanOrEqualTo`
- **AND / OR filter composition** and **ignore-case** support
- **Multi-attribute sort order** including embedded / related entity attributes
- **Custom filter rules** using the JPA Criteria API (e.g. ACLs, existence checks)
- **Total count** on demand with caching support
- **Reversed cursors** to traverse backwards
- **Serialization & encryption** of page requests for stateless client-server communication
- **RSQL / FIQL** filter syntax for REST APIs
- **JPA Metamodel-based attribute resolution** for reliable deserialization

## Concept

Cursor-based paging replaces page/offset with a **cursor position** — one or more indexed column values that uniquely
identify the start of a page. Instead of `OFFSET 10000 LIMIT 10`, the query becomes:

```sql
-- @formatter:off
SELECT *
  FROM records r
 WHERE r.id > :cursor
 ORDER BY r.id LIMIT :pageSize
```

The database can use an index to jump directly to the cursor position, making page access time constant regardless of
how deep into the data set you are.

See [Concept Details](cursorpaging-docs/user/concept.md) for the full background.

![Basic concept](cursorpaging-docs/media/basic-concept.png)

## Quickstart

### 1. Add Dependencies

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

Published versions: <https://mvnrepository.com/artifact/io.vigier.cursorpaging>

### 2. Register the Repository Factory Bean

```java

@Configuration
@EnableJpaRepositories( basePackageClasses = MyApplication.class,
        repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class JpaConfig {
}
```

### 3. Create a Repository

```java

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {
}
```

### 4. Query a Page

```java
void example() {
  PageRequest<DataRecord> request = PageRequest.create(
          b -> b.pageSize( 10 ).asc( DataRecord_.name ).asc( DataRecord_.id ) );
  
  Page<DataRecord> page = dataRecordRepository.loadPage( request );
  page.forEach( System.out::println );
  
  // Next page
  dataRecordRepository.loadPage( page.next( ).orElseThrow() );
}
```

## Modules

| Module                      | Description                                                                                  |
|-----------------------------|----------------------------------------------------------------------------------------------|
| **`cursorpaging-jpa`**      | Core library: repository fragment, `PageRequest`, `Page`, `Filter`, `FilterRule`, `Rules`    |
| **`cursorpaging-jpa-api`**  | API support: `RequestSerializer`, `Encrypter`, `DtoPageRequest`, `PageLinks`, `Base64String` |
| **`cursorpaging-jpa-rsql`** | RSQL/FIQL filter creation via `RsqlFilterFactory`                                            |

## Documentation

| Topic                 | Link                                                           |
|-----------------------|----------------------------------------------------------------|
| Getting Started       | [cursorpaging-docs/user/getting-started.md](cursorpaging-docs/user/getting-started.md)               |
| Querying Pages        | [cursorpaging-docs/user/querying-pages.md](cursorpaging-docs/user/querying-pages.md)                |
| Filtering             | [cursorpaging-docs/user/filtering.md](cursorpaging-docs/user/filtering.md)                     |
| Custom Filter Rules   | [cursorpaging-docs/user/filter-rules.md](cursorpaging-docs/user/filter-rules.md)                  |
| Total Count           | [cursorpaging-docs/user/total-count.md](cursorpaging-docs/user/total-count.md)                   |
| Reversing Pages       | [cursorpaging-docs/user/reversing.md](cursorpaging-docs/user/reversing.md)                     |
| Serialization & API   | [cursorpaging-docs/user/serialization.md](cursorpaging-docs/user/serialization.md)                 |
| RSQL / FIQL Filtering | [cursorpaging-docs/user/rsql.md](cursorpaging-docs/user/rsql.md)                          |
| Concept / Background  | [cursorpaging-docs/user/concept.md](cursorpaging-docs/user/concept.md)                       |

## Example Application

A complete Spring Boot example is available under [
`cursorpaging-examples/webapp-with-maven`](cursorpaging-examples/webapp-with-maven).

## License

See [LICENSE](LICENSE).
