package com.UndoSchool.course_search.controller;


import com.UndoSchool.course_search.dto.CourseSearchRequest;
import com.UndoSchool.course_search.document.CourseDocument;
import com.UndoSchool.course_search.service.CourseSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseSearchController {

    private final CourseSearchService courseSearchService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false, defaultValue = "upcoming") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) throws IOException {
        CourseSearchRequest request = new CourseSearchRequest();
        request.setKeyword(q);
        request.setMinAge(minAge);
        request.setMaxAge(maxAge);
        request.setCategory(category);
        request.setType(type);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setSort(sort);
        request.setPage(page);
        request.setSize(size);

        if (startDate != null && !startDate.isBlank()) {
            request.setStartDate(Instant.parse(startDate));
        }

        Page<CourseDocument> results = courseSearchService.searchCourses(request);

        List<Map<String, Object>> courseList = results.getContent().stream().map(course -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("category", course.getCategory());
            map.put("price", course.getPrice());
            map.put("nextSessionDate", course.getNextSessionDate());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", results.getTotalElements());
        response.put("courses", courseList);

        return ResponseEntity.ok(response);
    }
    @GetMapping("search/suggest")
    public List<String> suggestCourseTitles(@RequestParam("q") String query) throws IOException {
        return courseSearchService.suggestTitles(query);
    }
}
