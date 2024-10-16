package io.vigier.cursorpaging.jpa.itest.repository;

import io.vigier.cursorpaging.jpa.itest.model.SecurityClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityClassRepository extends JpaRepository<SecurityClass, Integer> {

    SecurityClass findByName( String name );
}
