package io.vigier.cursorpaging.jpa.filter;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Filters;
import io.vigier.cursorpaging.jpa.Filters.FilterCreator;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Some test to make sure that comparing by equals is safe in other tests
 */
class FilterTest {

    @Test
    void shouldBeEqual() {
        shouldBeEqual( f1 -> f1.like( "John" ), f2 -> f2.like( "John" ) );
        shouldBeEqual( f1 -> f1.equalTo( "John" ), f2 -> f2.equalTo( "John" ) );
        shouldBeEqual( f1 -> f1.greaterThan( 1L ), f2 -> f2.greaterThan( 1L ) );
        shouldBeEqual( f1 -> f1.lessThan( 1L ), f2 -> f2.lessThan( 1L ) );
    }

    private void shouldBeEqual( Function<FilterCreator, Filter> f1, Function<FilterCreator, Filter> f2 ) {
        var name1 = Attribute.of( "name", String.class );
        var name2 = Attribute.of( "name", String.class );

        Assertions.assertThat( f1.apply( Filters.attribute( name1 ) ) )
                  .isEqualTo( f2.apply( Filters.attribute( name2 ) ) );
    }

    @Test
    void filterListsShouldBeEqual() {
        var nameAndAge1 = Filters.and( Filters.attribute( Attribute.of( "name", String.class ) ).equalTo( "John" ),
                Filters.attribute( Attribute.of( "age", Long.class ) ).equalTo( 18 ) );
        var nameAndAge2 = Filters.and( Filters.attribute( Attribute.of( "name", String.class ) ).equalTo( "John" ),
                Filters.attribute( Attribute.of( "age", Long.class ) ).equalTo( 18 ) );

        Assertions.assertThat( nameAndAge1 ).isEqualTo( nameAndAge2 );
        Assertions.assertThat( Filters.and( nameAndAge1 ) ).isEqualTo( Filters.and( nameAndAge2 ) );
        Assertions.assertThat( Filters.or( nameAndAge1 ) ).isEqualTo( Filters.or( nameAndAge2 ) );

        Assertions.assertThat( Filters.or( nameAndAge1 ) ).isNotEqualTo( Filters.and( nameAndAge2 ) );
    }
}