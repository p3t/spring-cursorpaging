package io.vigier.cursorpaging.jpa;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.itest.model.DataRecord_;
import io.vigier.cursorpaging.jpa.itest.model.Tag;
import io.vigier.cursorpaging.jpa.itest.model.Tag_;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;

public class JpaModelMock {

    public static void initDefault() {
        DataRecord_.name = mock( DataRecord.class, "name" ).single( String.class );
        DataRecord_.tags = mock( DataRecord.class, "tags" ).set( Tag.class );
        Tag_.name = mock( Tag.class, "name" ).single( String.class );
    }

    @RequiredArgsConstructor
    public static class MockMaker<X> {
        private final Class<X> entityType;
        private final String name;

        @SuppressWarnings( "unchecked" )
        public <E> ListAttribute<X, E> list( final Class<E> elementType ) {
            return mockPlural( elementType, ListAttribute.class, List.class );
        }

        @SuppressWarnings( "unchecked" )
        public <E> SetAttribute<X, E> set( final Class<E> elementType ) {
            return mockPlural( elementType, SetAttribute.class, Set.class );
        }

        public <T> SingularAttribute<X, T> single( final Class<T> attributeType ) {
            @SuppressWarnings( "unchecked" ) final SingularAttribute<X, T> mock = Mockito.mock(
                    SingularAttribute.class );
            Mockito.lenient().when( mock.getName() ).thenReturn( name );
            Mockito.lenient().when( mock.getJavaType() ).thenReturn( attributeType );
            @SuppressWarnings( "unchecked" ) final ManagedType<X> mangedType = Mockito.mock( ManagedType.class );
            Mockito.lenient().when( mock.getDeclaringType() ).thenReturn( mangedType );
            Mockito.lenient().when( mangedType.getJavaType() ).thenReturn( entityType );
            return mock;
        }

        private <C, E, A extends PluralAttribute<X, C, E>> A mockPlural( final Class<E> elementType,
                final Class<A> attrClass, final Class<C> collClass ) {
            @SuppressWarnings( "unchecked" ) final Type<E> type = Mockito.mock( Type.class );
            Mockito.lenient().when( type.getJavaType() ).thenReturn( elementType );

            final A mock = Mockito.mock( attrClass );
            Mockito.lenient().when( mock.getName() ).thenReturn( name );
            Mockito.lenient().when( mock.getJavaType() ).thenReturn( collClass );

            @SuppressWarnings( "unchecked" ) final ManagedType<X> mangedType = Mockito.mock( ManagedType.class );
            Mockito.lenient().when( mock.getDeclaringType() ).thenReturn( mangedType );
            Mockito.lenient().when( mangedType.getJavaType() ).thenReturn( entityType );

            Mockito.lenient().when( mock.getElementType() ).thenReturn( type );
            return mock;
        }
    }

    public static <X> MockMaker<X> mock( final Class<X> entityType, final String name ) {
        return new MockMaker<X>( entityType, name );
    }

}
