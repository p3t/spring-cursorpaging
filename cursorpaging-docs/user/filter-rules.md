# Custom Filter Rules

For cases where the built-in `Filter` operators are not sufficient — for example, sub-query based ACL checks or
existence tests — you can implement the `FilterRule` interface and add it to a page request.

## The `FilterRule` Interface

```java
public interface FilterRule extends QueryElement {
    Predicate toPredicate( QueryBuilder cqb );

    // optional: name and parameters for serialization support
    default String name() {
        return this.getClass().getName();
    }

    default Map<String, List<String>> parameters() {
        return Map.of();
    }
}
```

A `FilterRule` has full access to the JPA `CriteriaBuilder` and `Root` via the `QueryBuilder`, so you can create
arbitrarily complex predicates.

## Example: ACL Check

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

// Usage
Page<DataRecord> load() {
    return PageRequest.create( b -> b //
            .asc( DataRecord_.id ) //
            .filter( new AclCheckFilterRule( "user-a", READ ) ) );
}
```

## The `Rules` Utility

The `Rules` class provides pre-built `FilterRule` creators for common patterns.

### Filtering by Collection Existence

```java
void example() {
    // Records without any tags
    FilterRule noTags = Rules.where( DataRecord_.tags ).isEmpty();

    // Records that have at least one tag
    FilterRule hasTags = Rules.where( DataRecord_.tags ).isNotEmpty();

    PageRequest.create( b -> b //
            .asc( DataRecord_.id ) //
            .filter( noTags ) );
}
```

### Named Rules with Parameters

Rules can carry a `name` and `parameters` map, which are serialized together with the page request. On deserialization a
registered `RuleFactory` re-creates the rule from these values (see [Serialization](serialization.md)).

```java
void example() {
    FilterRule rule = Rules.named( "acl-check" )
            .withParameter( "subject", "user-a" )
            .withParameter( "action", List.of( "READ" ) )
            .where( DataRecord_.tags )
            .isEmpty();
}
```

> **Important:** `FilterRule` instances are **not** directly serialized because their implementation is unknown to the
> serializer. You **must** re-add them on each subsequent request, or register a `RuleFactory` with the
`RequestSerializer`.

---

Next: [Serialization & API](serialization.md)

