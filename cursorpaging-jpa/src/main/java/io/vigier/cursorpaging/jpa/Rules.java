package io.vigier.cursorpaging.jpa;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.PluralAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A set of rules to be used as {@link FilterRule}.
 */
public class Rules {

    @Getter
    @Setter( AccessLevel.PACKAGE )
    @Accessors( fluent = true )
    public static abstract class RuleCreator<T extends RuleCreator<T>> {
        private Map<String, List<String>> parameters = new HashMap<>();
        private String name;

        public T withParameter( final String key, final List<String> values ) {
            this.parameters.put( key, values );
            return self();
        }

        public T withParameters( final Map<String, List<String>> parameters ) {
            this.parameters.putAll( parameters );
            return self();
        }

        public T name( final String name ) {
            this.name = name;
            return self();
        }

        @SuppressWarnings( "unchecked" )
        private T self() {
            return (T) this;
        }
    }

    @RequiredArgsConstructor
    public static class CollectionRuleCreator extends RuleCreator<CollectionRuleCreator> {
        private final PluralAttribute<?, ?, ?> attribute;

        public FilterRule isEmpty() {
            return new FilterRule() {
                @Override
                public Predicate toPredicate( final QueryBuilder cqb ) {
                    final var subquery = cqb.query().subquery( Long.class );
                    final var subRoot = subquery.from( attribute.getDeclaringType().getJavaType() );
                    final var cb = cqb.cb();
                    subquery.select( cb.literal( 1L ) )
                            .where( cb.equal( subRoot, cqb.root() ),
                                    cb.isNotEmpty( subRoot.get( attribute.getName() ) ) );
                    return cb.not( cb.exists( subquery ) );
                }

                @Override
                public Map<String, List<String>> parameters() {
                    return CollectionRuleCreator.this.parameters();
                }

                @Override
                public String name() {
                    return CollectionRuleCreator.this.name();
                }
            };
        }

        public FilterRule isNotEmpty() {
            return new FilterRule() {
                @Override
                public Predicate toPredicate( final QueryBuilder cqb ) {
                    final var subquery = cqb.query().subquery( Long.class );
                    final var subRoot = subquery.from( attribute.getDeclaringType().getJavaType() );
                    final var cb = cqb.cb();
                    subquery.select( cb.literal( 1L ) )
                            .where( cb.equal( subRoot, cqb.root() ),
                                    cb.isNotEmpty( subRoot.get( attribute.getName() ) ) );
                    return cb.exists( subquery );
                }

                @Override
                public Map<String, List<String>> parameters() {
                    return CollectionRuleCreator.this.parameters();
                }

                @Override
                public String name() {
                    return CollectionRuleCreator.this.name();
                }
            };
        }

        public static CollectionRuleCreator where( final PluralAttribute<?, ?, ?> attribute ) {
            return new CollectionRuleCreator( attribute );
        }
    }

    @NoArgsConstructor
    @Accessors( fluent = true )
    public static class RuleFactory extends RuleCreator<RuleFactory> {
        public CollectionRuleCreator where( final PluralAttribute<?, ?, ?> attribute ) {
            return CollectionRuleCreator.where( attribute ).name( name() ).withParameters( parameters() );
        }
    }

    public static CollectionRuleCreator where( final PluralAttribute<?, ?, ?> attribute ) {
        return new RuleFactory().where( attribute );
    }

    public static RuleFactory withParameter( final String key, final List<String> values ) {
        return new RuleFactory().withParameter( key, values );
    }

    public static RuleFactory withParameter( final String key, final String value ) {
        return new RuleFactory().withParameter( key, List.of( value ) );
    }

    public static RuleFactory named( final String name ) {
        return new RuleFactory().name( name );
    }
}
