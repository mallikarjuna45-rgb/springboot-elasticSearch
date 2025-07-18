package com.UndoSchool.course_search.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;

@Data
public class CourseSearchRequest {
    private String keyword;
    private Integer minAge;
    private Integer maxAge;
    private Double minPrice;
    private Double maxPrice;
    private String category;
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Instant startDate;
    private String sort; // "priceAsc", "priceDesc"
    private int page = 0;
    private int size = 10;
}
