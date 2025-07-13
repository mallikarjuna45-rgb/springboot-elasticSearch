package com.UndoSchool.course_search.service;

import com.UndoSchool.course_search.document.CourseDocument;
import com.UndoSchool.course_search.repository.CourseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @SneakyThrows
    @Override
    public void run(String... args) {
        var indexOps = elasticsearchOperations.indexOps(CourseDocument.class);

        // Delete existing index (clean setup)
        if (indexOps.exists()) {
            indexOps.delete();
        }

        // Create index
        indexOps.create();

        // Manually define suggest mapping as "completion"
        Map<String, Object> mapping = Map.of(
                "properties", Map.of(
                        "suggest", Map.of(
                                "type", "completion",
                                "analyzer", "simple",
                                "preserve_separators", true,
                                "preserve_position_increments", true,
                                "max_input_length", 100
                        )
                )
        );
        indexOps.putMapping(Document.from(mapping));

        // Load JSON (without 'suggest' field)
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sample-courses.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        List<CourseDocument> courses = mapper.readValue(inputStream, new TypeReference<>() {});

        for (CourseDocument course : courses) {
            List<String> inputs = buildSuggestInputs(course.getTitle());
            course.setSuggest(new Completion(inputs));
            System.out.println("Indexing: " + course.getTitle() + " -> " + inputs);
        }



        courseRepository.saveAll(courses);
    }

    private List<String> buildSuggestInputs(String title) {
        Set<String> inputs = new LinkedHashSet<>();
        inputs.add(title); // full title

        for (String word : title.split("\\s+")) {
            String clean = word.trim();
            if (!clean.isEmpty()) {
                inputs.add(clean);
                if (clean.length() >= 3) {
                    inputs.add(clean.substring(0, 3).toLowerCase());
                }
            }
        }

        return new ArrayList<>(inputs);
    }
}
