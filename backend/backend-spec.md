# Lyric Vocabulary Builder - 后端实现说明文档

## 1. 项目概述与技术栈

### 1.1. 项目目标

本项目旨在为 "Lyric Vocabulary Builder" 桌面及移动应用提供一个纯本地运行的后端服务。该服务负责管理用户的英文歌曲库、从歌词中构建和查询词汇索引、并提供内置的英英词典查询功能。所有数据均存储在本地 SQLite 数据库中，应用无需联网即可运行。

### 1.2. 核心技术栈

- **框架**: Spring Boot 3.5.7
- **语言**: Java 25
- **数据访问**: Spring Data JPA
- **数据库**: SQLite (通过 `sqlite-jdbc` 驱动)
- **Web 服务**: Spring Web (Tomcat 内嵌)
- **API 规范**: OpenAPI 3.1.0 (`openapi.yaml`)
- **开发工具**: Lombok, Spring Boot DevTools

### 1.3. 项目架构

项目采用经典的分层架构，以保证代码的清晰、可维护和可测试性：

- **Controller**: `controller` 层，负责接收 HTTP 请求，验证基本参数，并调用 `Service` 层处理业务逻辑。它直接与外界的 API 规范对应。
- **Service**: `service` 层，包含业务逻辑的核心。它定义接口 (`service`) 和实现 (`service.impl`)，负责协调 `Repository` 和 `DTO` 转换，处理复杂的业务流程（如异步任务）。
- **Repository**: `repository` 层，继承自 Spring Data JPA 的 `JpaRepository`，负责与数据库的直接交互，执行增删改查 (CRUD) 操作。
- **Entity**: `entity` 层，定义与数据库表结构一一对应的 JPA 实体类。
- **DTO (Data Transfer Object)**: `dto` 层，定义用于在不同层之间（特别是 Controller 和 Service 之间）传输数据的对象，避免直接暴露数据库实体。
- **Mapper**: `mapper` 层，负责 `Entity` 和 `DTO`之间的相互转换。

## 2. 配置文件 (`application.yaml`)

项目使用 `application.yaml` 进行配置，比 `.properties` 文件更具结构化和可读性。

```yaml
server:
  port: 8080

spring:
  # --- JPA 和主数据源 (app_data.sqlite) 配置 ---
  datasource:
    # 动态指向用户主目录下的应用数据文件
    url: jdbc:sqlite:${user.home}/.lyricbuilder/app_data.sqlite
    driver-class-name: org.sqlite.JDBC
    # 为了简化多数据源配置，我们将把第二数据源在 Java Config 中定义
    
  jpa:
    hibernate:
      # 自动根据 Entity 更新或创建表结构
      ddl-auto: update
    show-sql: true # 开发时开启，方便调试
    properties:
      hibernate:
        # 必须为 SQLite 指定方言
        dialect: org.hibernate.dialect.SQLiteDialect
        # 优化 SQLite 的批量插入/更新
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

# --- 自定义配置 ---
app:
  dictionary:
    # 词典数据库由独立项目发布，通过环境变量注入 JDBC URL；主项目不携带词典文件
    enabled: false
    datasource:
      url: "jdbc:sqlite:/absolute/path/lyric-dictionary.sqlite"
```

## 3. 数据库与实体 (Entity)

### 3.1. 目录与文件创建
在项目启动时，需要一个组件来检查并创建用户数据目录。

- **`AppInitializer.java`**:
  ```java
  @Component
  public class AppInitializer implements CommandLineRunner {
      @Override
      public void run(String... args) throws Exception {
          String userHome = System.getProperty("user.home");
          Path dataDir = Paths.get(userHome, ".lyricbuilder");
          if (!Files.exists(dataDir)) {
              Files.createDirectories(dataDir);
              // Log directory creation
          }
      }
  }
  ```

### 3.2. `Song` 实体
对应 `songs` 表，存储用户导入的歌曲。

