package io.vigier.cursorpaging.testapp.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vigier.cursorpaging.jpa.serializer.EntitySerializer;
import io.vigier.cursorpaging.testapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursorpaging.testapp.model.DataRecord;
import io.vigier.cursorpaging.testapp.repository.DataRecordRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest( DataRecordController.class )
//@ExtendWith( MockitoExtension.class )
class DataRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataRecordRepository dataRecordRepository;

    @MockBean
    private DtoDataRecordMapper dtoDataRecordMapper;

    @MockBean
    private EntitySerializer<DataRecord> serializer;

    @Test
    void shouldValidateMaxPageSize() throws Exception {
        mockMvc.perform( get( DataRecordController.PATH ).param( "pageSize", "1000" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.detail" ).value( "Validation failure" ) );
    }

    @Test
    void shouldValidateCursor() throws Exception {
        mockMvc.perform( get( DataRecordController.PATH ) //
                        .param( "pageSize", "10" ) //
                        .param( "cursor", "%&/$RT5" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.detail" ).value( Matchers.containsString( "Failed to convert 'cursor'" ) ) );
    }

}