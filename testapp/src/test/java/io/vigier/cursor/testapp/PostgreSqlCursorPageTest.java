package io.vigier.cursor.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import io.vigier.cursor.Filter;
import io.vigier.cursor.PageRequest;
import io.vigier.cursor.Position;
import io.vigier.cursor.repository.bootstrap.CursorPageRepositoryFactoryBean;
import io.vigier.cursor.testapp.model.DataRecord;
import io.vigier.cursor.testapp.model.DataRecord_;
import io.vigier.cursor.testapp.repository.DataRecordRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;


@SpringBootTest
@Slf4j
@Import( PostgreSqlTestConfiguration.class )
class PostgreSqlCursorPageTest {

    private static final String[] NAMES = { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
            "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
            "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu" };

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Test
    void contextLoads() {
        assertThat( applicationContext.getBean( CursorPageRepositoryFactoryBean.class ) ).isNotNull();
    }

    void generateData( final int count ) {
        Instant created = Instant.parse( "1999-01-02T10:15:30.00Z" );
        for ( int i = 0; i < count; i++ ) {
            created = created.plus( 1, ChronoUnit.DAYS );
            dataRecordRepository.save( DataRecord.builder()
                    .createdAt( created )
                    .name( nextName( i ) )
                    .build() );
        }
        log.info( "Generated {} test data-records", dataRecordRepository.count() );
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

        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 )
                .attributeAsc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).hasSize( 10 );
        // Result should be sorted by ID...
        final var resultIdList = firstPage.getContent().stream().map( DataRecord::getId ).toList();
        final var allIdsSorted = all.stream().map( DataRecord::getId )
                .sorted( Comparator.comparing( UUID::toString ) ).limit( 10 ).toList();
        assertThat( resultIdList ).containsExactlyElementsOf( allIdsSorted );
    }

    @Test
    void shouldFetchNextPage() {
        generateData( 30 );
        final Position<DataRecord, UUID> idPos = Position.attributeAsc( DataRecord_.id );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 10 )
                .position( idPos ) );

        final var firstPage = dataRecordRepository.loadPage( request );
        final var next = firstPage.next();
        assertThat( next ).isPresent();
        final var nextPage = dataRecordRepository.loadPage( next.get().withPageSize( 20 ) );
        assertThat( nextPage ).isNotNull();
        assertThat( nextPage.getContent() ).hasSize( 20 );
        assertThat( nextPage.getContent() ).doesNotContainAnyElementsOf( firstPage.getContent() );
        assertThat( nextPage.next() ).isNotEmpty();
    }

    @Test
    void shouldFetchPagesOrderedByCreatedDesc() {
        generateData( 100 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .attributeDesc( DataRecord_.createdAt ).attributeAsc( DataRecord_.id ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();
        assertThat( firstPage.getContent() ).hasSize( 100 );

        final var all = dataRecordRepository.findAll().stream()
                .sorted( Comparator.comparing( DataRecord::getCreatedAt ).reversed()
                        .thenComparing( r -> r.getId().toString() ) ).toList();

        assertThat( firstPage.getContent() ).containsExactlyElementsOf( all );
        assertThat( firstPage.next() ).isEmpty();
    }

    @Test
    void shouldUseDefaultPageSize() {
        final PageRequest<DataRecord> request = PageRequest.attributeAsc( DataRecord_.id ).withPageSize( 42 );
        assertThat( request.pageSize() ).isEqualTo( PageRequest.DEFAULT_PAGE_SIZE );
    }

    @Test
    void shouldFilterResults() {
        generateData( 100 );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .attributeDesc( DataRecord_.createdAt ).attributeAsc( DataRecord_.id )
                .filter( Filter.attributeIs( DataRecord_.name, "Alpha" ) ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch( e -> e.getName().equals( "Alpha" ) );
    }

    @Test
    void shouldFilterResultsWithInPredicate() {
        generateData( 100 );
        final Filter<DataRecord, String> nameIsAlpha = Filter.create(
                b -> b.attribute( DataRecord_.name ).value( "Alpha" ).value( "Bravo" ) );
        final PageRequest<DataRecord> request = PageRequest.create( b -> b.pageSize( 100 )
                .attributeDesc( DataRecord_.createdAt ).attributeAsc( DataRecord_.id )
                .filter( nameIsAlpha ) );

        final var firstPage = dataRecordRepository.loadPage( request );

        assertThat( firstPage ).isNotNull();

        assertThat( firstPage.getContent() ).allMatch(
                e -> e.getName().equals( "Alpha" ) || e.getName().equals( "Bravo" ) );
    }
}


