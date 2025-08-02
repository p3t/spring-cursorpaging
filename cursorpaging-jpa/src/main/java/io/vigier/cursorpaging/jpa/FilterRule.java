package io.vigier.cursorpaging.jpa;

import java.util.List;
import java.util.Map;


/**
 * A custom rule to filter the query.
 */
public interface FilterRule extends QueryElement {

    @Override
    default List<Attribute> attributes() {
        return List.of();
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    default String name() {
        return this.getClass().getName();
    }

    default Map<String, List<String>> parameters() {
        return Map.of();
    }
}
