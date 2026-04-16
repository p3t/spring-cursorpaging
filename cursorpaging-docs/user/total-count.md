# Total Count

Executing a `COUNT(*)` can be expensive, so **by default the total count is not included** in a page response.

## Explicit Count Query

Use the repository's `count()` method, optionally passing a request so that its filters are applied:

```java
void example() {
    long total = dataRecordRepository.count( request );
}
```

> **Tip:** Cache the result — there is usually no need to re-count on every page.
>
> ```java
> @Cacheable( "datarecord.count" )
> public long queryCount( PageRequest<DataRecord> request ) {
>     return dataRecordRepository.count( request );
> }
> ```

## Inline Total Count

You can instruct the repository to calculate the total count together with the first page load:

```java
void example() {
    PageRequest<DataRecord> request = PageRequest.create( b -> b
            .pageSize( 5 )
            .withTotalCount( true )
            .asc( DataRecord_.id ) );

    Page<DataRecord> page = dataRecordRepository.loadPage( request );
    Optional<Long> total = page.getTotalCount(); // present
}
```

The count is **stored within the self/next page requests** and is **not recalculated** for subsequent pages:

```java
void example() {
    Page<DataRecord> page2 = dataRecordRepository.loadPage( page.next().orElseThrow() );
    page2.getTotalCount(); // same value as page.getTotalCount()
}
```

### Force Recalculation

```java
void example() {
    PageRequest<DataRecord> recounted = page2.next().orElseThrow().withEnableTotalCount( true );
    Page<DataRecord> page3 = dataRecordRepository.loadPage( recounted );
    page3.getTotalCount(); // freshly calculated
}
```

---

Next: [Reversing Pages](reversing.md)

