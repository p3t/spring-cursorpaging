package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class FiltersTest {

    @Mock
    private SingularAttribute<DataRecord, String> name;

    @Mock
    private SetAttribute<DataRecord, Tag> tags;

    @Mock
    private SingularAttribute<Tag, String> tagName;

    @Mock
    Predicate predicate;

    @BeforeEach
    void setup() {
        lenient().when( name.getName() ).thenReturn( "name" );
        lenient().when( name.getJavaType() ).thenReturn( String.class );
        DataRecord_.name = name;
        DataRecord_.tags = tags;
        Tag_.name = tagName;
    }

    @Test
    void shouldGenerateEqualsFilter() {
        final var filter = Filters.attribute( DataRecord_.name ).equalTo( "Test" );
        assertThat( filter.attributes() ).contains( Attribute.of( name ) );
        assertThat( filter.values().getFirst() ).isEqualTo( "Test" );
    }

    @Test
    void shouldGeneratePathFilterWithIgnoreCase() {
        when( tags.getName() ).thenReturn( "tags" );
        when( tagName.getName() ).thenReturn( "name" );
        when( tagName.getJavaType() ).thenReturn( String.class );

        final List<String> values = List.of( "Test1", "Test2" );
        final var filter = Filters.ignoreCase( DataRecord_.tags, Tag_.name ).in( values );
        assertThat( filter.attributes() ).contains( Attribute.of( DataRecord_.tags, Tag_.name ).withIgnoreCase() );

        assertThat( filter.values() ).hasSize( values.size() );
        assertThat( filter.values( String.class ) ).containsAll( values );
    }

    @Test
    void shouldAcceptNullAsFilterValueList() {
        final List<Comparable<?>> nullList = null;
        final var filter = Filters.attribute( DataRecord_.name ).equalTo( nullList );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterValue() {
        final var filter = Filters.attribute( DataRecord_.name ).equalTo( nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterInValues() {
        final var filter = Filters.attribute( DataRecord_.name ).in( nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterMultipleValues() {
        final var filter = Filters.attribute( DataRecord_.name ).in( nullComparable(), nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    private Comparable<?> nullComparable() {
        return null;
    }
}