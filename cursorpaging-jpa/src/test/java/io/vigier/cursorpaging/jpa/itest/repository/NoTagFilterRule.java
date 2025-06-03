package io.vigier.cursorpaging.jpa.itest.repository;

import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor( staticName = "of" )
public class NoTagFilterRule implements FilterRule {
    public static final String NAME = "TAG_FILTER";
    private final List<String> missingTagNames;

    /**
     * query in SQL somehow like this:
     * <pre>
     * SELECT ds.id,
     *      ( SELECT 1 from tag t
     *          JOIN datarecord_tag dt on dt.tag_id = t.id
     *         WHERE dt.datarecord_id = ds.is
     *         GROUP BY dt.datarecord_id
     *      ) AS has_tag
     *   FROM datarecord r order by r.lastmodifiedat
     *  WHERE has_tag is null
     * </pre>
     *
     * @param cqb criteria query builder
     * @return predicate to be added to filter results
     */
    @Override
    public Predicate toPredicate( final QueryBuilder cqb ) {
        final var cb = cqb.cb();
        final Root<DataRecord> root = cqb.root();

        if ( missingTagNames == null || missingTagNames.isEmpty() ) {
            return cb.isEmpty( root.get( DataRecord_.tags ) );
        }

        // Create a subquery to find DataRecords that have ANY of the specified tags
        final Subquery<UUID> subquery = cqb.query().subquery( UUID.class );
        final Root<DataRecord> subRoot = subquery.from( DataRecord.class );
        final var joinTags = subRoot.join( DataRecord_.tags );

        // Select IDs of records that have any of the specified tags
        subquery.select( cb.literal( UUID.randomUUID() ) ) //
                .where( cb.equal( subRoot, root ), //
                        joinTags.get( Tag_.name ).in( missingTagNames ) //

                );

        // Main query predicate: NOT EXISTS (this record has any of the specified tags)
        return cb.not( cb.exists( subquery ) );
    }

    @Override
    public String name() {
        return NAME;
    }
}