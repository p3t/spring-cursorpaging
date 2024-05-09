package io.vigier.cursorpaging.testapp.api.controller;

import static io.vigier.cursorpaging.testapp.api.controller.DataRecordController.PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.serializer.Base64String;
import io.vigier.cursorpaging.jpa.serializer.EntitySerializer;
import io.vigier.cursorpaging.testapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursorpaging.testapp.model.DataRecord;
import io.vigier.cursorpaging.testapp.repository.DataRecordRepository;
import java.util.Base64;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest( DataRecordController.class )
//@ExtendWith( MockitoExtension.class )
class DataRecordControllerTest {

    private static final String PATH_COUNT = PATH + DataRecordController.COUNT;

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
        mockMvc.perform( get( PATH ).param( "pageSize", "1000" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.detail" ).value( "Validation failure" ) );
    }

    @Test
    void shouldValidateCursor() throws Exception {
        mockMvc.perform( get( PATH ) //
                        .param( "pageSize", "10" ) //
                        .param( "cursor", "%&/$RT5" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.detail" ).value( Matchers.containsString( "Failed to convert 'cursor'" ) ) );
    }

    @Test
    void shouldReturnTotalCountWhenNoCursorProvided() throws Exception {
        when( dataRecordRepository.count() ).thenReturn( 4711L );
        mockMvc.perform( get( PATH_COUNT ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.totalElements" ).value( 4711 ) );
    }

    @Test
    void shouldReturnCountUsingCursor() throws Exception {
        final var pageRequest = PageRequest.<DataRecord>create( b -> b.asc( Attribute.of( "id", UUID.class ) ) );
        final var encoded = Base64.getUrlEncoder().encode( "TEST".getBytes( UTF_8 ) );
        when( serializer.toPageRequest( any( Base64String.class ) ) ).thenReturn( pageRequest );
        when( dataRecordRepository.count( pageRequest ) ).thenReturn( 4711L );

        mockMvc.perform( get( PATH_COUNT ) //
                        .param( "cursor", new String( encoded, UTF_8 ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.totalElements" ).value( 4711 ) );
    }
}