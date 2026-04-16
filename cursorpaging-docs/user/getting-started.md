# Getting Started

## Dependencies

Spring-CursorPaging is split into several Maven modules. Add the ones you need:

### Core: `cursorpaging-jpa`

The repository fragment providing cursor-based paging via JPA.

```xml
<dependency>
  <groupId>io.vigier.cursorpaging</groupId>
  <artifactId>cursorpaging-jpa</artifactId>
  <version>${cursorpaging.version}</version>
</dependency>
```

### API support: `cursorpaging-jpa-api`

Serialization / deserialization of page requests, encryption, and helper classes for REST controllers.

```xml
<dependency>
  <groupId>io.vigier.cursorpaging</groupId>
  <artifactId>cursorpaging-jpa-api</artifactId>
  <version>${cursorpaging.version}</version>
</dependency>
```

### RSQL filtering: `cursorpaging-jpa-rsql`

Allows clients to pass [RSQL / FIQL](https://github.com/jirutka/rsql-parser) filter expressions as query strings.

```xml
<dependency>
  <groupId>io.vigier.cursorpaging</groupId>
  <artifactId>cursorpaging-jpa-rsql</artifactId>
  <version>${cursorpaging.version}</version>
</dependency>
```

Published versions: <https://mvnrepository.com/artifact/io.vigier.cursorpaging>

> The library is also available on GitHub Packages:
> - Repository URL: `https://maven.pkg.github.com/p3t/spring-cursorpaging`
> - You need a personal-access token (classic) to read from any GitHub Packages
    repo — [see documentation](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages).

---

## Generate the JPA Metamodel

The library is easiest to use when the JPA metamodel is generated, so you can reference entity attributes in a type-safe
way (e.g. `DataRecord_.id`).

### Maven

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
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
```

### Gradle

```kotlin
dependencies {
    annotationProcessor("org.hibernate:hibernate-jpamodelgen:${hibernateVersion}")
}
```

### Without the JPA Metamodel

You can define attributes manually as name/type combinations:

```java
void example() {
    Attribute.of( "id", UUID.class );
    Attribute.of( "auditInfo", AuditInfo.class, "createdAt", Instant.class );
}
```

Lombok's `@FieldNameConstants` can help to avoid hard-coded field name strings.

---

## Register the `CursorPageRepositoryFactoryBean`

A custom `JpaRepositoryFactoryBean` is required so that Spring Data picks up the `CursorPageRepository` fragment.
Register it via `@EnableJpaRepositories`:

```java
@Configuration
@EnableJpaRepositories(
        basePackageClasses = SomeRepositoryClassHere.class,
        repositoryFactoryBeanClass = CursorPageRepositoryFactoryBean.class )
public class JpaConfig {
}
```

> **Tip:** Placing the annotation on a dedicated `@Configuration` class avoids side-effects in `@SpringBootTest`s that
> don't need an `EntityManagerFactory`.

---

## Define a Repository Interface

Extend both `JpaRepository` and `CursorPageRepository`:

```java
@Repository
public interface DataRecordRepository
        extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {
}
```

`CursorPageRepository<T>` provides two methods:

| Method                             | Description                                |
|------------------------------------|--------------------------------------------|
| `Page<T> loadPage(PageRequest<T>)` | Load a page of data.                       |
| `long count(PageRequest<T>)`       | Count matching records (respects filters). |

---

Next: [Querying Pages](querying-pages.md)

