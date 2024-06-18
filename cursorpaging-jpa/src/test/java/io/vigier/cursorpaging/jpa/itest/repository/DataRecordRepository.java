package io.vigier.cursorpaging.jpa.itest.repository;

import io.vigier.cursorpaging.jpa.itest.model.DataRecord;
import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {

}
