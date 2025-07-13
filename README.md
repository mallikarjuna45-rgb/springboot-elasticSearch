# Course Search Service

A Spring Boot application to search, filter, and autocomplete courses using Elasticsearch. This project offers modern Elasticsearch setup, bulk data ingestion, fuzzy searching, filtering, sorting, and autocompletion for a collection of 50+ sample course documents.

---

## Table of Contents

- [Quickstart](#quickstart)
- [Prerequisites](#prerequisites)
- [Elasticsearch Setup](#elasticsearch-setup)
- [Building and Running the App](#building-and-running-the-app)
- [Sample Data](#sample-data)
- [Elasticsearch Configuration](#elasticsearch-configuration)
- [Course Document Structure](#course-document-structure)
- [Data Ingestion & Verification](#data-ingestion--verification)
- [API Endpoints & Usage](#api-endpoints--usage)
  - [Search](#search)
  - [Filtering & Sorting](#filtering--sorting)
  - [Pagination](#pagination)
  - [Autocomplete (Completion Suggester)](#autocomplete-completion-suggester)
  - [Fuzzy Search](#fuzzy-search)
- [Sample Responses](#sample-responses)
- [More API Endpoint Examples & Outputs](#more-api-endpoint-examples--outputs)
- [Maven Dependencies and Versions](#maven-dependencies-and-versions)


---

## Quickstart

1. **Launch Elasticsearch**

   ```bash
   docker-compose up -d
   ```
   Wait a few seconds for Elasticsearch to be ready.
   Verify with:
   ```bash
   curl http://localhost:9200
   ```
   You should see JSON output with cluster info.

2. **Build and Run the Spring Boot Application**

   Using Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or to build a jar:
   ```bash
   ./mvnw clean package
   java -jar target/course-search-*.jar
   ```

3. **Index Population**

   On application startup, the sample data in `src/main/resources/sample-courses.json` is automatically indexed in Elasticsearch.  
   _No manual steps required!_

4. **Verifying Data**

   Check your application logs for lines like:
   ```
   Indexing: Algebra Basics -> [Algebra Basics, Algebra, alg]
   ```
   Or check the index directly:
   ```bash
   curl 'http://localhost:9200/course/_search?pretty'
   ```

5. **Call the API**

   See [API Endpoints & Usage](#api-endpoints--usage) for example curl requests.

---

## Prerequisites

- **Java:** 21 or newer
- **Docker:** Required for running Elasticsearch locally
- **Maven:** For building and running the Spring Boot application

---

## Elasticsearch Setup

**docker-compose.yml**:
```yaml
version: '3.7'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
```

Start Elasticsearch:

```bash
docker-compose up -d
```

Verify Elasticsearch is running:

```bash
curl http://localhost:9200
```

---

## Building and Running the App

**Build and run:**
```bash
./mvnw spring-boot:run
```
Or build a jar and run:
```bash
./mvnw clean package
java -jar target/course-search-*.jar
```

---

## Sample Data

The repository includes a file with 50+ sample courses:  
`src/main/resources/sample-courses.json`

Example entry:
```json
{
  "id": "course-050",
  "title": "Algebra Basics",
  "description": "A fun and engaging course about Algebra Basics.",
  "category": "Music",
  "type": "CLUB",
  "gradeRange": "2rd–4th",
  "minAge": 8,
  "maxAge": 11,
  "price": 80.08,
  "nextSessionDate": "2025-08-10T00:00:00Z"
}
```

Fields:
- `id`: Unique identifier
- `title`: Short text
- `description`: Longer text
- `category`: E.g., "Math", "Science", "Art"
- `type`: `ONE_TIME` | `COURSE` | `CLUB`
- `gradeRange`: E.g., "1st–3rd"
- `minAge`, `maxAge`: Numeric
- `price`: Decimal
- `nextSessionDate`: ISO-8601 date-time

---

## Elasticsearch Configuration

Example from `application.yml`:
```yaml
spring:
  application:
    name: course-search
  elasticsearch:
    uris: http://localhost:9200
```

---

## Course Document Structure

Elasticsearch documents for courses use the following structure (Java, simplified):

```java
@Id
private String id;

@Field(type = FieldType.Text)
private String title;

@Field(type = FieldType.Text)
private String description;

@Field(type = FieldType.Keyword)
private String category;

@Field(type = FieldType.Keyword)
private CourseType type;

@Field(type = FieldType.Keyword)
private String gradeRange;

@Field(type = FieldType.Integer)
private Integer minAge;

@Field(type = FieldType.Integer)
private Integer maxAge;

@Field(type = FieldType.Double)
private Double price;

@Field(type = FieldType.Date, format = DateFormat.date_time)
private Instant nextSessionDate;

@CompletionField(maxInputLength = 100)
private Completion suggest;
```

---

## Data Ingestion & Verification

On application startup, the `DataLoader` component:
- Deletes the existing index (if any)
- Creates a new index with the right mappings (including the `suggest` field for autocompletion)
- Loads and indexes all courses from `sample-courses.json`
- Generates autocomplete suggestions for each course

**To verify successful ingestion**:
- Check console logs for lines like:
  ```
  Indexing: Algebra Basics -> [Algebra Basics, Algebra, alg]
  ```
- Check Elasticsearch index content:
  ```bash
  curl 'http://localhost:9200/course/_search?pretty'
  ```
- Or use the API as below.

---

## API Endpoints & Usage

### Search

**Keyword search:**
```bash
curl "http://localhost:8080/api/search?q=physics"
```

### Filtering & Sorting

- **By category:**  
  `curl "http://localhost:8080/api/search?category=Art"`
- **Age range:**  
  `curl "http://localhost:8080/api/search?minAge=9&maxAge=12"`
- **Price range:**  
  `curl "http://localhost:8080/api/search?minPrice=30&maxPrice=60"`
- **Upcoming sessions:**  
  `curl "http://localhost:8080/api/search?startDate=2025-07-20T00:00:00Z"`
- **Sort by price ascending:**  
  `curl "http://localhost:8080/api/search?sort=priceAsc"`
- **Sort by price descending:**  
  `curl "http://localhost:8080/api/search?sort=priceDesc"`
- **By type:**  
  `curl "http://localhost:8080/api/search?type=CLUB"`

### Pagination

- **Paginated search:**  
  `curl "http://localhost:8080/api/search?q=java&page=0&size=10"`
- **Combined filters:**  
  ```
  curl "http://localhost:8080/api/search?q=data&category=science&type=OFFLINE&minAge=16&maxAge=22&minPrice=1000&maxPrice=3000&startDate=2025-08-10T00:00:00Z&sort=priceDesc&page=0&size=10"
  ```

### Autocomplete (Completion Suggester)

- **Suggest endpoint:**  
  `curl "http://localhost:8080/api/search/suggest?q=phy"`
  - Example response:
    ```json
    [
      "History Physics",
      "Kite Physics"
    ]
    ```

### Fuzzy Search

- **Typos are handled:**  
  `curl "http://localhost:8080/api/search?q=scence"`
  - Example response:
    ```json
    {
      "total": 3,
      "courses": [
        {
          "id": "course-043",
          "title": "Rocket Science Basics",
          "category": "Science",
          "price": 50.61,
          "nextSessionDate": "2025-07-17T00:00:00Z"
        },
        {
          "id": "course-021",
          "title": "Virtual Science Fair",
          "category": "Technology",
          "price": 65.73,
          "nextSessionDate": "2025-07-27T00:00:00Z"
        },
        {
          "id": "course-029",
          "title": "Data Science for Kids",
          "category": "Music",
          "price": 63.31,
          "nextSessionDate": "2025-08-03T00:00:00Z"
        }
      ]
    }
    ```

---

## Sample Responses

### Search by Keyword

```bash
curl "http://localhost:8080/api/search?q=physics"
```
**Response:**
```json
{
  "total": 2,
  "courses": [
    {
      "id": "course-044",
      "title": "History Physics",
      "category": "Science",
      "price": 48.50,
      "nextSessionDate": "2025-07-20T00:00:00Z"
    },
    {
      "id": "course-045",
      "title": "Kite Physics",
      "category": "Science",
      "price": 52.75,
      "nextSessionDate": "2025-07-22T00:00:00Z"
    }
  ]
}
```

### Autocomplete Example

```bash
curl "http://localhost:8080/api/search/suggest?q=phy"
```
**Response:**
```json
[
  "History Physics",
  "Kite Physics"
]
```

### Fuzzy Search Example

```bash
curl "http://localhost:8080/api/search?q=scence"
```
**Response:**
```json
{
  "total": 3,
  "courses": [
    {
      "id": "course-043",
      "title": "Rocket Science Basics",
      "category": "Science",
      "price": 50.61,
      "nextSessionDate": "2025-07-17T00:00:00Z"
    },
    {
      "id": "course-021",
      "title": "Virtual Science Fair",
      "category": "Technology",
      "price": 65.73,
      "nextSessionDate": "2025-07-27T00:00:00Z"
    },
    {
      "id": "course-029",
      "title": "Data Science for Kids",
      "category": "Music",
      "price": 63.31,
      "nextSessionDate": "2025-08-03T00:00:00Z"
    }
  ]
}
```

---

## More API Endpoint Examples & Outputs

Below are additional sample requests and responses for the `/api/search` endpoint, matching the provided sample data. These examples demonstrate filtering, sorting, pagination, and combined queries.

<!-- (Section omitted here for brevity, but see previous messages for detailed examples for category, age range, price, sorting, combined filters, autocomplete, and fuzzy search.) -->

---

## Maven Dependencies and Versions

Your project uses the following key dependencies (see `pom.xml`):

| Dependency                                                      | Version   | Purpose                                      |
|-----------------------------------------------------------------|-----------|----------------------------------------------|
| org.springframework.boot:spring-boot-starter-parent             | 3.5.3     | Spring Boot parent, dependency management    |
| org.springframework.boot:spring-boot-starter-data-elasticsearch | 3.5.3     | Spring Data Elasticsearch integration        |
| co.elastic.clients:elasticsearch-java                           | 8.14.0    | Elasticsearch Java API client (v8.x)         |
| org.springframework.boot:spring-boot-starter-web                | 3.5.3     | Build RESTful web applications               |
| com.fasterxml.jackson.datatype:jackson-datatype-jsr310          | *(latest)*| Java 8+ date/time support for Jackson        |
| org.projectlombok:lombok                                        | *(latest)*| Boilerplate code generation (annotations)    |
| org.springframework.boot:spring-boot-starter-test               | 3.5.3     | Testing support                              |

**pom.xml** snippet:
```xml
<properties>
    <java.version>21</java.version>
</properties>
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    <dependency>
        <groupId>co.elastic.clients</groupId>
        <artifactId>elasticsearch-java</artifactId>
        <version>8.14.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

