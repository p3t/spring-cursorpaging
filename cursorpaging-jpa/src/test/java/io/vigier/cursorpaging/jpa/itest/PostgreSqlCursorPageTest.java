package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursorpaging.jpa.itest.config.JpaConfig;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry_;
import io.vigier.cursorpaging.jpa.itest.model.AuditInfo;
import io.vigier.cursorpaging.jpa.itest.model.AuditInfo_;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord.Fields;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass;
import io.vigier.cursorpaging.jpa.itest.repository.AccessEntryRepository;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.itest.repository.SecurityClassRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.READ;
import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.WRITE;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
@Import( { PostgreSqlTestConfiguration.class, JpaConfig.class } )
class PostgreSqlCursorPageTest {

    private static final String[] NAMES = { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
            "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
            "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu" };
    public static final Attribute DATARECORD_ID = Attribute.of( Fields.id, UUID.class );
    public static final String SUBJECT_READ_STANDARD = "read_standard";
    public static final String SUBJECT_READ_SENSITIVE = "read_sensitive";
    public static final String SUBJECT_WRITE_SENSITIVE = "write_sensitive";

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    private DataRecordRepository dataRecordRepository;
    @Autowired
    private AccessEntryRepository accessEntryRepository;
    @Autowired
    private SecurityClassRepository securityClassRepository;

    @Test
    void contextLoads() {
        final var factoryBeans = applicationContext.getBeansOfType( CursorPageRepositoryFactoryBean.class );
        assertThat( factoryBeans.size() ).isGreaterThanOrEqualTo( 1 ); // I.e. one per repository
    }

    List<DataRecord> generateData( final int count ) {
        securityClassRepository.deleteAll();
        securityClassRepository.flush();
        final SecurityClass cl0 = securityClassRepository.save( SecurityClass.builder().level( 0 ).name( "public" )
                .build() );
        final SecurityClass cl1 = securityClassRepository.save( SecurityClass.builder().level( 1 ).name( "standard" )
                .build() );
        final SecurityClass cl2 = securityClassRepository.save(
                SecurityClass.builder().level( 2 ).name( "confidential" )
                        .build() );
        final SecurityClass[] securityClasses = { cl0, cl1, cl2 };

        dataRecordRepository.deleteAll();
        dataRecordRepository.flush();
        Instant created = Instant.parse( "1999-01-02T10:15:30.00Z" );
        final List<DataRecord> allRecords = new ArrayList<>( count );
        for ( int i = 0; i < count; i++ ) {
            created = created.plus( 1, ChronoUnit.DAYS );
            allRecords.add( dataRecordRepository.save(
                    DataRecord.builder().name( nextName( i ) ).securityClass( securityClasses[i % 3] )
                    .auditInfo( AuditInfo.create( created, created.plus( 10, ChronoUnit.MINUTES ) ) )
                            .build() ) );
        }

        accessEntryRepository.deleteAll();
        accessEntryRepository.flush();
        accessEntryRepository.saveEntry( b -> b.subject( SUBJECT_READ_STANDARD ).action( READ ).securityClass( cl1 ) );
        accessEntryRepository.saveEntry( b -> b.subject( SUBJECT_READ_SENSITIVE ).action( READ ).securityClass( cl2 ) );
        accessEntryRepository.saveEntry(
                b -> b.subject( SUBJECT_WRITE_SENSITIVE ).action( WRITE ).securityClass( cl2 ) );

        log.info( "Generated {} test data-records", dataRecordRepository.count() );
        return allRecords;
    }

    private String nextName( final int i ) {
        return NAMES[i % NAMES.length];
    }

    @AfterEach
    void cleanup() {
        dataRecordRepository.deleteAll();
    }

    @Test
    void shouldFetchFirstPage() {
        generateData( 100 );
        final var all = dataRecordRepository.findAll();

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 ).asc( DATARECORD_ID ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).hasSize( 10 );
        // Result should be sorted by ID...
        final var resultIdList = firstPage.getContent().stream().map( DataRecord::getId ).toList();
        final var allIdsSorted = all.stream()
                .map( DataRecord::getId )
                .sorted( Comparator.comparing( UUID::toString ) )
                .limit( 10 )
                .toList();
        assertThat( resultIdList ).containsExactlyElementsOf( allIdsSorted );
    }

    @Test
    void shouldFetchNextPage() {
        generateData( 30 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 ).asc( DATARECORD_ID ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        assertThat( firstPage ).isNotNull().hasSize( 10 );
        final var next = firstPage.next();
        assertThat( next ).isPresent();
        final var nextPage = dataRecordRepository.loadPage( next.get().withPageSize( 20 ) );
        assertThat( nextPage ).isNotNull().hasSize( 20 );
        assertThat( nextPage ).doesNotContainAnyElementsOf( firstPage );
        assertThat( nextPage.next() ).isEmpty();
    }

    @Test
    void shouldFetchPagesOrderedByCreatedDesc() {
        generateData( 5 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.modifiedAt ) )
                .asc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).hasSize( 5 );

        final var all = dataRecordRepository.findAll()
                .stream()
                .sorted( Comparator.comparing( DataRecord::getAuditInfo )
                        .reversed()
                        .thenComparing( r -> r.getId().toString() ) )
                .toList();

        assertThat( firstPage.getContent() ).containsExactlyElementsOf( all );
        assertThat( firstPage.next() ).isEmpty();
    }

    @Test
    void shouldUseDefaultPageSize() {
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.asc( DataRecord_.id ) );
        assertThat( request.pageSize() ).isEqualTo( PageRequest.DEFAULT_PAGE_SIZE );
    }

    @Test
    void shouldFilterResults() {
        generateData( 100 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.name, "Alpha" ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().equals( "Alpha" ) );
    }

    @Test
    void shouldFilterResultsWithInPredicate() {
        generateData( 100 );
        final Filter nameIsAlphaOrBravo = Filter.create(
                b -> b.attribute( DataRecord_.name ).value( "Alpha" ).value( "Bravo" ) );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id ).filter( nameIsAlphaOrBravo ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch(
                e -> e.getName().equals( "Alpha" ) || e.getName().equals( "Bravo" ) );
    }

    @Test
    void shouldReturnTotalCountWhenNoFilterPresent() {
        generateData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 42 );
    }

    @Test
    void shouldReturnZeroCountWhenNoRecordsMatches() {
        generateData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.name, "name-does-not-exist" ) ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 0 );
    }

    @Builder
    private static class AclFilterRule implements FilterRule {

        private String subject;
        private Action requiredAction;

        @Override
        public Predicate getPredicate( final QueryBuilder cqb ) {
            final var query = cqb.query();
            final var subquery = query.subquery( Integer.class );
            final var subRoot = subquery.from( AccessEntry.class );
            final var builder = cqb.cb();
            final var root = cqb.root();
            root.fetch( DataRecord_.SECURITY_CLASS, JoinType.LEFT );
            subquery.select( builder.literal( 1 ) )
                    .where( builder.equal( subRoot.get( AccessEntry_.SUBJECT ), subject ),  //
                            builder.equal( subRoot.get( AccessEntry_.ACTION ), requiredAction ) );

            return builder.exists( subquery );
        }
    }

    @Test
    void shouldReturnFilteredEntityCountWhenFilterPresent() {
        final List<DataRecord> all = generateData( NAMES.length );
        final long countStandardAccess = all.stream().filter( r -> r.getSecurityClass().getLevel() == 1 ).count();
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .asc( DataRecord_.id )
                .rule( AclFilterRule.builder().subject( SUBJECT_READ_STANDARD ).requiredAction( READ )
                        .build() ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( countStandardAccess );
    }
}


