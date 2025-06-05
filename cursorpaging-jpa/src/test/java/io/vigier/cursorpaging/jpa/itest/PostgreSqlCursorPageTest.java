package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.Rules;
import io.vigier.cursorpaging.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursorpaging.jpa.itest.config.JpaConfig;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry_;
import io.vigier.cursorpaging.jpa.itest.model.AuditInfo_;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass_;
import io.vigier.cursorpaging.jpa.itest.model.Status;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import io.vigier.cursorpaging.jpa.itest.repository.AccessEntryRepository;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.itest.repository.NoTagFilterRule;
import io.vigier.cursorpaging.jpa.itest.repository.SecurityClassRepository;
import io.vigier.cursorpaging.jpa.itest.repository.TagRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static io.vigier.cursorpaging.jpa.Filters.attribute;
import static io.vigier.cursorpaging.jpa.itest.TestData.NAMES;
import static io.vigier.cursorpaging.jpa.itest.TestData.NAME_ALPHA;
import static io.vigier.cursorpaging.jpa.itest.TestData.NAME_BRAVO;
import static io.vigier.cursorpaging.jpa.itest.TestData.TAG_RED;
import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.READ;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
@Import( { PostgreSqlTestConfiguration.class, JpaConfig.class } )
class PostgreSqlCursorPageTest {

    public static final String SUBJECT_READ_STANDARD = "read_standard";

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    private DataRecordRepository dataRecordRepository;
    @Autowired
    private AccessEntryRepository accessEntryRepository;
    @Autowired
    private SecurityClassRepository securityClassRepository;
    @Autowired
    private TestDataPersister testDataPersister;
    @Autowired
    private TagRepository tagRepository;


    @Test
    void contextLoads() {
        final var factoryBeans = applicationContext.getBeansOfType( CursorPageRepositoryFactoryBean.class );
        assertThat( factoryBeans ).isNotEmpty(); // I.e. one per repository
    }


    @AfterEach
    @Transactional
    void cleanup() {
        testDataPersister.deleteAll();
    }

    @BeforeEach
    @Transactional
    void beforeEach() {
        testDataPersister.deleteAll();
    }

    private TestData defaultData() {
        return testDataPersister.persist( TestData.create( td -> td.recordCount( 100 ) ) );
    }

    private TestData defaultData( final int count ) {
        return testDataPersister.persist( TestData.create( td -> td.recordCount( count ) ) );
    }

