package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.FilterRule;
import io.vigier.cursorpaging.jpa.Page;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.QueryBuilder;
import io.vigier.cursorpaging.jpa.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursorpaging.jpa.itest.config.JpaConfig;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry;
import io.vigier.cursorpaging.jpa.itest.model.AccessEntry_;
import io.vigier.cursorpaging.jpa.itest.model.AuditInfo_;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord.Fields;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass_;
import io.vigier.cursorpaging.jpa.itest.model.Status;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import io.vigier.cursorpaging.jpa.itest.repository.AccessEntryRepository;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.itest.repository.SecurityClassRepository;
import io.vigier.cursorpaging.jpa.itest.repository.TagRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static io.vigier.cursorpaging.jpa.itest.TestDataGenerator.NAME_ALPHA;
import static io.vigier.cursorpaging.jpa.itest.TestDataGenerator.NAME_BRAVO;
import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.READ;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
@Import( { PostgreSqlTestConfiguration.class, JpaConfig.class } )
class PostgreSqlCursorPageTest {

    public static final Attribute DATARECORD_ID = Attribute.of( Fields.id, UUID.class );
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
    private TestDataGenerator testDataGenerator;
    @Autowired
    private TagRepository tagRepository;

    @Test
    void contextLoads() {
        final var factoryBeans = applicationContext.getBeansOfType( CursorPageRepositoryFactoryBean.class );
        assertThat( factoryBeans.size() ).isGreaterThanOrEqualTo( 1 ); // I.e. one per repository
    }


    @AfterEach
    @Transactional
    void cleanup() {
        accessEntryRepository.deleteAllInBatch();
        dataRecordRepository.deleteAllInBatch();
        securityClassRepository.deleteAllInBatch();
    }

