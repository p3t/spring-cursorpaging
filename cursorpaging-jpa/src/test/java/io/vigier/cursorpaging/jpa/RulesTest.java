package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RulesTest {

    @BeforeAll
    static void initJpaMetamodel() {
        JpaModelMock.initDefault();
    }

    @Test
    void shouldCreateRuleWithParameters() {
        final FilterRule rule = Rules.withParameter( "do-it", "true" )
                .where( DataRecord_.tags )
                .withParameter( "yet-another", List.of( "parameter" ) )
                .isEmpty();

        Assertions.assertThat( rule.parameters() )
                .containsEntry( "do-it", List.of( "true" ) )
                .containsEntry( "yet-another", List.of( "parameter" ) );
    }
}
