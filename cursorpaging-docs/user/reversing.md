# Reversing Pages (Previous Page)

A cursor can be **reversed** — the query direction changes while the sort order stays the same.
This allows you to fetch the content that comes *before* the current cursor position.

> **Note:** A reversed cursor does not equal "go to the previous page". It simply traverses in the opposite direction from the current position. This is primarily useful in memory-limited client scenarios that cannot cache previously fetched pages.

## API

Use `PageRequest::toReversed()` to create a reversed page request:

```java
void example() {
    PageRequest<DataRecord> request = PageRequest.create( b -> b
            .pageSize( 5 )
            .asc( DataRecord_.name )
            .asc( DataRecord_.id ) );

    Page<DataRecord> firstPage  = dataRecordRepository.loadPage( request );
    Page<DataRecord> secondPage = dataRecordRepository.loadPage( firstPage.next().orElseThrow() );

    // Reverse from the second page's self-pointer
    Page<DataRecord> reversedFirstPage = dataRecordRepository.loadPage(
            secondPage.self().toReversed() );

    // The reversed page contains everything before the second page
    assertThat( reversedFirstPage ).containsExactlyElementsOf( firstPage );

    // No further page before the first page
    assertThat( reversedFirstPage.next() ).isNotPresent();
}
```

![Reversed cursor](../media/reversed-cursor.png "Reversed cursor")

---

Next: [Serialization & API](serialization.md)