    @Test
    void shouldFetchFirstPage() {
        testDataGenerator.generateData( 100 );
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
        testDataGenerator.generateData( 30 );
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
        testDataGenerator.generateData( 15 );

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.modifiedAt ) )
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
        testDataGenerator.generateData( 5 );
        // duplicate names
        testDataGenerator.generateDataRecords( 10 );

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

        final var lastOnFirstPage = firstPageContent.get( firstPageContent.size() - 1 );
        final var firstOnSecondPage = secondPageContent.get( 0 );

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
        testDataGenerator.generateData( 100 );
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
        testDataGenerator.generateData( 100 );
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
    void shouldFilterResultsWithLikeExpression() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length * 2 );
        final Filter nameLikeAlp = Filter.create( b -> b.attribute( DataRecord_.name ).like( "Alp%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLikeAlp ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().startsWith( "Alp" ) );
    }

    @Test
    void shouldFilterResultsWithLikeExpression2() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length * 2 );
        final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).like( "%r%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().indexOf( 'r' ) > 0 );
    }

    @Test
    void shouldFilterResultsWithMultipleLikeExpressions() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length * 2 );
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
        final var count = TestDataGenerator.NAMES.length;
        testDataGenerator.generateData( count );
        var all = dataRecordRepository.loadPage( PageRequest.create(
                        r -> r.asc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) ).pageSize( count ) ) )
                .content();

        final var createdAt = Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt );
        final var firstCreatedAt = all.get( 0 ).getAuditInfo().getCreatedAt();
        final var lastCreatedAt = all.get( all.size() - 1 ).getAuditInfo().getCreatedAt();
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
    void shouldCountWhereFilterWithLikeExpressions() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );
        final Filter nameLike = Filter.create( b -> b.attribute( DataRecord_.name ).like( "A%", "B%" ) );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 100 ).asc( DataRecord_.id ).filter( nameLike ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    void shouldAddCountInPageRequestWhenRequested() {
        // Given
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );
        final PageRequest<DataRecord> request = PageRequest.create(
                b -> b.pageSize( 5 ).enableTotalCount( true ).asc( DataRecord_.id ) );

        // When
        final var page = dataRecordRepository.loadPage( request );

        assertThat( page ).isNotNull();
        assertThat( page.getTotalCount() ).isPresent().get().isEqualTo( (long) TestDataGenerator.NAMES.length );

        testDataGenerator.generateDataRecords( 66 );
        final var secondPage = dataRecordRepository.loadPage( page.next().orElseThrow() );

        // THEN the totalCount should not be re-calculated
        assertThat( secondPage.getTotalCount() ).isPresent().get().isEqualTo( (long) TestDataGenerator.NAMES.length );

        final var thirdPage = dataRecordRepository.loadPage(
                secondPage.next().orElseThrow().withEnableTotalCount( true ) );
        assertThat( thirdPage.getTotalCount() ).isPresent().get().isEqualTo( TestDataGenerator.NAMES.length + 66L );
    }

    @Test
    void shouldReturnTotalCountWhenNoFilterPresent() {
        testDataGenerator.generateData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 42L );
    }

    @Test
    void shouldReturnZeroCountWhenNoRecordsMatches() {
        testDataGenerator.generateData( 42 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.name, "name-does-not-exist" ) ) );

        final var count = dataRecordRepository.count( request );

        assertThat( count ).isEqualTo( 0 );
    }

    private static class OnlyPublicFilterRule implements FilterRule {

        @Override
        public Predicate getPredicate( final QueryBuilder cqb ) {
            final var builder = cqb.cb();
            final var root = cqb.root();
            root.fetch( DataRecord_.SECURITY_CLASS, JoinType.LEFT );
            return builder.equal( root.get( DataRecord_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ), 0 );
        }

        @Override
        public Predicate getCountPredicate( final QueryBuilder cqb ) {
            final var builder = cqb.cb();
            final var root = cqb.root();
            root.join( DataRecord_.SECURITY_CLASS, JoinType.LEFT );
            return builder.equal( root.get( DataRecord_.SECURITY_CLASS ).get( SecurityClass_.LEVEL ), 0 );
        }
    }

    @Test
    void shouldReturnFilteredEntityCountWhenFilterPresent() {
        final List<DataRecord> all = testDataGenerator.generateData( TestDataGenerator.NAMES.length );
        final long countPublicAccess = all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 )
                .asc( DataRecord_.id ).rule( new OnlyPublicFilterRule() ) );

        final var page = dataRecordRepository.loadPage( request.withPageSize( 200 ) );
        final var count = dataRecordRepository.count( request );

        assertThat( page.size() ).isEqualTo( countPublicAccess );
        assertThat( count ).isEqualTo( countPublicAccess );
    }

    @Test
    void shouldFilterByEnumAttribute() {
        final List<DataRecord> all = testDataGenerator.generateData( 99 );
        final int countDraft = (int) all.stream().filter( r -> r.getStatus() == Status.DRAFT ).count();
        final int countActive = (int) all.stream().filter( r -> r.getStatus() == Status.ACTIVE ).count();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.status, Status.DRAFT ) ) ) );

        assertThat( page ).hasSize( countDraft );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.status, Status.DRAFT, Status.ACTIVE ) ) ) );

        assertThat( page2 ).hasSize( countDraft + countActive );
    }

    @Test
    void shouldNotIgnoreCaseInEqualFilter() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );

        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter(
                Filter.attributeIs( Attribute.of( DataRecord_.name ), NAME_ALPHA.toUpperCase() ) ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filter.attributeIs( Attribute.of( DataRecord_.name ), "alPHa" ) ) ) ) ) //
                .isEmpty();
        assertThat( dataRecordRepository.loadPage( PageRequest.create(
                r -> r.filter( Filter.attributeIs( Attribute.of( DataRecord_.name ), "alPHa", "BRAVO" ) ) ) ) ) //
                .isEmpty();
    }

    @Test
    void shouldIgnoreCaseInEqualFilter() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );

        // equal
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter(
                Filter.attributeIs( Attribute.of( DataRecord_.name ).withIgnoreCase(),
                        NAME_ALPHA.toUpperCase() ) ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );

        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter(
                Filter.attributeIs( Attribute.of( DataRecord_.name ).withIgnoreCase(), "alPHa" ) ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );
    }

    @Test
    void shouldIgnoreCaseInWithInFilter() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );

        // in operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter(
                Filter.attributeIs( Attribute.of( DataRecord_.name ).withIgnoreCase(), "alPHa",
                        NAME_BRAVO.toUpperCase() ) ) ) ) ) //
                .hasSize( 2 ).first().extracting( DataRecord::getName ).isIn( NAME_ALPHA, NAME_BRAVO );
    }

    @Test
    void shouldIgnoreCaseWithLikeFilter() {
        testDataGenerator.generateData( TestDataGenerator.NAMES.length );

        // Like operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter( Filter.create(
                f -> f.attribute( Attribute.of( DataRecord_.name ).withIgnoreCase() ).like( "ALPH%" ) ) ) ) ) ) //
                .hasSize( 1 ).first().extracting( DataRecord::getName ).isEqualTo( NAME_ALPHA );

        // like-in operation
        assertThat( dataRecordRepository.loadPage( PageRequest.create( r -> r.filter( Filter.create(
                f -> f.attribute( Attribute.of( DataRecord_.name ).withIgnoreCase() )
                        .like( "ALPH%", "BRA%" ) ) ) ) ) ) //
                .hasSize( 2 ).first().extracting( DataRecord::getName ).isIn( NAME_ALPHA, NAME_BRAVO );
    }

    @Test
    void shouldFilterByEntryInManyToOneRelationship() {
        final List<DataRecord> all = testDataGenerator.generateData( 99 );
        final int countPublic = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final int countStandard = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 1 ).count();

        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.securityClass,
                        securityClassRepository.findByName( "public" ) ) ) ) );

        assertThat( page ).hasSize( countPublic );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.securityClass, securityClassRepository.findByName( "public" ),
                        securityClassRepository.findByName( "standard" ) ) ) ) );

        assertThat( page2 ).hasSize( countPublic + countStandard );
    }

    @Test
    void shouldFilterByAttributeOfEntryInManyToOneRelationship() {
        final List<DataRecord> all = testDataGenerator.generateData( 99 );
        final int countPublic = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 0 ).count();
        final int countStandard = (int) all.stream().filter( r -> r.getSecurityClass().getLevel() == 1 ).count();

        final var attributeSecurityClassLevel = Attribute.path( DataRecord_.securityClass, SecurityClass_.level );
        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( attributeSecurityClassLevel, 0 ) ) ) );

        assertThat( page ).hasSize( countPublic );

        final var page2 = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( attributeSecurityClassLevel, 0, 1 ) ) ) );

        assertThat( page2 ).hasSize( countPublic + countStandard );
    }

    @Test
    void shouldFilterByAttributeOfEntryInManyToManyRelationship() {
        final List<DataRecord> all = testDataGenerator.generateData( 99 );
        var redTag = tagRepository.findByName( "red" );
        var greenTag = tagRepository.findByName( "green" );
        final int redOrGreenCount = (int) all.stream()
                .filter( r -> r.getTags().contains( greenTag ) || r.getTags().contains( redTag ) )
                .count();

        final var attribute = Attribute.path( DataRecord_.tags, Tag_.name );
        final var page = dataRecordRepository.loadPage( PageRequest.create( b -> b.pageSize( 99 )
                .asc( DataRecord_.id )
                .filter( Filter.attributeIs( attribute, "green", "red" ) ) ) );

        assertThat( page ).hasSize( redOrGreenCount );
    }

    @RequiredArgsConstructor
    @Builder
    private static class AclCheckFilterRule implements FilterRule {

        private final String subject;
        private final AccessEntry.Action action;

        @Override
        public Predicate getPredicate( final QueryBuilder cqb ) {
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
    public void shouldUseMoreComplicateFilterRulesForAclChecks() {
        testDataGenerator.generateData( 100 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .rule( new AclCheckFilterRule( SUBJECT_READ_STANDARD, READ ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).allMatch( e -> e.getSecurityClass().getLevel() <= 1 );

        final PageRequest<DataRecord> request2 = PageRequest.create( b -> b.pageSize( 100 )
                .desc( Attribute.path( DataRecord_.auditInfo, AuditInfo_.createdAt ) )
                .asc( DataRecord_.id )
                .rule( new AclCheckFilterRule( "does not exist", READ ) ) );

        final var shouldBeEmpty = dataRecordRepository.loadPage( request2 );
        assertThat( shouldBeEmpty ).isNotNull();
        assertThat( shouldBeEmpty.getContent() ).isEmpty();
        assertThat( dataRecordRepository.count( request2 ) ).isEqualTo( 0 );
    }

    @Test
    void shouldReverseDirectionOfCursors() {
        testDataGenerator.generateData( 10 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 5 ).asc( DataRecord_.name )
                .asc( DataRecord_.id ) );

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

        assertThat( reversedFirstPage ).isNotNull();
        assertThat( reversedFirstPage ).containsExactlyElementsOf( firstPage );
        assertThat( reversedFirstPage.next() ).isNotPresent();
    }

    private static void logNames( final String message, final Page<DataRecord> allRecords ) {
        if ( log.isDebugEnabled() ) {
            log.debug( message + ": {}", allRecords.content().stream().map( DataRecord::getName ).toList() );
        }
    }
}