    @Test
    void shouldFetchFirstPage() {
        defaultData();
        final var all = dataRecordRepository.findAll();

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 ).asc( DataRecord_.id ) );

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
        defaultData( 30 );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 10 ).asc( Attribute.of( DataRecord.Fields.id, UUID.class ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        assertThat( firstPage ).isNotNull().hasSize( 10 );

        final var next = firstPage.next();
        assertThat( next ).isPresent();

        final var nextPage = dataRecordRepository.loadPage( next.get().withPageSize( 20 ) );
        assertThat( nextPage ).isNotNull().hasSize( 20 ).doesNotContainAnyElementsOf( firstPage );
        assertThat( nextPage.next() ).isEmpty();
    }

    @Test
    void shouldFetchPagesOrderedByCreatedDesc() {
        defaultData( 15 );

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.modifiedAt ) )
                .asc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        final var secondPage = dataRecordRepository.loadPage( firstPage.next().orElseThrow().withPageSize( 10 ) );

        assertThat( firstPage ).isNotNull();
        assertThat( secondPage ).isNotNull();

        final var all = dataRecordRepository.findAll()
                .stream()
                .sorted( Comparator.comparing( DataRecord::getAuditInfo )
                        .reversed()
                        .thenComparing( r -> r.getId().toString() ) )
                .toList();

        log.debug( "First page: {}", firstPage.content()
                .stream()
                .map( r -> r.getName() + " -> " + r.getAuditInfo().getCreatedAt() )
                .toList() );
        log.debug( "Second page: {}", secondPage.content()
                .stream()
                .map( r -> r.getName() + " -> " + r.getAuditInfo().getCreatedAt() )
                .toList() );
        log.debug( "All : {}",
                all.stream().map( r -> r.getName() + " -> " + r.getAuditInfo().getCreatedAt() ).toList() );

        assertThat( firstPage.getContent() ).hasSize( 5 );
        assertThat( secondPage.getContent() ).hasSize( 10 );

        assertThat( firstPage.getContent() ).containsExactlyElementsOf( all.subList( 0, 5 ) );
        assertThat( secondPage.getContent() ).containsExactlyElementsOf( all.subList( 5, 15 ) );
        assertThat( secondPage.next() ).isEmpty();
    }

    @Test
    void shouldFetchPagesOrderedByNameAsc() {
        final TestData testData = defaultData( 5 );
        // duplicate names
        testDataPersister.persist( testData.generateRecords( 10 ) );

        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 5 ).asc( DataRecord_.name ).asc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        final var secondPage = dataRecordRepository.loadPage( firstPage.next( 10 ).orElseThrow() );
        final var all = dataRecordRepository.findAll()
                .stream()
                .sorted( Comparator.comparing( DataRecord::getName ).thenComparing( r -> r.getId().toString() ) )
                .toList();

        assertThat( firstPage ).isNotNull();
        assertThat( secondPage ).isNotNull();

        log.debug( "First page: {}", firstPage.content().stream().map( DataRecord::getName ).toList() );
        log.debug( "Second page: {}", secondPage.content().stream().map( DataRecord::getName ).toList() );
        log.debug( "All : {}", all.stream().map( DataRecord::getName ).toList() );

        final var firstPageContent = firstPage.getContent();
        assertThat( firstPageContent ).hasSize( 5 );
        final var secondPageContent = secondPage.getContent();
        assertThat( secondPageContent ).hasSize( 10 );

        final var lastOnFirstPage = firstPageContent.getLast();
        final var firstOnSecondPage = secondPageContent.getFirst();

        assertThat( lastOnFirstPage ).extracting( DataRecord::getName ).isEqualTo( "Charlie" );
        assertThat( firstOnSecondPage ).extracting( DataRecord::getName ).isEqualTo( "Charlie" );
        assertThat( lastOnFirstPage.getId() ).isNotEqualTo( firstOnSecondPage.getId() );

        assertThat( firstPageContent ).containsExactlyElementsOf( all.subList( 0, 5 ) );
        assertThat( secondPageContent ).containsExactlyElementsOf( all.subList( 5, 15 ) );
        assertThat( secondPage.next() ).isEmpty();
    }

    @Test
    void shouldUseDefaultPageSize() {
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.asc( DataRecord_.id ) );
        assertThat( request.pageSize() ).isEqualTo( PageRequest.DEFAULT_PAGE_SIZE );
    }

    @Test
    void shouldFilterResults() {
        defaultData();
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.name ).equalTo( "Alpha" ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().equals( "Alpha" ) );
    }

    @Test
    void shouldFilterResultsWithInPredicate() {
        defaultData();
        final Filter nameIsAlphaOrBravo = Filter.create( b -> b.attribute( DataRecord_.name ).in( "Alpha", "Bravo" ) );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( nameIsAlphaOrBravo ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch(
                e -> e.getName().equals( "Alpha" ) || e.getName().equals( "Bravo" ) );
    }

    @Test
    void shouldFilterResultsWithLikeExpression() {
        defaultData( TestData.NAMES.length * 2 );
        final Filter nameLikeAlp = Filter.create( b -> b.attribute( DataRecord_.name ).like( "Alp%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLikeAlp ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().startsWith( "Alp" ) );
    }

    @Test
    void shouldFilterResultsWithLikeExpression2() {
        defaultData( TestData.NAMES.length * 2 );
        final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).like( "%r%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().indexOf( 'r' ) > 0 );
    }

    @Test
    void shouldFilterResultsWithMultipleLikeExpressions() {
        defaultData( TestData.NAMES.length * 2 );
        final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).like( "A%", "B%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch(
                e -> e.getName().startsWith( "A" ) || e.getName().startsWith( "B" ) );
    }

    @Test
    void shouldFilterByGreaterAndLowerThan() {
        final var count = TestData.NAMES.length;
        defaultData( count );
        final var all = dataRecordRepository.loadPage( PageRequest.create(
                        r -> r.asc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) ).pageSize( count ) ) )
                .content();

        final var createdAt = Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt );
        final var firstCreatedAt = all.getFirst().getAuditInfo().getCreatedAt();
        final var lastCreatedAt = all.getLast().getAuditInfo().getCreatedAt();
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filter.create( f -> f.attribute( createdAt ).greaterThan( firstCreatedAt ) ) ) ) ) ) //
                .hasSize( all.size() - 1 );
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filter.create( f -> f.attribute( createdAt ).greaterThan( lastCreatedAt ) ) ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filter.create( f -> f.attribute( createdAt ).lessThan( firstCreatedAt ) ) ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filter.create( f -> f.attribute( createdAt ).lessThan( lastCreatedAt ) ) ) ) ) ) //
                .hasSize( all.size() - 1 );
    }

    @Test
    void shouldFilterAndFetchNextWithAndFilter() {
        defaultData();
        final var names = List.of( NAME_ALPHA, NAME_BRAVO, "Charlie", "Delta", "Echo" );
        final var status = List.of( Status.DRAFT, Status.ACTIVE );

        final var page1 = dataRecordRepository.loadPage( PageRequest.create(
                r -> r.asc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                        .pageSize( 5 )
                        .filter( Filters.and( attribute( DataRecord_.name ).in( names ),
                                attribute( DataRecord_.status ).in( status ) ) ) ) );
        assertThat( page1 ).allMatch( r -> names.contains( r.getName() ) && status.contains( r.getStatus() ) );

        final var nextRequest = page1.next().orElseThrow();
        assertThat( nextRequest.filters() ).hasSize( 1 );

        final var page2 = dataRecordRepository.loadPage( nextRequest );
        assertThat( page2 ).hasSize( 5 )
                .allMatch( r -> names.contains( r.getName() ) && status.contains( r.getStatus() ) );
    }

    @Test
    void shouldCountWhereFilterWithLikeExpressions() {
        defaultData( TestData.NAMES.length );
        final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).like( "A%", "B%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    void shouldAddCountInPageRequestWhenRequested() {
        // Given
        final TestData testData = defaultData( NAMES.length );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 5 ).enableTotalCount( true ).asc( DataRecord_.id ) );

        // When
        final var page = dataRecordRepository.loadPage( request );

        assertThat( page ).isNotNull();
        assertThat( page.getTotalCount() ).isPresent().get().isEqualTo( (long) NAMES.length );

        testDataPersister.persist( testData.generateRecords( 1 ) );
        final var secondPage = dataRecordRepository.loadPage( page.next().orElseThrow() );

        // THEN the totalCount should not be re-calculated
        assertThat( secondPage.getTotalCount() ).isPresent().get().isEqualTo( (long) NAMES.length );

        final var thirdPage = dataRecordRepository.loadPage(
                secondPage.next().orElseThrow().withEnableTotalCount( true ) );
        assertThat( thirdPage.getTotalCount() ).isPresent().get().isEqualTo( NAMES.length + 1L );
    }

    @Test
    void shouldReturnTotalCountWhenNoFilterPresent() {
        defaultData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 42L );
    }

    @Test
    void shouldReturnZeroCountWhenNoRecordsMatches() {
        defaultData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.name ).equalTo( "name-does-not-exist" ) ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isZero();
    }

    private static class OnlyPublicFilterRule implements FilterRule {

        @Override
        public Predicate toPredicate( final QueryBuilder cqb ) {
            final var builder = cqb.cb();
            final var root = cqb.root();
            root.fetch( DataRecord_.SECURITY_CLASS, JoinType.LEFT );
            return builder.equal( root.get( DataRecord_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ), 0 );
        }

        @Override
        public Predicate toCountPredicate( final QueryBuilder cqb ) {
            final var builder = cqb.cb();
            final var root = cqb.root();
            root.join( DataRecord_.SECURITY_CLASS, JoinType.LEFT );
            return builder.equal( root.get( DataRecord_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ), 0 );
        }

        @Override
        public List<Attribute> attributes() {
            return List.of( Attribute.of( DataRecord_.securityClass, SecurityClass_.level ) );
        }
    }

    @Test
    void shouldReturnFilteredEntityCountWhenFilterPresent() {
        final List<DataRecord> all = defaultData( TestData.NAMES.length ).records();
        final long countPublicAccess = all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 5 ).asc( DataRecord_.id ).rule( new OnlyPublicFilterRule() ) );

        final var page = dataRecordRepository.loadPage( request.withPageSize( 200 ) );
        final var count = dataRecordRepository.count( request );

        assertThat( page.size() ).isEqualTo( countPublicAccess );
        assertThat( count ).isEqualTo( countPublicAccess );
    }

    @Test
    void shouldFilterByEnumAttribute() {
        final List<DataRecord> all = defaultData( 99 ).records();
        final int countDraft = (int) all.stream().filter( r -> r.getStatus() == Status.DRAFT ).count();
        final int countActive = (int) all.stream().filter( r -> r.getStatus() == Status.ACTIVE ).count();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.status ).equalTo( Status.DRAFT ) ) ) );

        assertThat( page ).hasSize( countDraft );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.status ).in( Status.DRAFT, Status.ACTIVE ) ) ) );

        assertThat( page2 ).hasSize( countDraft + countActive );
    }

    @Test
    void shouldNotIgnoreCaseInEqualFilter() {
        defaultData( TestData.NAMES.length );

        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( attribute( DataRecord_.name ).equalTo( NAME_ALPHA.toUpperCase() ) )
                        .asc( DataRecord_.id ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( attribute( DataRecord_.name ).equalTo( "alPHa" ) ).asc( DataRecord_.id ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( attribute( DataRecord_.name ).in( "alPHa", "BRAVO" ) ).asc( DataRecord_.id ) ) ) ) //
                .isEmpty();
    }

    @Test
    void shouldIgnoreCaseInEqualFilter() {
        defaultData( TestData.NAMES.length );

        // equal
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filters.ignoreCase( DataRecord_.name ).equalTo( NAME_ALPHA.toUpperCase() ) )
                        .asc( DataRecord_.id ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );

        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filters.ignoreCase( DataRecord_.name ).equalTo( "alPHa" ) )
                        .asc( DataRecord_.id ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );
    }

    @Test
    void shouldIgnoreCaseInWithInFilter() {
        defaultData( TestData.NAMES.length );

        // in operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filters.ignoreCase( DataRecord_.name ).in( "alPHa", NAME_BRAVO.toUpperCase() ) )
                        .asc( DataRecord_.id ) ) ) ) //
                .hasSize( 2 ).first().extracting( DataRecord::getName ).isIn( NAME_ALPHA, NAME_BRAVO );
    }

    @Test
    void shouldIgnoreCaseWithLikeFilter() {
        defaultData( TestData.NAMES.length );

        // Like operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create( //
                r -> r.filter( Filters.ignoreCase( DataRecord_.name ).like( "ALPH%" ) ) //
                        .asc( DataRecord_.id ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );

        // like-in operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filters.ignoreCase( DataRecord_.name ).like( "ALPH%", "BRA%" ) )
                        .asc( DataRecord_.id ) ) ) )  //
                .hasSize( 2 ).first().extracting( DataRecord::getName ).isIn( NAME_ALPHA, NAME_BRAVO );
    }

    @Test
    void shouldFilterWithGreaterAndLessThan() {
        defaultData( TestData.NAMES.length );

        final var all = dataRecordRepository.findAllOrderedByAuditInfoCreatedAtAsc();

        final var createdAt = Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt );
        final var firstCreatedAt = all.getFirst().getAuditInfo().getCreatedAt();
        final var lastCreatedAt = all.getLast().getAuditInfo().getCreatedAt();

        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filters.attribute( createdAt ).greaterThan( firstCreatedAt ) ) ) ) ) //
                .hasSize( all.size() - 1 );
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filters.attribute( createdAt ).greaterThan( lastCreatedAt ) ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filters.attribute( createdAt ).greaterThanOrEqualTo( lastCreatedAt ) ) ) ) ) //
                .hasSize( 1 );
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.desc( DataRecord_.id )
                .filter( Filters.attribute( createdAt ).lessThanOrEqualTo( firstCreatedAt ) ) ) ) ) //
                .hasSize( 1 );
    }

    @Test
    void shouldFilterByEntryInManyToOneRelationship() {
        final List<DataRecord> all = defaultData( 99 ).records();
        final int countPublic = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final int countStandard = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 1 ).count();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.securityClass ).equalTo(
                        securityClassRepository.findByName( "public" ) ) ) ) );

        assertThat( page ).hasSize( countPublic );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.securityClass ).in( securityClassRepository.findByName( "public" ),
                        securityClassRepository.findByName( "standard" ) ) ) ) );

        assertThat( page2 ).hasSize( countPublic + countStandard );
    }

    @Test
    void shouldFilterByAttributeOfEntryInManyToOneRelationship() {
        final List<DataRecord> all = defaultData( 99 ).records();
        final int countPublic = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final int countStandard = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 1 ).count();

        final var attributeSecurityClassLevel = Attribute.of( DataRecord_.securityClass, SecurityClass_.level );
        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( attributeSecurityClassLevel ).equalTo( 0 ) ) ) );

        assertThat( page ).hasSize( countPublic );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( attributeSecurityClassLevel ).in( 0, 1 ) ) ) );

        assertThat( page2 ).hasSize( countPublic + countStandard );
    }

    @Test
    void shouldFilterByAttributeOfEntryInManyToManyRelationship() {
        final List<DataRecord> all = defaultData( 99 ).records();
        final var redTag = tagRepository.findByName( "red" );
        final var greenTag = tagRepository.findByName( "green" );
        final int redOrGreenCount = (int) all.stream()
                .filter( r -> r.getTags().contains( greenTag ) || r.getTags().contains( redTag ) )
                .count();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( attribute( DataRecord_.tags, Tag_.name ).in( "green", "red" ) ) ) );

        assertThat( page ).hasSize( redOrGreenCount );
    }

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

    @Test
    void shouldUseMoreComplicateFilterRulesForAclChecks() {
        defaultData();
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .rule( new AclCheckFilterRule( SUBJECT_READ_STANDARD, READ ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).allMatch( e -> e.getSecurityClass().getLevel() <= 1 );

        final PageRequest<DataRecord> request2 = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.of( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .rule( new AclCheckFilterRule( "does not exist", READ ) ) );

        final var shouldBeEmpty = dataRecordRepository.loadPage( request2 );
        assertThat( shouldBeEmpty ).isNotNull();
        assertThat( shouldBeEmpty.getContent() ).isEmpty();
        assertThat( dataRecordRepository.count( request2 ) ).isZero();
    }

    @Test
    void shouldFilterByNotExists() {
        defaultData( NAMES.length * 2 );
        final var allWithoutTag = dataRecordRepository.findAll().stream().filter( r -> r.getTags().isEmpty() ).toList();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .rule( Rules.withParameter( "do-it", "true" )
                        .where( DataRecord_.tags )
                        .withParameter( "yet-another", List.of( "parameter" ) )
                        .isEmpty() ) ) );

        assertThat( page ).containsExactlyInAnyOrderElementsOf( allWithoutTag );
    }

    @Test
    void shouldFilterByExists() {
        defaultData( NAMES.length * 2 );
        final var allWithTag = dataRecordRepository.findAll().stream().filter( r -> !r.getTags().isEmpty() ).toList();

        final var page = dataRecordRepository.loadPage( PageRequest.create(
                b -> b.pageSize( 99 ).asc( DataRecord_.id ).rule( Rules.where( DataRecord_.tags ).isNotEmpty() ) ) );

        assertThat( page ).containsExactlyInAnyOrderElementsOf( allWithTag );
    }

    @Test
    void shouldCombineFilterByOrCondition() {
        final var all = defaultData( 99 ).records();
        final int expectedSize = (int) all.stream()
                .filter( r -> r.getName().equals( NAME_ALPHA ) || r.getName().equals( NAME_BRAVO ) )
                .count();
        final var nameIsAlpha = attribute( DataRecord_.name ).equalTo( NAME_ALPHA );
        final var nameIsBravo = attribute( DataRecord_.name ).equalTo( NAME_BRAVO );
        final var request = PageRequest.<DataRecord>create(
                r -> r.desc( DataRecord_.id ).filter( Filters.or( nameIsAlpha, nameIsBravo ) ) );

        final var result = dataRecordRepository.loadPage( request );

        assertThat( result ).hasSize( expectedSize )
                .allSatisfy( r -> assertThat( r ).extracting( DataRecord::getName ).isIn( NAME_ALPHA, NAME_BRAVO ) );
    }


    @Test
    void shouldCombineAndWithOrFilter() {
        final var all = defaultData( 99 ).records();
        final Tag red = tagRepository.findByName( "red" );
        final Tag green = tagRepository.findByName( "green" );
        final int expectedSize = (int) all.stream()
                .filter( r -> (r.getName().equals( NAME_ALPHA ) && r.getTags().contains( red )) //
                        || (r.getName().equals( NAME_BRAVO ) && r.getTags().contains( green )) )
                .count();
        final var redAlpha = Filters.and( attribute( DataRecord_.name ).equalTo( NAME_ALPHA ),
                attribute( DataRecord_.tags, Tag_.name ).equalTo( red.getName() ) );
        final var greenBravo = Filters.and( attribute( DataRecord_.name ).equalTo( NAME_BRAVO ),
                attribute( DataRecord_.tags, Tag_.name ).equalTo( green.getName() ) );
        final var request = PageRequest.<DataRecord>create(
                r -> r.desc( DataRecord_.id ).filter( Filters.or( redAlpha, greenBravo ) ) );

        final var result = dataRecordRepository.loadPage( request );

        assertThat( result ).hasSize( expectedSize ).allSatisfy( r -> {
            if ( r.getName().equals( NAME_ALPHA ) ) {
                assertThat( r.getTags() ).contains( red );
            } else if ( r.getName().equals( NAME_BRAVO ) ) {
                assertThat( r.getTags() ).contains( green );
            } else {
                throw new AssertionError( "Unexpected name: " + r.getName() );
            }
        } );
    }

    @Test
    void shouldReverseDirectionOfCursors() {
        defaultData( 10 );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 5 ).asc( DataRecord_.name ).asc( DataRecord_.id ) );

        final var allRecords = dataRecordRepository.loadPage( request.withPageSize( 10 ) );
        final var firstPage = dataRecordRepository.loadPage( request );
        final var secondPage = dataRecordRepository.loadPage( firstPage.next().orElseThrow() );
        final var reversedFirstPage = dataRecordRepository.loadPage( secondPage.self().toReversed() );
        logNames( "First", firstPage );
        logNames( "Second", secondPage );
        logNames( "First/reversed", reversedFirstPage );
        logNames( "All", allRecords );

        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).hasSize( 5 );
        assertThat( secondPage ).isNotNull();
        assertThat( secondPage.getContent() ).hasSize( 5 );

        assertThat( reversedFirstPage ).isNotNull().containsExactlyElementsOf( firstPage );
        assertThat( reversedFirstPage.next() ).isNotPresent();
    }

    @Test
    void shouldMixRulesWithFilters() {
        final var data = testDataPersister.persist( td -> td.recordCount( 10 ).recordNames( NAME_ALPHA, NAME_BRAVO ) )
                .records();
        final var recordsWithSecClass0 = data.stream()
                .filter( r -> r.getSecurityClass().getLevel() == 0 && r.getName().startsWith( "A" ) )
                .toList();

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .asc( DataRecord_.name )
                .asc( DataRecord_.id )
                .filter( Filters.and( Filters.attribute( DataRecord_.name ).like( "A%" ),
                        new OnlyPublicFilterRule() ) ) );
        final var page = dataRecordRepository.loadPage( request );
        assertThat( page ).hasSize( recordsWithSecClass0.size() )
                .allSatisfy( r -> assertThat( r.getName() ).startsWith( "A" ) )
                .allSatisfy( r -> assertThat( r.getSecurityClass().getLevel() ).isEqualTo( 0 ) );
    }

    @Test
    void shouldFilterDataRecordsWithNoTags() {
        final TestData data = testDataPersister.persist(
                td -> td.recordCount( 100 ).tagNames( TestData.TAG_RED, TestData.TAG_BLUE ) );
        final var redTag = tagRepository.findByName( TestData.TAG_RED );
        final var recordsWithoutRedTag = new LinkedList<DataRecord>();
        final var recordsWithoutAnyTag = new LinkedList<DataRecord>();
        final var recordsWithRedTag = new LinkedList<DataRecord>();
        final var alphaBrovoRecordsWithoutRedTag = new LinkedList<DataRecord>();

        data.records().forEach( r -> {
            if ( r.getTags().isEmpty() ) {
                recordsWithoutAnyTag.add( r );
            }
            if ( r.getTags().contains( redTag ) ) {
                recordsWithRedTag.add( r );
            } else {
                recordsWithoutRedTag.add( r );
                if ( List.of( NAME_ALPHA, NAME_BRAVO ).contains( r.getName() ) ) {
                    alphaBrovoRecordsWithoutRedTag.add( r );
                }
            }
        } );
        assertThat( recordsWithoutAnyTag ).isNotEmpty();
        assertThat( recordsWithoutRedTag ).isNotEmpty();
        assertThat( recordsWithRedTag ).isNotEmpty();

        final var request = PageRequest.<DataRecord>create( b -> b.pageSize( 100 )
                .asc( DataRecord_.name )
                .asc( DataRecord_.id )
                .filter( NoTagFilterRule.of( List.of( TAG_RED ) ) )
                .enableTotalCount( true ) );
        final var page = dataRecordRepository.loadPage( request );

        assertThat( page ).hasSize( recordsWithoutRedTag.size() ).allSatisfy( r -> {
            assertThat( r.getTags() ).doesNotContain( redTag );
            assertThat( r.getName() ).isNotEqualTo( TAG_RED );
        } );

        final var requestForNoTags = PageRequest.<DataRecord>create( b -> b.pageSize( 100 )
                .asc( DataRecord_.name )
                .asc( DataRecord_.id )
                .filter( NoTagFilterRule.of( List.of() ) )
                .enableTotalCount( true ) );
        final var page2 = dataRecordRepository.loadPage( requestForNoTags );

        assertThat( page2 ).hasSize( recordsWithoutAnyTag.size() );

        // now we mix filters with ruled
        final var request3 = PageRequest.<DataRecord>create( b -> b.pageSize( 100 )
                .asc( DataRecord_.name )
                .asc( DataRecord_.id )
                .filter( Filters.and( NoTagFilterRule.of( List.of( TAG_RED ) ),
                        Filters.attribute( DataRecord_.name ).in( NAME_ALPHA, NAME_BRAVO ) ) )
                .enableTotalCount( true ) );
        final var page3 = dataRecordRepository.loadPage( request3 );
        assertThat( page3 ).hasSize( alphaBrovoRecordsWithoutRedTag.size() )
                .allSatisfy( r -> assertThat( r.getTags() ).doesNotContain( redTag ) )
                .allSatisfy( r -> assertThat( r.getName() ).isIn( NAME_ALPHA, NAME_BRAVO ) );
    }

    private static void logNames( final String message, final Page<DataRecord> allRecords ) {
        if ( log.isDebugEnabled() ) {
            log.debug( message + ": {}", allRecords.content().stream().map( DataRecord::getName ).toList() );
        }
    }
}


