package io.vigier.cursorpaging.jpa.api;

import io.vigier.cursorpaging.jpa.Attribute;
import io.vigier.cursorpaging.jpa.Filter;
import io.vigier.cursorpaging.jpa.Order;
import io.vigier.cursorpaging.jpa.QueryElement;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoAndFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoEqFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoFilterList;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoGtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLikeFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoLtFilter;
import io.vigier.cursorpaging.jpa.api.DtoPageRequest.DtoOrFilter;
import io.vigier.cursorpaging.jpa.filter.FilterType;
import io.vigier.cursorpaging.jpa.filter.OrFilter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class DtoPageRequestTest {

    @Test
    void shouldDeserializeFromJson() throws Exception {
        final String json = """
                {
                    "orderBy": {
                        "id": "ASC"
                    },
                    "filterBy": {
                        "AND": [
                            {
                                "GT": {
                                    "id": [
                                        "666"
                                    ]
                                }
                            }
                        ]
                    },
                    "pageSize": 10,
                    "withTotalCount": false
                }
                """;

        final var mapper = JsonMapper.builder()
                .build();
        final DtoPageRequest request = mapper.readValue(json, DtoPageRequest.class);
        assertThat(request.getOrderBy()).containsExactly(Map.entry("id", Order.ASC));
        assertThat(request.getFilterBy()).isNotNull().satisfies(fl -> {
            assertThat(fl).isInstanceOf(DtoAndFilter.class);
            assertThat(((DtoFilterList) fl).getFilters()).hasSize(1).first().satisfies(gtf -> {
                assertThat(gtf).isInstanceOf(DtoGtFilter.class);
                assertThat(((DtoGtFilter) gtf).getAttribute()).isEqualTo("id");
                assertThat(((DtoGtFilter) gtf).getValues()).contains("666");
            });
        });
        assertThat(request.getPageSize()).isEqualTo(10);

    }

    @Test
    void shouldDeserializeWithOrRootList() {
        final String json = """
                {
                    "orderBy": {
                        "id": "ASC"
                    },
                    "filterBy": {
                        "OR": [
                            { "GT": { "id": [ 666 ] } },
                            { "GE": { "id": [ 666 ] } }
                        ]
                    },
                    "pageSize": 10,
                    "withTotalCount": false
                }
                """;
        final var mapper = JsonMapper.builder()
                .build();
        final DtoPageRequest request = mapper.readValue(json, DtoPageRequest.class);
        assertThat(request.getOrderBy()).containsExactly(Map.entry("id", Order.ASC));
        assertThat(request.getFilterBy()).isNotNull().satisfies(fl -> {
            assertThat(fl).isInstanceOf(DtoOrFilter.class);
            assertThat(((DtoFilterList) fl).getFilters()).hasSize(2);
        });

        final var pageRequest = request.toPageRequest(DtoPageRequestTest::getAttribute);
        assertThat(pageRequest.filters()).hasSize(2).isInstanceOf(OrFilter.class).satisfies(orFilter -> {
            final var iterator = orFilter.iterator();
            assertThat(iterator.next()).satisfies(f -> operationIs(f, FilterType.GREATER_THAN));
            assertThat(iterator.next()).satisfies(f -> operationIs(f, FilterType.GREATER_THAN_OR_EQUAL_TO));
        });
    }

    private static void operationIs(final QueryElement f, final FilterType type) {
        assertThat(((Filter) f).operation()).isEqualTo(type);
    }

    @Test
    void shouldSerializeDtoPageRequestsToJson() {
        final var request = DtoPageRequest.builder()
                .pageSize(10)
                .orderBy(Map.of("id", Order.ASC))
                .filterBy(DtoAndFilter.builder()
                        .filter(DtoGtFilter.builder().attribute("id").value("666")
                                .build())
                        .filter(DtoOrFilter.builder()
                                .filter(DtoEqFilter.builder().attribute("super").value("true")
                                        .build())
                                .filter(DtoLikeFilter.builder().attribute("name").value("4711")
                                        .build())
                                .filter(DtoLtFilter.builder().attribute("priority").value("0815")
                                        .build())
                                .build())
                        .build())
                .build();

        final var jsonMapper = JsonMapper.builder()
                .build();
        final var json = jsonMapper.writeValueAsString(request);
        log.info(json);
        final var nodes = jsonMapper.readTree(json);
        assertThat(nodes.get("pageSize").intValue()).isEqualTo(10);
        assertThat(nodes.get("orderBy").get("id").stringValue()).isEqualTo("ASC");
        final var filterBy = nodes.get("filterBy");
        assertThat(filterBy.get("AND")).isNotNull();
        final var andArray = filterBy.get("AND");
        assertThat(andArray).hasSize(2);
    }

    @Test
    void shouldGenerateValidPageRequests() {
        final var request = DtoPageRequest.builder()
                .pageSize(10)
                .orderBy(Map.of("id", Order.ASC))
                .filterBy(DtoAndFilter.builder()
                        .filter(DtoGtFilter.builder().attribute("id").value("666")
                                .build())
                        .filter(DtoOrFilter.builder()
                                .filter(DtoEqFilter.builder().attribute("super").value("true")
                                        .build())
                                .filter(DtoLikeFilter.builder().attribute("name").value("4711")
                                        .build())
                                .filter(DtoLtFilter.builder().attribute("priority").value("0815")
                                        .build())
                                .build())
                        .build())
                .build();

        final var pageRequest = request.toPageRequest(DtoPageRequestTest::getAttribute);

        assertThat(pageRequest.pageSize()).isEqualTo(10);
        assertThat(pageRequest.filters()).hasSize(2);
        assertThat(pageRequest.filters().filters().get(0)).satisfies(
                f -> operationIs(f, FilterType.GREATER_THAN));
        assertThat(pageRequest.filters().filters().get(1)).isInstanceOf(OrFilter.class).satisfies(of -> {
            assertThat(((OrFilter) of).filters()).hasSize(3);
            assertThat(((OrFilter) of).filters().get(0)).satisfies(f -> operationIs(f, FilterType.EQUAL_TO));
            assertThat(((OrFilter) of).filters().get(1)).satisfies(f -> operationIs(f, FilterType.LIKE));
            assertThat(((OrFilter) of).filters().get(2)).satisfies(f -> operationIs(f, FilterType.LESS_THAN));
        });
    }

    private static Attribute getAttribute(final String s) {
        return switch (s) {
            case "id" -> Attribute.of("id", Long.class);
            case "super" -> Attribute.of("super", Boolean.class);
            case "name" -> Attribute.of("name", String.class);
            case "priority" -> Attribute.of("priority", Integer.class);
            default -> throw new IllegalArgumentException("Unknown attribute: " + s);
        };
    }
}