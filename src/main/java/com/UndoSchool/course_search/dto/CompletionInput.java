package com.UndoSchool.course_search.dto;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.CompletionField;

import java.util.List;

@Data
public class CompletionInput {
    @CompletionField(maxInputLength = 100)
    private List<String> input;
}
