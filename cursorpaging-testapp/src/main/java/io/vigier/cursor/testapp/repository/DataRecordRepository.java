package io.vigier.cursor.testapp.repository;

import io.vigier.cursor.jpa.repository.CursorPageRepository;
import io.vigier.cursor.testapp.model.DataRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, UUID>, CursorPageRepository<DataRecord> {

}
