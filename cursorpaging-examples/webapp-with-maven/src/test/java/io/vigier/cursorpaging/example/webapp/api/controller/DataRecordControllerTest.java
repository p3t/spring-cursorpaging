package io.vigier.cursorpaging.example.webapp.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vigier.cursorpaging.example.webapp.api.model.mapper.DtoDataRecordMapper;
import io.vigier.cursorpaging.example.webapp.model.DataRecord;
import io.vigier.cursorpaging.example.webapp.model.DataRecord_;
import io.vigier.cursorpaging.example.webapp.repository.DataRecordRepository;
import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.PageRequest;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoAndFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoEqFilter;
import io.vigier.cursorpaging.jpa.serializer.Base64String;
import io.vigier.cursorpaging.jpa.serializer.RequestSerializer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static io.vigier.cursorpaging.example.webapp.api.controller.DataRecordController.PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataRecordController.class)
// @ExtendWith( MockitoExtension.class )
@Slf4j
class DataRecordControllerTest {

        private static final String PATH_COUNT = PATH + DataRecordController.COUNT;
        private static final String CURSOR = "OW2T2rDudgONjtP04KHOUguDTrUGTCA7edByRquqlqus1TaSdcr1JwMLSwiDcW88hp7zSMqJrn9Q-W94P1GFGMuAQNeWfMZ5vfK6Mf712w";
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DataRecordRepository dataRecordRepository;

        @MockitoBean
        private DtoDataRecordMapper dtoDataRecordMapper;

        @MockitoBean
        private RequestSerializer<DataRecord> serializer;

        @Test
        void shouldValidateMaxPageSize() throws Exception {
                mockMvc.perform(get(PATH).param("pageSize", "1000"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("Validation failure"));
        }

        @Test
        void shouldValidateCursor() throws Exception {
                mockMvc.perform(get(PATH) //
                                .param("pageSize", "10") //
                                .param("cursor", "%&/$RT5"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail")
                                                .value(Matchers.containsString("Failed to convert 'cursor'")));
        }

        @Test
        void shouldReturnTotalCountWhenNoCursorProvided() throws Exception {
                when(dataRecordRepository.count()).thenReturn(4711L);
                mockMvc.perform(get(PATH_COUNT))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(4711));
        }

        @Test
        void shouldReturnCountUsingCursor() throws Exception {
                final var pageRequest = PageRequest.<DataRecord>create(b -> b.asc(Attribute.of("id", UUID.class)));
                final var encoded = Base64.getUrlEncoder().encode("TEST".getBytes(UTF_8));
                when(serializer.toPageRequest(any(Base64String.class))).thenReturn(pageRequest);
                when(dataRecordRepository.count(pageRequest)).thenReturn(4711L);

                mockMvc.perform(get(PATH_COUNT) //
                                .param("cursor", new String(encoded, UTF_8)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(4711));
        }

        @Test
        void shouldCreateNewCursorOnPost() throws Exception {
                final var request = DtoPageRequest.builder()
                                .filterBy(DtoAndFilter.builder()
                                                .filter(DtoEqFilter.builder()
                                                                .attribute(DataRecord_.NAME)
                                                                .values(List.of("Tango", "Bravo"))
                                                                .build())
                                                .build())
                                .orderBy(Map.of(DataRecord_.NAME, Order.ASC))
                                .pageSize(10)
                                .build();
                final String json = new ObjectMapper().writeValueAsString(request);
                log.debug("Json:, {}", json);

                when(serializer.toBase64(any())).thenReturn(new Base64String(CURSOR));

                mockMvc.perform(post(PATH + "/page") //
                                .contentType(MediaType.APPLICATION_JSON) //
                                .content(json)) //
                                .andExpect(status().isCreated()) //
                                .andExpect(jsonPath("$.orderBy.name").value("ASC"))
                                .andExpect(jsonPath("$.orderBy.id").value("ASC"))
                                .andExpect(jsonPath("$.filterBy.AND").isArray())
                                .andExpect(jsonPath("$.filterBy.AND[0].EQ.name").isArray())
                                .andExpect(jsonPath("$.filterBy.AND[0].EQ.name[0]").value("Tango"))
                                .andExpect(jsonPath("$.filterBy.AND[0].EQ.name[1]").value("Bravo"))
                                .andExpect(jsonPath("$.pageSize").value(10)) //
                                .andExpect(jsonPath("$._links.first.href").exists())
                                .andExpect(jsonPath("$._links.first.href").value(Matchers.containsString(CURSOR)));
        }
}