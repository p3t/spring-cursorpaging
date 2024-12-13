package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

public interface FilterOperation {

    Predicate apply( final QueryBuilder qb, final Attribute attribute, final List<? extends Comparable<?>> values );
}
