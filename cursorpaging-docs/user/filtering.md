# Filtering

Filters restrict the result set of a page request.
They are applied as additional `WHERE` conditions to the underlying JPA criteria query.

## Simple Equality / In

```java
void example() {
    PageRequest.create( b -> b.asc( DataRecord_.id )
            .filter( Filters.attribute( DataRecord_.name )
                    .equalTo( "Alpha" ) ) );
}
```

When multiple values are given, an `IN` clause is used:

```java
void example() {
    PageRequest.create( b -> b.asc( DataRecord_.id )
            .filter( Filters.attribute( DataRecord_.name )
                    .in( "Alpha", "Bravo" ) ) );
}
```

## Like (Pattern Matching)

```java
void example() {
    Filter nameLike = Filters.attribute( DataRecord_.name )
            .like( "%lpha%" );

    // Multiple patterns result in an OR-like condition
    Filter multiLike = Filters.attribute( DataRecord_.name )
            .like( "Alph%", "Bra%" );
}
```

## Ignore Case

Enable case-insensitive comparison with `ignoreCase(…)` or `withIgnoreCase()`:

```java
void example() {
    Filter f = Filters.ignoreCase( DataRecord_.name )
            .like( "%ALPHA%" );
    // Also works with equalTo / in
    Filter eq = Filters.ignoreCase( DataRecord_.name )
            .equalTo( "alpha" );
}
```

## Comparison Operators

```java
void example() {
    Filters.attribute( createdAt )
            .greaterThan( someInstant );
    Filters.attribute( createdAt )
            .greaterThanOrEqualTo( someInstant );
    Filters.attribute( createdAt )
            .lessThan( someInstant );
    Filters.attribute( createdAt )
            .lessThanOrEqualTo( someInstant );
}
```

## Filtering by Related Entity Attributes

### To-One Relationships

```java
void example() {
    PageRequest.create( b -> b.asc( DataRecord_.id )
            .filter( Filters.attribute( DataRecord_.securityClass, SecurityClass_.level )
                    .equalTo( 0 ) ) );
}
```

### To-Many Relationships

```java
void example() {
    PageRequest.create( b -> b.asc( DataRecord_.id )
            .filter( Filters.attribute( DataRecord_.tags, Tag_.name )
                    .in( "green", "red" ) ) );
}
```

## Combining Filters — AND / OR

Use `Filters.and(…)` and `Filters.or(…)` to combine multiple filters:

```java
void example() {
    var redAlpha = Filters.and( Filters.attribute( DataRecord_.name )
            .equalTo( "Alpha" ), Filters.attribute( DataRecord_.tags, Tag_.name )
            .equalTo( "red" ) );

    var greenBravo = Filters.and( Filters.attribute( DataRecord_.name )
            .equalTo( "Bravo" ), Filters.attribute( DataRecord_.tags, Tag_.name )
            .equalTo( "green" ) );

    PageRequest.create( b -> b.desc( DataRecord_.id )
            .filter( Filters.or( redAlpha, greenBravo ) ) );
}
```

## Adding Filters to an Existing Request

`PageRequest.copy(…)` lets you derive a new request while preserving positions and other settings:

```java
void example() {
    PageRequest<DataRecord> withFilter = request.copy( b -> b.filter( Filters.attribute( DataRecord_.name )
            .equalTo( "Alpha" ) ) );
}
```

## The `Filter.create` Builder

For fine-grained control you can build a `Filter` directly:

```java
void example() {
    Filter f = Filter.create( b -> b.attribute( DataRecord_.name )
            .withIgnoreCase()
            .like( "%r%" ) );
}
```

---

Next: [Custom Filter Rules](filter-rules.md) · [RSQL Filtering](rsql.md)
