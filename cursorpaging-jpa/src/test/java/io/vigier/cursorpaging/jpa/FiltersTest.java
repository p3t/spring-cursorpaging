package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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

    @Mock
    Predicate likePredicate;

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    CriteriaBuilder criteriaBuilder;

    @BeforeEach
    void setup() {
        lenient().when( name.getName() )
                .thenReturn( "name" );
        lenient().when( name.getJavaType() )
                .thenReturn( String.class );
        DataRecord_.name = name;
        DataRecord_.tags = tags;
        Tag_.name = tagName;
    }

    @Test
    void shouldGenerateEqualsFilter() {
        final var filter = Filters.attribute( DataRecord_.name )
                .equalTo( "Test" );
        assertThat( filter.attributes() ).contains( Attribute.of( name ) );
        assertThat( filter.values()
                .getFirst() ).isEqualTo( "Test" );
    }

    @Test
    void shouldGeneratePathFilterWithIgnoreCase() {
        when( tags.getName() ).thenReturn( "tags" );
        when( tagName.getName() ).thenReturn( "name" );
        when( tagName.getJavaType() ).thenReturn( String.class );

        final List<String> values = List.of( "Test1", "Test2" );
        final var filter = Filters.ignoreCase( DataRecord_.tags, Tag_.name )
                .in( values );
        assertThat( filter.attributes() ).contains( Attribute.of( DataRecord_.tags, Tag_.name )
                .withIgnoreCase() );

        assertThat( filter.values() ).hasSize( values.size() );
        assertThat( filter.values( String.class ) ).containsAll( values );
    }

    @Test
    void shouldGenerateLikeFilter() {
        final var filter = Filters.attribute( DataRecord_.name )
                .like( "Test%" );
        assertThat( filter.operation() ).isEqualTo( FilterType.LIKE );
        assertThat( filter.attributes() ).contains( Attribute.of( name ) );
        assertThat( filter.values( String.class ) ).containsExactly( "Test%" );
    }

    @Test
    void shouldGenerateLikeFilterWithMultiplePatterns() {
        final var filter = Filters.attribute( DataRecord_.name )
                .like( "Test%", "%Data" );
        assertThat( filter.operation() ).isEqualTo( FilterType.LIKE );
        assertThat( filter.values( String.class ) ).containsExactly( "Test%", "%Data" );
    }

    @Test
    void shouldGenerateLikeFilterFromValueList() {
        final List<String> patterns = List.of( "Test%", "%Data" );
        final var filter = Filters.attribute( DataRecord_.name )
                .like( patterns );
        assertThat( filter.operation() ).isEqualTo( FilterType.LIKE );
        assertThat( filter.values( String.class ) ).containsExactlyElementsOf( patterns );
    }

    @Test
    void shouldGenerateLikeFilterWithIgnoreCase() {
        final var filter = Filters.ignoreCase( DataRecord_.name )
                .like( "test%" );
        assertThat( filter.operation() ).isEqualTo( FilterType.LIKE );
        assertThat( filter.attributes() ).contains( Attribute.of( name )
                .withIgnoreCase() );
        assertThat( filter.values( String.class ) ).containsExactly( "test%" );
    }

    @Test
    void shouldGenerateNotLikeFilter() {
        final var filter = Filters.attribute( DataRecord_.name )
                .notLike( "Test%" );
        assertThat( filter.operation() ).isEqualTo( FilterType.NOT_LIKE );
        assertThat( filter.attributes() ).contains( Attribute.of( name ) );
        assertThat( filter.values( String.class ) ).containsExactly( "Test%" );
    }

    @Test
    void shouldGenerateNotLikeFilterWithMultiplePatterns() {
        final var filter = Filters.attribute( DataRecord_.name )
                .notLike( "Test%", "%Data" );
        assertThat( filter.operation() ).isEqualTo( FilterType.NOT_LIKE );
        assertThat( filter.values( String.class ) ).containsExactly( "Test%", "%Data" );
    }

    @Test
    void shouldGenerateNotLikeFilterFromValueList() {
        final List<String> patterns = List.of( "Test%", "%Data" );
        final var filter = Filters.attribute( DataRecord_.name )
                .notLike( patterns );
        assertThat( filter.operation() ).isEqualTo( FilterType.NOT_LIKE );
        assertThat( filter.values( String.class ) ).containsExactlyElementsOf( patterns );
    }

    /**
     * Escape sequences are not interpreted by the filter-API, they are part of the pattern and are passed on to the
     * database as they are given.
     */
    @Test
    void shouldKeepEscapedWildcardsInLikePatterns() {
        final var pattern = "50\\% of\\_all\\\\";
        assertThat( Filters.attribute( DataRecord_.name )
                .like( pattern )
                .values( String.class ) ) //
                .containsExactly( pattern );
        assertThat( Filters.attribute( DataRecord_.name )
                .notLike( pattern )
                .values( String.class ) ) //
                .containsExactly( pattern );
    }

    @Test
    void shouldPassLikePatternUnchangedToTheQueryBuilder() {
        final var pattern = "50\\% of\\_all";
        when( queryBuilder.isLike( any(), any() ) ).thenReturn( predicate );

        final var filter = Filters.attribute( DataRecord_.name )
                .like( pattern );
        assertThat( filter.toPredicate( queryBuilder ) ).isSameAs( predicate );

        final var patternCaptor = ArgumentCaptor.forClass( String.class );
        verify( queryBuilder ).isLike( eq( Attribute.of( name ) ), patternCaptor.capture() );
        assertThat( patternCaptor.getValue() ).isEqualTo( pattern );
    }

    @Test
    void shouldNegateTheLikePredicateForNotLike() {
        when( queryBuilder.isLike( any(), any() ) ).thenReturn( likePredicate );
        when( queryBuilder.cb() ).thenReturn( criteriaBuilder );
        when( criteriaBuilder.not( likePredicate ) ).thenReturn( predicate );

        final var filter = Filters.attribute( DataRecord_.name )
                .notLike( "Test%" );
        assertThat( filter.toPredicate( queryBuilder ) ).isSameAs( predicate );

        verify( queryBuilder ).isLike( Attribute.of( name ), "Test%" );
    }

    @Test
    void shouldAcceptNullAsFilterValueList() {
        final List<Comparable<?>> nullList = null;
        final var filter = Filters.attribute( DataRecord_.name )
                .equalTo( nullList );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterValue() {
        final var filter = Filters.attribute( DataRecord_.name )
                .equalTo( nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterInValues() {
        final var filter = Filters.attribute( DataRecord_.name )
                .in( nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsFilterMultipleValues() {
        final var filter = Filters.attribute( DataRecord_.name )
                .in( nullComparable(), nullComparable() );
        assertThat( filter.isEmpty() ).isTrue();
    }

    @Test
    void shouldAcceptNullAsLikePatternList() {
        final List<Comparable<?>> nullList = null;
        assertThat( Filters.attribute( DataRecord_.name )
                .like( nullList )
                .isEmpty() ).isTrue();
        assertThat( Filters.attribute( DataRecord_.name )
                .notLike( nullList )
                .isEmpty() ).isTrue();
    }

    private Comparable<?> nullComparable() {
        return null;
    }
}
