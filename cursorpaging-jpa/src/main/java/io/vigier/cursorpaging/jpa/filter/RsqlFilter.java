package io.vigier.cursorpaging.jpa.filter;

import cz.jirutka.rsql.parser.RSQLParser;
import io.vigier.cursorpaging.jpa.QueryElement;
import jakarta.persistence.metamodel.Metamodel;

/**
 * Creates a {@link QueryElement} from an RSQL/FIQL expression string.
 * <p>
 * Example usage:
 * <pre>{@code
 *   // simple (all attributes typed as String)
 *   QueryElement filter = RsqlFilter.of( "name==John;age=gt=18" );
 *
 *   // with JPA metamodel lookup (correct types for all attributes)
 *   QueryElement filter = RsqlFilter.of( "auditInfo.modifiedAt=gt=2024-01-01T00:00:00Z",
 *                                        entityManager.getMetamodel(), DataRecord.class );
 * }</pre>
 *
 * @see <a href="https://github.com/jirutka/rsql-parser">rsql-parser</a>
 */
public final class RsqlFilter {

    private static final RSQLParser PARSER = new RSQLParser();

    private RsqlFilter() {
    }

    /**
     * Parses the given RSQL expression and returns the corresponding {@link QueryElement}.
     * <p>
     * All attribute types default to {@link String}. Use {@link #of(String, Metamodel, Class)} or
     * {@link #of(String, AttributeResolver)} when non-string attributes are involved.
     *
     * @param rsql the RSQL/FIQL expression (must not be {@code null})
     * @return the parsed filter as a {@link QueryElement}
     * @throws cz.jirutka.rsql.parser.RSQLParserException if the expression is not valid RSQL
     */
    public static QueryElement of( final String rsql ) {
        return of( rsql, RsqlFilterVisitor.DEFAULT_RESOLVER );
    }

    /**
     * Parses the given RSQL expression, resolving attribute types from the JPA {@link Metamodel}.
     *
     * @param rsql        the RSQL/FIQL expression (must not be {@code null})
     * @param metamodel   the JPA metamodel (typically from {@code entityManager.getMetamodel()})
     * @param entityClass the root entity class to resolve selectors against
     * @return the parsed filter as a {@link QueryElement}
     * @throws cz.jirutka.rsql.parser.RSQLParserException if the expression is not valid RSQL
     */
    public static QueryElement of( final String rsql, final Metamodel metamodel, final Class<?> entityClass ) {
        return of( rsql, JpaMetamodelAttributeResolver.of( metamodel, entityClass ) );
    }

    /**
     * Parses the given RSQL expression using the provided {@link AttributeResolver} to map selectors to typed
     * {@link io.vigier.cursorpaging.jpa.Attribute} instances.
     *
     * @param rsql     the RSQL/FIQL expression (must not be {@code null})
     * @param resolver maps RSQL selectors (e.g. "auditInfo.modifiedAt") to {@link io.vigier.cursorpaging.jpa.Attribute}
     * @return the parsed filter as a {@link QueryElement}
     * @throws cz.jirutka.rsql.parser.RSQLParserException if the expression is not valid RSQL
     */
    public static QueryElement of( final String rsql, final AttributeResolver resolver ) {
        return PARSER.parse( rsql ).accept( new RsqlFilterVisitor( resolver ) );
    }
}
