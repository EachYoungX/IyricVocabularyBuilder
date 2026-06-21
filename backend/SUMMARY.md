
---

### 《Lyric Vocabulary Builder - 后端开发问题与解决方案总结》（Java Spring Boot + SQLite）

**版本**：v1.0  
**日期**：2025-11-22  

#### 1. Spring Boot 启动失败的“多重嵌套依赖冲突”根本原因与终极解决方案

**问题现象**：  
应用反复出现 `BeanCreationException`、`Schema-validation failed`、`Could not resolve placeholder` 等连锁错误，错误日志不断变化，修复一个又暴露下一个。

**根本原因**：  
Spring Boot 的自动配置机制在面对 **SQLite（文件型、非嵌入式数据库） + 多数据源** 场景时存在系统性盲区：
- `spring.sql.init.mode=always` 对 SQLite 无效
- `spring.jpa.hibernate.ddl-auto=update` 在 SQLite 上行为不稳定（尤其 UNIQUE 约束）
- 多个 `@Configuration` 中重复定义 `DataSource` / `databaseInitializer` Bean 导致冲突
- 自动配置被手动配置干扰后完全失效

**解决方案**：
1. application.yaml
```yml

spring:
  sql:
    init:
      mode: never           # 彻底关闭自动初始化
  jpa:
    hibernate:
      ddl-auto: none      # 禁止自动 DDL
```
2. 完全手动配置 DataSource + 强制执行 schema.sql
```java
@Bean(name = "appDataSource")
@Primary
public DataSource appDataSource(
        @Value("${spring.datasource.url}") String url,
        @Value("${spring.datasource.driver-class-name}") String driverClassName,
        ResourceLoader resourceLoader
) {
    // 1. 手动创建 DataSource 实例
    final DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(url);
    dataSource.setDriverClassName(driverClassName);

    // 2. 手动执行 schema.sql 初始化
    Resource schema = resourceLoader.getResource("classpath:schema.sql");
    if (schema.exists()) {
        System.out.println(">>> schema.sql found. Executing initialization script...");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
        populator.execute(dataSource);
        System.out.println(">>> schema.sql execution finished.");
    } else {
        System.err.println("!!! WARNING: schema.sql not found! Tables will not be created.");
    }

    return dataSource;
}
```

**核心经验**：
> 在 SQLite + 多数据源场景下，**任何依赖 Spring Boot 自动配置的方案最终都会失败**。唯一可靠的路径是：**完全手动控制 DataSource 创建与 SQL 初始化顺序**。

#### 2. 歌曲变更后词汇索引自动更新（关键业务一致性保障）

**问题**：前端导入/修改/删除歌曲后，词汇页仍显示旧数据，用户必须手动点击 “Refresh Index”。

**解决方案**：在所有写操作完成后，**统一触发异步词汇索引重建**。

```java
// 在 createSong / updateSong / deleteSong / processSongImport 中统一调用
vocabularyService.refreshVocabularyIndexAsync();

// VocabularyService 实现（防抖 + 异步）
@Async
public void refreshVocabularyIndexAsync() {
    if (pendingRebuild != null && !pendingRebuild.isDone()) {
        pendingRebuild.cancel(false);
    }
    pendingRebuild = scheduler.schedule(() -> {
        log.info("Executing delayed vocabulary index rebuild...");
        rebuildIndexFromAllSongs();  // 全量重建
    }, 3, TimeUnit.SECONDS);
}
```

**效果**：
- 用户感知：保存/导入/删除后 3 秒内词汇自动更新
- 避免短时间多次变更导致重复重建

#### 3. 异步批量导入任务状态实时跟踪（专业级用户体验）

**实现方式**：
- 使用 `ConcurrentHashMap<UUID, ImportTaskResultDto>` 内存缓存任务状态
- `@Async` 后台执行导入
- 提供 `GET /tasks/{taskId}` 接口供前端轮询
- 导入完成后自动触发词汇索引重建

**优势**：
- 前端可显示精确进度与失败项
- 支持导入上千首歌不卡界面
- 失败歌曲清晰反馈（标题、艺术家、错误原因）

#### 4. 数据库唯一性约束与重复导入处理

**最佳实践**：
```sql
-- schema.sql 中显式定义
CREATE TABLE IF NOT EXISTS songs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    lyrics TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(title, artist)
);
```

**Service 层处理**：
```java
try {
    songRepository.save(song);
} catch (DataIntegrityViolationException e) {
    // 优雅处理重复歌曲
    taskResult.getFailedItems().add(...);
}
```

---
