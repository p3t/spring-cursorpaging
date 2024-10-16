package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.itest.model.AuditInfo;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass;
import io.vigier.cursorpaging.jpa.itest.model.Status;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.repository.AccessEntryRepository;
import io.vigier.cursorpaging.jpa.itest.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.itest.repository.SecurityClassRepository;
import io.vigier.cursorpaging.jpa.itest.repository.TagRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.READ;
import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.WRITE;

@Service
@Slf4j
public class TestDataGenerator {

    public static final String[] NAMES = { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
            "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
            "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu" };
    public static final String[] TAGS = { "red", "green", "blue", "yellow", "black", "white", "orange", "purple",
            "pink", "brown" };
    public static final String SUBJECT_READ_STANDARD = "read_standard";
    public static final String SUBJECT_READ_SENSITIVE = "read_sensitive";
    public static final String SUBJECT_WRITE_SENSITIVE = "write_sensitive";

    @Autowired
    private DataRecordRepository dataRecordRepository;
    @Autowired
    private AccessEntryRepository accessEntryRepository;
    @Autowired
    private SecurityClassRepository securityClassRepository;
    @Autowired
    private TagRepository tagRepository;

    List<DataRecord> generateData( final int count ) {
        deleteAll();

        securityClassRepository.save( SecurityClass.builder().level( 0 ).name( "public" )
                .build() );
        final SecurityClass cl1 = securityClassRepository.save( SecurityClass.builder().level( 1 ).name( "standard" )
                .build() );
        final SecurityClass cl2 = securityClassRepository.save(
                SecurityClass.builder().level( 2 ).name( "confidential" )
                        .build() );

        for ( String tagName : TAGS ) {
            tagRepository.save( Tag.builder().name( tagName )
                    .build() );
        }

        accessEntryRepository.saveEntry( b -> b.subject( SUBJECT_READ_STANDARD ).action( READ ).securityClass( cl1 ) );
        accessEntryRepository.saveEntry( b -> b.subject( SUBJECT_READ_SENSITIVE ).action( READ ).securityClass( cl2 ) );
        accessEntryRepository.saveEntry(
                b -> b.subject( SUBJECT_WRITE_SENSITIVE ).action( WRITE ).securityClass( cl2 ) );

        return generateDataRecords( count );
    }

    private void deleteAll() {
        accessEntryRepository.deleteAll();
        dataRecordRepository.deleteAll();
        securityClassRepository.deleteAll();
        tagRepository.deleteAll();
        accessEntryRepository.flush();
        dataRecordRepository.flush();
        securityClassRepository.flush();
        tagRepository.flush();
    }

    public List<DataRecord> generateDataRecords( final int count ) {

        final SecurityClass[] securityClasses = securityClassRepository.findAll().toArray( SecurityClass[]::new );
        var tags = tagRepository.findAll();

        Instant created = Instant.parse( "1999-01-02T10:15:30.00Z" );
        final List<DataRecord> allRecords = new ArrayList<>( count );
        for ( int i = 0; i < count; i++ ) {
            created = created.plus( 1, ChronoUnit.DAYS );
            allRecords.add( dataRecordRepository.save( DataRecord.builder()
                    .name( nextName( i ) )
                    .securityClass( securityClasses[i % securityClasses.length] )
                    .auditInfo( AuditInfo.create( created, created.plus( 10, ChronoUnit.MINUTES ) ) )
                    .status( nextStatus( i ) )
                    .tags( someTags( tags, i ) )
                    .build() ) );
        }
        log.info( "Generated {} test data-records", dataRecordRepository.count() );
        return allRecords;
    }

    private Set<Tag> someTags( final List<Tag> tags, final int i ) {
        return Set.of( tags.get( i % tags.size() ), tags.get( (i + 1) % tags.size() ) );
    }

    private static Status nextStatus( final int i ) {
        return Status.values()[i % Status.values().length];
    }

    private String nextName( final int i ) {
        return NAMES[i % NAMES.length];
    }
}
