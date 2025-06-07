package io.vigier.cursorpaging.jpa.itest;

import io.vigier.cursorpaging.jpa.itest.model.AccessEntry;
import io.vigier.cursorpaging.jpa.itest.model.AuditInfo;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.SecurityClass;
import io.vigier.cursorpaging.jpa.itest.model.Status;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.READ;
import static io.vigier.cursorpaging.jpa.itest.model.AccessEntry.Action.WRITE;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Getter
@Accessors( fluent = true )
@Builder
public class TestData {
    public static final String NAME_ALPHA = "Alpha";
    public static final String NAME_BRAVO = "Bravo";
    public static final String[] NAMES = { NAME_ALPHA, NAME_BRAVO, "Charlie", "Delta", "Echo", "Foxtrot", "Golf",
            "Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo",
            "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu" };
    public static final String TAG_RED = "red";
    public static final String TAG_BLUE = "blue";
    public static final String[] TAGS = { TAG_RED, "green", TAG_BLUE, "yellow", "black", "white", "orange", "purple",
            "pink", "brown" };
    public static final String SUBJECT_READ_STANDARD = "read_standard";
    public static final String SUBJECT_READ_SENSITIVE = "read_sensitive";
    public static final String SUBJECT_WRITE_SENSITIVE = "write_sensitive";


    private final List<DataRecord> records;
    private final Map<String, Tag> tags;
    private final List<AccessEntry> accessEntries;
    private final SecurityClass[] securityClasses;

    @Builder.Default
    private String[] recordNames = NAMES;
    @Builder.Default
    private String[] tagNames = TAGS;
    @Builder.Default
    private int recordCount = 100;

    public static class TestDataBuilder {

        public TestDataBuilder tag( final Tag t ) {
            if ( this.tags == null ) {
                this.tags = Map.of();
            }
            this.tags.put( t.getName(), t );
            return this;
        }

        public TestDataBuilder recordNames( final String... names ) {
            this.recordNames$value = names;
            this.recordNames$set = names != null && names.length > 0;
            return this;
        }

        public TestDataBuilder tagNames( final String... names ) {
            this.tagNames$value = names;
            this.tagNames$set = names != null && names.length > 0;
            return this;
        }
    }

    public static class CustomTestDataBuilder extends TestDataBuilder {
        public TestData build() {
            if ( isEmpty( super.tags ) ) {
                final var names = super.tagNames$set ? super.tagNames$value : TAGS;
                tags( Arrays.stream( names )
                        .map( tag -> Tag.builder().name( tag )
                                .build() )
                        .collect( Collectors.toMap( Tag::getName, t -> t ) ) );
            }
            if ( super.securityClasses == null ) {
                securityClasses( List.of( SecurityClass.builder().level( 0 ).name( "public" )
                        .build(), SecurityClass.builder().level( 1 ).name( "standard" )
                        .build(), SecurityClass.builder().level( 2 ).name( "confidential" )
                        .build() ).toArray( SecurityClass[]::new ) );
            }
            if ( isEmpty( super.accessEntries ) ) {
                accessEntries( List.of( AccessEntry.builder()
                                .subject( SUBJECT_READ_STANDARD )
                                .action( READ )
                                .securityClass( super.securityClasses[1] )
                                .build(),  //
                        AccessEntry.builder()
                                .subject( SUBJECT_READ_SENSITIVE )
                                .action( READ )
                                .securityClass( super.securityClasses[2] )
                                .build(), //
                        AccessEntry.builder()
                                .subject( SUBJECT_WRITE_SENSITIVE )
                                .action( WRITE )
                                .securityClass( super.securityClasses[2] )
                                .build() ) );
            }
            if ( super.records == null ) {
                super.records( new ArrayList<>( Math.max( super.recordCount$value, 100 ) ) );
            }
            final var testData = super.build();
            if ( testData.records().isEmpty() ) {
                testData.generateRecords();
            }
            return testData;
        }
    }

    public static TestDataBuilder builder() {
        return new CustomTestDataBuilder();
    }

    public TestDataBuilder toBuilder() {
        return new CustomTestDataBuilder().records( this.records )
                .tags( this.tags )
                .accessEntries( this.accessEntries )
                .securityClasses( this.securityClasses )
                .recordNames( this.recordNames )
                .tagNames( this.tagNames )
                .recordCount( this.recordCount );
    }

    public TestData generateRecords( final int count, final Consumer<DataRecord.DataRecordBuilder> consumer ) {
        Objects.requireNonNull( this.securityClasses );
        for ( int i = 0; i < count; ++i ) {
            final var created = Instant.parse( "1999-01-02T10:15:30.00Z" ).plus( i, ChronoUnit.DAYS );
            final var builder = DataRecord.builder()
                    .name( nextName( i ) )
                    .securityClass( securityClasses[i % securityClasses.length] )
                    .auditInfo( AuditInfo.create( created, created.plus( 10, ChronoUnit.MINUTES ) ) )
                    .status( nextStatus( i ) )
                    .tags( someTags( i ) );
            consumer.accept( builder );
            records.add( builder.build() );
        }
        log.debug( "Number of test records created: {} total count: {}", count, records.size() );
        return this;
    }

    public static TestData create( final Consumer<TestDataBuilder> consumer ) {
        final var builder = TestData.builder();
        consumer.accept( builder );
        return builder.build();
    }

    public void generateRecords() {
        generateRecords( recordCount );
    }

    public TestData generateRecords( final int count ) {
        return generateRecords( count, b -> {
            // no-op, just to use the method signature
        } );
    }

    public TestData generateRecords( final Consumer<DataRecord.DataRecordBuilder> consumer ) {
        return generateRecords( recordCount, consumer );
    }

    private Set<Tag> someTags( final int i ) {
        final List<Tag> tagList = new ArrayList<>( tags.values() );
        return i % 10 == 0 ? Set.of()
                           : Set.of( tagList.get( i % tagList.size() ), tagList.get( (i + 1) % tags.size() ) );
    }

    private static Status nextStatus( final int i ) {
        return Status.values()[i % Status.values().length];
    }

    private String nextName( final int i ) {
        return NAMES[i % NAMES.length];
    }
}
