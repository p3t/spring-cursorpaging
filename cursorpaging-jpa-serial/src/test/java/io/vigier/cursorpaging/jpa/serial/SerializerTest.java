package io.vigier.cursorpaging.jpa.serial;

import static org.assertj.core.api.Assertions.assertThat;

import io.vigier.cursor.PageRequest;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SerializerTest {

    private static class TestEntity {

        private String name;
    }

    private static class TestEntity_ {

        public static volatile SingularAttribute<TestEntity, String> name = Mockito.mock( SingularAttribute.class );
    }

    @BeforeAll
    static void setup() {
        Mockito.when( TestEntity_.name.getJavaType() ).thenReturn( String.class );
        Mockito.when( TestEntity_.name.getName() ).thenReturn( "name" );
    }

    @Test
    public void shouldSerializePageRequests() {
        final var pageRequest = PageRequest.firstDesc( TestEntity_.name );

        final var serializer = Serializer.of( TestEntity.class, Encrypter.getInstance() ).use( TestEntity_.name );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );
        
        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }
}
