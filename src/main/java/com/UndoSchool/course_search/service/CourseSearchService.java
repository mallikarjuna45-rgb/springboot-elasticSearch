package com.UndoSchool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.UndoSchool.course_search.document.CourseDocument;
import com.UndoSchool.course_search.dto.CourseSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchClient elasticsearchClient;

    public Page<CourseDocument> searchCourses(CourseSearchRequest request) throws IOException {
        List<Query> must = new ArrayList<>();
        List<Query> should = new ArrayList<>();

        // Keyword match
        if (StringUtils.hasText(request.getKeyword())) {
            should.add(MatchQuery.of(m -> m.field("title").query(request.getKeyword()))._toQuery());
            should.add(MatchQuery.of(m -> m.field("description").query(request.getKeyword()))._toQuery());
        }

        // Filters
        if (StringUtils.hasText(request.getCategory())) {
            must.add(TermQuery.of(t -> t.field("category").value(request.getCategory()))._toQuery());
        }

        if (StringUtils.hasText(request.getType())) {
            must.add(TermQuery.of(t -> t.field("type").value(request.getType()))._toQuery());
        }

        if (request.getMinAge() != null) {
            must.add(RangeQuery.of(r -> r.field("minAge").gte(JsonData.of(request.getMinAge())))._toQuery());
        }

        if (request.getMaxAge() != null) {
            must.add(RangeQuery.of(r -> r.field("minAge").lte(JsonData.of(request.getMaxAge())))._toQuery());
        }

        if (request.getMinPrice() != null) {
            must.add(RangeQuery.of(r -> r.field("price").gte(JsonData.of(request.getMinPrice())))._toQuery());
        }

        if (request.getMaxPrice() != null) {
            must.add(RangeQuery.of(r -> r.field("price").lte(JsonData.of(request.getMaxPrice())))._toQuery());
        }

        if (request.getStartDate() != null) {
            must.add(RangeQuery.of(r -> r
                    .field("nextSessionDate")
                    .gte(JsonData.of(request.getStartDate().toEpochMilli()))
            )._toQuery());
        }

        // Build final bool query
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (!must.isEmpty()) boolBuilder.must(must);
        if (!should.isEmpty()) boolBuilder.should(should).minimumShouldMatch("1");

        Query finalQuery = Query.of(q -> q.bool(boolBuilder.build()));

        // Sorting
        final String sortField;
        final SortOrder sortOrder;
        if ("priceAsc".equalsIgnoreCase(request.getSort())) {
            sortField = "price";
            sortOrder = SortOrder.Asc;
        } else if ("priceDesc".equalsIgnoreCase(request.getSort())) {
            sortField = "price";
            sortOrder = SortOrder.Desc;
        } else {
            sortField = "nextSessionDate";
            sortOrder = SortOrder.Asc;
        }

        SearchResponse<CourseDocument> response = elasticsearchClient.search(s -> s
                        .index("courses")
                        .from(request.getPage() * request.getSize())
                        .size(request.getSize())
                        .query(finalQuery)
                        .sort(so -> so
                                .field(f -> f
                                        .field(sortField)
                                        .order(sortOrder)
                                )
                        ),
                CourseDocument.class
        );

        List<CourseDocument> content = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        assert response.hits().total() != null;
        return new PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize()),
                response.hits().total().value()
        );
    }

    public List<String> suggestTitles(String prefix) throws IOException {
        var response = elasticsearchClient.search(s -> s
                        .index("courses")
                        .suggest(sg -> sg
                                .suggesters("title-suggest", suggester -> suggester
                                        .prefix(prefix)
                                        .completion(c -> c
                                                .field("suggest")
                                                .skipDuplicates(false)
                                                .size(10)
                                        )
                                )
                        ),
                CourseDocument.class
        );

        return response.suggest()
                .get("title-suggest").stream()
                .flatMap(suggestion -> suggestion.completion().options().stream())
                .map(option -> {
                    CourseDocument doc = option.source();
                    return doc != null ? doc.getTitle() : option.text(); // use _source.title
                })
                .distinct()
                .toList();
    }

}
