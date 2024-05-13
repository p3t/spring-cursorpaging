package io.vigier.sbcpreleasetest.repository;

import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import io.vigier.sbcpreleasetest.model.DataRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {

}
