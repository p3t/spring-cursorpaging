package io.vigier.cursorpaging.jpa.rsql.filter;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import io.vigier.cursorpaging.jpa.QueryElement;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.ManagedType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Factory for creating a filter from an RSQL expression. Note: Not all operations might be supported.
 *
 * @see <a href="https://github.com/jirutka/rsql-parser">rsql-parser</a>
 */
@Service
@RequiredArgsConstructor
public final class RsqlFilterFactory<T> {

    private final EntityManager entityManager;
    private final ManagedType<T> rootType;

    private static final RSQLParser PARSER = new RSQLParser();

    public QueryElement toFilter( final String rsql ) {
        try {
            return PARSER.parse( rsql )
                    .accept( new RsqlFilterVisitor(
                            new JpaMetamodelAttributeResolver( entityManager.getMetamodel(), rootType ) ) );
        } catch ( final RSQLParserException e ) {
            throw new RsqlSyntaxException( "Invalid RSQL expression: " + rsql, e );
        }
    }
}
