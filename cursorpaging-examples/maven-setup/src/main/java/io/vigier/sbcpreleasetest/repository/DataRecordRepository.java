package io.vigier.cursorpaging.testapp.repository;

import io.vigier.cursorpaging.jpa.repository.CursorPageRepository;
import io.vigier.cursorpaging.testapp.model.DataRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {

}
