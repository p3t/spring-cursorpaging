package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import java.util.List;

public interface QueryElement {

    Predicate toPredicate( QueryBuilder cqb );

    List<Attribute> attributes();

    boolean isEmpty();
}
