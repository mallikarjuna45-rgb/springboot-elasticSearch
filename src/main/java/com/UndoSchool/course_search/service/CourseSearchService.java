package com.UndoSchool.course_search.service;

import com.UndoSchool.course_search.document.CourseDocument;
import com.UndoSchool.course_search.dto.CourseSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchOperations elasticsearchTemplate;

    public Page<CourseDocument> searchCourses(CourseSearchRequest request) {
        Criteria criteria = new Criteria();

        // Keyword search on title or description
        if (StringUtils.hasText(request.getKeyword())) {
            criteria = criteria.or(new Criteria("title").matches(request.getKeyword()))
                    .or(new Criteria("description").matches(request.getKeyword()));
        }

        // Exact filters
        if (StringUtils.hasText(request.getCategory())) {
            criteria = criteria.and(new Criteria("category").is(request.getCategory()));
        }
        if (StringUtils.hasText(request.getType())) {
            criteria = criteria.and(new Criteria("type").is(request.getType()));
        }

        // Range filters
        if (request.getMinAge() != null || request.getMaxAge() != null) {
            Criteria ageCriteria = new Criteria("minAge");
            if (request.getMinAge() != null) ageCriteria = ageCriteria.greaterThanEqual(request.getMinAge());
            if (request.getMaxAge() != null) ageCriteria = ageCriteria.lessThanEqual(request.getMaxAge());
            criteria = criteria.and(ageCriteria);
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            Criteria priceCriteria = new Criteria("price");
            if (request.getMinPrice() != null) priceCriteria = priceCriteria.greaterThanEqual(request.getMinPrice());
            if (request.getMaxPrice() != null) priceCriteria = priceCriteria.lessThanEqual(request.getMaxPrice());
            criteria = criteria.and(priceCriteria);
        }

        if (request.getStartDate() != null) {
            criteria = criteria.and(new Criteria("nextSessionDate").greaterThanEqual(request.getStartDate()));
        }

        // Sorting
        Sort sort = Sort.by("nextSessionDate").ascending();
        if ("priceAsc".equalsIgnoreCase(request.getSort())) {
            sort = Sort.by("price").ascending();
        } else if ("priceDesc".equalsIgnoreCase(request.getSort())) {
            sort = Sort.by("price").descending();
        }

        // Pagination
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        // Build query
        CriteriaQuery query = new CriteriaQuery(criteria, pageable);
        query.addSort(sort);

        SearchHits<CourseDocument> hits = elasticsearchTemplate.search(query, CourseDocument.class);
        List<CourseDocument> courses = hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(courses, pageable, hits.getTotalHits());
    }
}
