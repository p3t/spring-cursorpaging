package io.vigier.sbcpreleasetest.service;

import io.vigier.sbcpreleasetest.model.AuditInfo;
import io.vigier.sbcpreleasetest.model.DataRecord;
import io.vigier.sbcpreleasetest.repository.DataRecordRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class TestDataService {

    private static final String[] NAMES = { "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
            "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
            "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu" };

    private final DataRecordRepository dataRecordRepository;

    @PostConstruct
    public void init() {
        generateData( 500 );
    }

    void generateData( final int count ) {
        Instant created = Instant.parse( "1999-01-02T10:15:30.00Z" );
        for ( int i = 0; i < count; i++ ) {
            created = created.plus( 1, ChronoUnit.DAYS );
            dataRecordRepository.save( DataRecord.builder()
                    .name( nextName( i ) )
                    .auditInfo( AuditInfo.create( created, created.plus( 10, ChronoUnit.MINUTES ) ) )
                    .build() );
        }
        log.info( "Generated {} test data-records", dataRecordRepository.count() );
    }

    private String nextName( final int i ) {
        return NAMES[i % NAMES.length];
    }
}
