package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.itest.TestData.TestDataBuilder;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.repository.AccessEntryRepository;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.itest.repository.SecurityClassRepository;
import io.vigier.cursorpaging.jpa.itest.repository.TagRepository;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.util.function.Function.identity;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestDataPersister {
    private final DataRecordRepository dataRecordRepository;
    private final AccessEntryRepository accessEntryRepository;
    private final SecurityClassRepository securityClassRepository;
    private final TagRepository tagRepository;

    public void deleteAll() {
        accessEntryRepository.deleteAllInBatch();
        dataRecordRepository.deleteAllInBatch();
        securityClassRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();

        accessEntryRepository.flush();
        dataRecordRepository.flush();
        securityClassRepository.flush();
        tagRepository.flush();
    }

    @Transactional
    public TestData persist( final TestData testData ) {
        final var persisted = testData.toBuilder();

        persisted.tags( testData.tags()
                .values()
                .stream()
                .map( t -> t.getId() != null ? tagRepository.getReferenceById( t.getId() ) : tagRepository.save( t ) )
                .collect( Collectors.toMap( Tag::getName, identity() ) ) );

        persisted.securityClasses( testData.securityClasses()
                .values()
                .stream()
                .map( sc -> securityClassRepository.findById( sc.getLevel() )
                        .orElseGet( () -> securityClassRepository.save( sc ) ) )
                .collect( Collectors.toMap( SecurityClass::getLevel, identity() ) ) );

        persisted.accessEntries( testData.accessEntries()
                .stream()
                .map( ae -> accessEntryRepository.findById( ae.getId() )
                        .orElseGet( () -> accessEntryRepository.save( ae ) ) )
                .collect( Collectors.toList() ) );

        persisted.records( testData.records()
                .stream()
                .map( dr -> dataRecordRepository.findById( dr.getId() )
                        .orElseGet( () -> dataRecordRepository.save( dr ) ) )
                .collect( Collectors.toList() ) );
        log.info( "Number of records in the test-data: {}", testData.records().size() );
        log.info( "Number of records in the DB: {}", dataRecordRepository.count() );
        return persisted.build();
    }

    @Transactional
    public TestData persist( final Consumer<TestDataBuilder> consumer ) {
        final var builder = TestData.builder();
        consumer.accept( builder );
        final var testData = builder.build();
        return persist( testData );
    }
}
