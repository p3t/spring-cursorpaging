# Querying Pages

## Creating a Page Request

A `PageRequest` defines the cursor position, sort order, page size, and optional filters.
At least one sort attribute must be specified — it determines the order in which records are traversed.

```java
void example() {
    PageRequest<DataRecord> request = PageRequest.create( b -> b
            .pageSize( 10 )
            .asc( DataRecord_.id ) );
}
```

> **Important:** The combination of all sort attributes must **uniquely identify** a single entity.
> It is recommended to always include the primary key as the last sort attribute.

## Sort Order

Use `.asc(…)` / `.desc(…)` on the builder, or the more explicit `.sort(attribute, order)`:

```java
void example() {
    // Single attribute
    PageRequest.create( b -> b.asc( DataRecord_.name )
            .asc( DataRecord_.id ) );

    // Explicit order enum
    PageRequest.create( b -> b.sort( DataRecord_.name, Order.DESC )
            .sort( DataRecord_.id, Order.ASC ) );
}
```

### Sorting by Embedded / Related Attributes

Use `Attribute.path(…)` for nested properties:

```java
void example() {
    PageRequest.create( b -> b
            .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
            .asc( DataRecord_.id ) );
}
```

### Sorting Without the JPA Metamodel

```java
void example() {
    PageRequest.create( b -> b
            .asc( "name", String.class )
            .asc( "id", UUID.class ) );
}
```

---

## Loading Pages

```java
Page<DataRecord> firstPage = dataRecordRepository.loadPage( request );
firstPage.

forEach( System.out::println );
```

### Navigating Forward

Each `Page` carries an optional **next** request. It is absent when there are no more records.

```java
Optional<PageRequest<DataRecord>> next = firstPage.next();
if(next.

isPresent() ){
Page<DataRecord> secondPage = dataRecordRepository.loadPage( next.get() );
}
```

### Adjusting Page Size

```java
// Change page size for the next request
Page<DataRecord> nextPage = dataRecordRepository.loadPage( firstPage.next()
                .orElseThrow()
                .withPageSize( 20 ) );

// Or use the convenience overload
Optional<PageRequest<DataRecord>> next = firstPage.next( 20 );
```

---

## Mapping Page Content

`Page` provides a helper to map entities inline:

```java
void example() {
    List<DtoDataRecord> dtos = page.content( dtoMapper::toDto );
}
```

---

## `Page` API at a Glance

| Method               | Description                                              |
|----------------------|----------------------------------------------------------|
| `content()`          | Unmodifiable list of entities                            |
| `content(Function)`  | Mapped list of entities                                  |
| `self()`             | `PageRequest` that produced this page                    |
| `next()`             | Optional next `PageRequest`                              |
| `next(int pageSize)` | Next request with a different page size                  |
| `isEmpty()`          | `true` when the page has no content                      |
| `size()`             | Number of elements on this page                          |
| `getTotalCount()`    | Optional total count (see [Total Count](total-count.md)) |

---

Next: [Filtering](filtering.md) · [Total Count](total-count.md) · [Reversing Pages](reversing.md)

