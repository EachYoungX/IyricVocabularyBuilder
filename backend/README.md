# Lyric Vocabulary Builder 后端 API

一个基于 Spring Boot 的后端服务，用于管理英文歌曲库、构建词汇索引并提供英英词典查询功能。所有数据均存储在本地 SQLite 数据库中，应用无需联网即可运行。

## 技术栈

- **框架**: Spring Boot 3.5.7
- **语言**: Java 21
- **数据访问**: Spring Data JPA
- **数据库**: SQLite (通过 `sqlite-jdbc` 驱动)
- **Web 服务**: Spring Web (Tomcat 内嵌)
- **API 规范**: OpenAPI 3.1.0
- **开发工具**: Lombok, Spring Boot DevTools

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/each17/backend/
│   │   │   ├── config/           # 配置类
│   │   │   ├── controller/       # 控制器层
│   │   │   ├── dto/              # 数据传输对象
│   │   │   ├── entity/           # 实体类
│   │   │   ├── exception/        # 异常处理
│   │   │   ├── mapper/           # 数据映射器
│   │   │   ├── repository/       # 数据访问层
│   │   │   ├── service/          # 服务层
│   │   │   │   └── impl/         # 服务实现
│   │   │   └── BackendApplication.java  # 应用入口
│   │   └── resources/
│   │       ├── application.yaml  # 配置文件
│   │       ├── api-docs.yaml     # OpenAPI 规范
│   │       └── dictionary.sqlite # 词典数据库
│   └── test/                     # 测试代码
└── pom.xml                       # Maven 配置
```

## 数据库设计

### 用户数据库 (`app_data.sqlite`)

存储用户导入的所有歌曲和词汇索引：

1. **`songs` 表**: 存储用户导入的歌曲原始文本信息
   - id: 歌曲唯一标识符（自增主键）
   - title: 歌曲标题
   - artist: 歌曲艺术家
   - lyrics: 完整的歌曲正文

2. **`vocabulary` 表**: 存储从所有歌曲中提取的词汇倒排索引
   - word: 小写英文单词（主键）
   - occurrences: JSON 字符串，记录单词在所有歌曲中的出现位置

### 词典数据库 (dictionary.sqlite)

提供内置的离线英英词典，包含单词的音标、释义、词形变化等详细信息。

## API 接口

### 词汇相关接口

#### 获取唯一单词分页列表
```http
GET /api/vocabulary/words
```

**查询参数:**
- `prefix`: 前缀过滤（可选）
- `page`: 页码（从0开始，默认0）
- size: 每页条数（默认50）

#### 获取单词出现位置
```http
GET /api/vocabulary/words/{word}/occurrences
```

#### 重新构建词汇索引
```http
POST /api/vocabulary/refresh
```

### 歌曲相关接口

#### 获取所有歌曲
```http
GET /api/songs
```

#### 创建歌曲
```http
POST /api/songs
```

#### 更新歌曲
```http
PUT /api/songs/{id}
```

#### 删除歌曲
```http
DELETE /api/songs/{id}
```

#### 异步批量导入歌曲
```http
POST /api/songs/import
```

#### 查询导入任务结果
```http
GET /api/songs/import/tasks/{taskId}
```

### 词典查询接口

#### 查询英英词典
```http
GET /api/dictionary/{word}
```

## 配置说明

### application.yaml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:src/main/resources/data/app_data.sqlite
    driver-class-name: org.sqlite.JDBC

  jpa:
    hibernate:
      ddl-auto: update                     # 自动建表/更新表结构
    show-sql: true                         # 开发时方便看 SQL
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
        format_sql: true

app:
  dictionary:
    db-path: "classpath:dictionary.sqlite"
```

## 运行环境

- JDK 21
- Maven 3.6+

## 构建和运行

### 构建项目
```bash
./mvnw clean package
```

### 运行应用
```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### 开发模式运行
```bash
./mvnw spring-boot:run
```

## 测试

### 运行所有测试
```bash
./mvnw test
```

### 运行单元测试
```bash
./mvnw test -Dtest="com.each17.backend.entity.*Test,com.each17.backend.dto.*Test,com.each17.backend.mapper.*Test,com.each17.backend.service.impl.*Test,com.each17.backend.exception.*Test"
```

## 许可证

MIT License