- **`entity/Song.java`**:
  ```java
  @Data // Lombok 注解，自动生成 Getter, Setter, toString, etc.
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Entity
  @Table(name = "songs", uniqueConstraints = @UniqueConstraint(columnNames = {"title", "artist"}))
  public class Song {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      @Column(nullable = false)
      private String title;
      
      @Column(nullable = false)
      private String artist;
      
      @Lob // 表示这是一个大的文本字段
      @Column(nullable = false, columnDefinition = "TEXT")
      private String lyrics;
  }
  ```

### 3.3. `Vocabulary` 实体
对应 `vocabulary` 表，存储词汇索引。

- **`entity/Vocabulary.java`**:
  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Entity
  @Table(name = "vocabulary")
  public class Vocabulary {
      @Id
      private String word;
      
      @Lob
      @Column(nullable = false, columnDefinition = "TEXT")
      private String occurrences; // 存储 JSON 字符串
  }
  ```

## 4. 数据访问层 (Repository)

使用 Spring Data JPA，我们只需要定义接口。

- **`repository/SongRepository.java`**:
  ```java
  @Repository
  public interface SongRepository extends JpaRepository<Song, Long> {
      // JPA 会根据方法名自动生成查询
      // 这里可以根据需要添加自定义查询，例如：
      // List<Song> findByArtist(String artist);
  }
  ```

- **`repository/VocabularyRepository.java`**:
  ```java

  @Repository
  public interface VocabularyRepository extends JpaRepository<Vocabulary, String> {
      // 查询以某个前缀开头的单词，并支持分页
      Page<Vocabulary> findByWordStartingWith(String prefix, Pageable pageable);
  }
  ```

## 5. 数据传输对象 (DTO) 与转换 (Mapper)

DTO 用于 API 的输入和输出，与 Entity 解耦。

### 5.1. DTO 定义 (`dto/` 目录下)
根据 `openapi.yaml` 定义以下 DTO 类（使用 Lombok 简化）：

- `WordOccurrenceDto.java`
- `SongDto.java`
- `SongImportRequestDto.java`
- `SongUpdateRequestDto.java`
- `DictionaryEntryDto.java`
- `ImportTaskResultDto.java`
- `ErrorResponseDto.java`

**示例 - `SongDto.java`**:```java
@Data
@Builder
public class SongDto {
private Long id;
private String title;
private String artist;
private String lyrics;
}
```

### 5.2. Mapper 定义
负责 DTO 和 Entity 之间的转换。推荐使用 MapStruct 库自动生成实现，但手动编写也可以。

- **`mapper/SongMapper.java`**:
  ```java
  @Component
  public class SongMapper {
      public SongDto toDto(Song song) {
          return SongDto.builder()
              .id(song.getId())
              .title(song.getTitle())
              .artist(song.getArtist())
              .lyrics(song.getLyrics())
              .build();
      }

      public Song toEntity(SongImportRequestDto dto) {
          return Song.builder()
              .title(dto.getTitle())
              .artist(dto.getArtist())
              .lyrics(dto.getLyrics())
              .build();
      }
      // ... 其他转换方法
  }
  ```

## 6. 业务逻辑层 (Service)

这是业务逻辑的核心。

- **`service/SongService.java`** (接口)
- **`service/VocabularyService.java`** (接口)
- **`service/DictionaryService.java`** (接口)
- **`service/ImportTaskService.java`** (接口)

### 6.1. `SongServiceImpl.java` (示例)
```java
@Service
@RequiredArgsConstructor // Lombok: 自动注入 final 字段
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final SongMapper songMapper;

    @Override
    public List<SongDto> getAllSongs() {
        return songRepository.findAll().stream()
                .map(songMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void importSongs(List<SongImportRequestDto> songsToImport) {
        // 实现异步导入逻辑，调用 ImportTaskService
    }
    
    // ... updateSong, deleteSong 等方法的实现
}
```

### 6.2. `VocabularyServiceImpl.java`
- `getWordList`: 实现分页和前缀查询，调用 `VocabularyRepository`。
- `getWordOccurrences`: 查询 `Vocabulary` 实体，并使用 `Jackson ObjectMapper` 将 `occurrences` JSON 字符串反序列化为 `List<WordOccurrenceDto>`。
- `refreshVocabularyIndex`: 实现**异步 (`@Async`)** 重建索引的核心逻辑。
    1.  调用 `SongRepository.findAll()` 获取所有歌曲。
    2.  在内存中进行分词、去重、构建倒排索引（一个 `Map<String, List<WordOccurrenceDto>>`）。
    3.  将这个 Map 转换为 `List<Vocabulary>` 实体（`occurrences` 字段需要序列化为 JSON 字符串）。
    4.  调用 `vocabularyRepository.deleteAll()` 清空旧索引。
    5.  调用 `vocabularyRepository.saveAll()` 批量保存新索引。

## 7. API 接口层 (Controller)

直接映射 `openapi.yaml` 的 `paths`。

- **`controller/SongController.java`**:
  ```java
  @RestController
  @RequestMapping("/api/songs")
  @RequiredArgsConstructor
  public class SongController {

      private final SongService songService;

      @GetMapping
      public ResponseEntity<List<SongDto>> getAllSongs() {
          return ResponseEntity.ok(songService.getAllSongs());
      }
      
      @PostMapping("/import")
      public ResponseEntity<ImportTaskStatusDto> importSongs(@RequestBody List<SongImportRequestDto> songsToImport) {
          // 调用异步导入服务，并立即返回 202 Accepted 和 taskId
      }
      
      // ... PUT /{id}, DELETE /{id}
  }
  ```
- **`controller/VocabularyController.java`**: 实现 `/api/vocabulary/` 下的所有接口。
- **`controller/DictionaryController.java`**: 实现 `/api/dictionary/` 下的接口。

## 8. 多数据源配置 (重要)

由于有两个数据库，需要显式配置多个数据源。

- **`config/DataSourceConfig.java`**:
  ```java
  @Configuration
  public class DataSourceConfig {

      // --- 主数据源 (app_data) ---
      @Primary
      @Bean(name = "appDataSource")
      @ConfigurationProperties(prefix = "spring.datasource")
      public DataSource appDataSource() {
          return DataSourceBuilder.create().build();
      }

      // --- 词典数据源 (dictionary) ---
      @Bean(name = "dictionaryDataSource")
      public DataSource dictionaryDataSource(@Value("${app.dictionary.datasource.url}") String url) {
          return DataSourceBuilder.create()
              .url(url)
              .driverClassName("org.sqlite.JDBC")
              .build();
      }
      
      // ... 需要为 dictionaryDataSource 配置一个独立的 JdbcTemplate 或其他查询工具
      // 因为它不使用 JPA
  }
  ```
- **`DictionaryServiceImpl`** 将注入 `dictionaryDataSource` 对应的 `JdbcTemplate` 来执行原生 SQL 查询。

## 9. 全局异常处理

- **`exception/GlobalExceptionHandler.java`**:
  ```java
  @RestControllerAdvice
  public class GlobalExceptionHandler {

      @ExceptionHandler(ResourceNotFoundException.class)
      @ResponseStatus(HttpStatus.NOT_FOUND)
      public ErrorResponseDto handleResourceNotFound(ResourceNotFoundException ex) {
          // ... 返回 404 对应的标准错误体
      }

      @ExceptionHandler(MethodArgumentNotValidException.class)
      @ResponseStatus(HttpStatus.BAD_REQUEST)
      public ErrorResponseDto handleValidationExceptions(MethodArgumentNotValidException ex) {
          // ... 处理JPA验证失败，返回 400 和详细错误信息
      }
      
      // ... 其他异常处理
  }
  ```

## 10. 异步任务配置

为了让 `@Async` 注解生效，需要一个配置类。

- **`config/AsyncConfig.java`**:
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      // 可以自定义线程池，也可以使用 Spring Boot 的默认配置
  }
  ```

---
