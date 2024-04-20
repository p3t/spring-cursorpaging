package io.vigier.cursorpaging.jpa.serial;


import io.vigier.cursor.PageRequest;
import io.vigier.cursorpaging.jpa.serial.dto.DtoCursor.DtoPageRequest;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Collection;
import java.util.LinkedList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor( staticName = "of" )
public class Serializer<E> {

    private final Class<E> entityType;
    private final Collection<SingularAttribute<E, ? extends Comparable<?>>> attributes = new LinkedList<>();

    private final Encrypter encrypter;

    public Serializer<E> use( final SingularAttribute<E, ? extends Comparable<?>> attribute ) {
        attributes.add( attribute );
        return this;
    }

    public byte[] toBytes( final PageRequest<E> page ) {
        final DtoPageRequest dtoRequest = ToDtoMapper.of( page ).map();
        return encrypter.encrypt( dtoRequest.toByteArray() );
    }

    @SneakyThrows
    public PageRequest<E> toPageRequest( final byte[] data ) {
        final var request = DtoPageRequest.parseFrom( encrypter.decrypt( data ) );
        return FromDtoMapper.<E>of( request ).using( attributes ).map();
    }
}
