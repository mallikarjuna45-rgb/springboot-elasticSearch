package com.UndoSchool.course_search.service;

import com.UndoSchool.course_search.document.CourseDocument;
import com.UndoSchool.course_search.repository.CourseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CourseRepository courseRepository;

    @Override
    public void run(String... args) throws Exception {
        // Load JSON file from resources
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sample-courses.json");

        // Parse JSON to Java List
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        List<CourseDocument> courses = objectMapper.readValue(inputStream, new TypeReference<>() {});

        // Bulk index to Elasticsearch
        courseRepository.saveAll(courses);
    }
}
