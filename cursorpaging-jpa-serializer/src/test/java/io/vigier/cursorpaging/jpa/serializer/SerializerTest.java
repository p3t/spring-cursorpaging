package io.vigier.cursorpaging.jpa.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.SingleAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SerializerTest {

    @Data
    private static class TestEntity {

        private String name;
    }

    private static class TestEntity_ {

        @SuppressWarnings( "unchecked" )
        public static volatile SingularAttribute<TestEntity, String> name = Mockito.mock( SingularAttribute.class );
    }

    @BeforeAll
    static void setup() {
        Mockito.when( TestEntity_.name.getJavaType() ).thenReturn( String.class );
        Mockito.when( TestEntity_.name.getName() ).thenReturn( "name" );
    }

    @Test
    public void shouldSerializePageRequests() {
        final PageRequest<TestEntity> pageRequest = PageRequest.firstDesc( TestEntity_.name );

        final EntitySerializer<TestEntity> serializer = EntitySerializer.create( b -> b.use( Attribute.of( TestEntity_.name ) ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );
        
        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldSerializePageRequestsWithMultipleAttributes() {
        final var attribute1 = Attribute.path( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        final var pageRequest = PageRequest.create( b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );

        final var serializer = EntitySerializer.create( b -> b.use( attribute1 ).use( attribute2 ) );
        final var serializedRequest = serializer.toBytes( pageRequest );
        final var deserializeRequest = serializer.toPageRequest( serializedRequest );

        assertThat( deserializeRequest ).isEqualTo( pageRequest );
    }

    @Test
    void shouldLearnAttributesBySerializing() {
        final var request = createPageRequest();
        final EntitySerializer<TestEntity> serializer = EntitySerializer.create();
        final var serializedRequest = serializer.toBase64( request );
        final var deserializedRequest = serializer.toPageRequest( serializedRequest );
        assertThat( deserializedRequest ).isEqualTo( request );
    }

    private PageRequest<TestEntity> createPageRequest() {
        final var attribute1 = Attribute.path( //
                SingleAttribute.of( "one", TestEntity.class ), //
                SingleAttribute.of( "two", Instant.class ) );
        final var attribute2 = Attribute.of( "three", Integer.class );
        return PageRequest.create( b -> b.pageSize( 42 ).asc( attribute1 ).desc( attribute2 ) );
    }

}
