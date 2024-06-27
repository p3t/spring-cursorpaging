package io.vigier.cursorpaging.jpa.itest.repository;

import io.vigier.cursorpaging.jpa.itest.model.AccessEntry;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessEntryRepository extends JpaRepository<AccessEntry, UUID> {

    default AccessEntry saveEntry( final Consumer<AccessEntry.AccessEntryBuilder> c ) {
        final AccessEntry.AccessEntryBuilder b = AccessEntry.builder();
        c.accept( b );
        return save( b.build() );
    }

}
