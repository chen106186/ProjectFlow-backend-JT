# ProjectFlow Backend Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable Java 21 Spring Boot backend foundation for ProjectFlow, covering infrastructure, auth/RBAC, project/task/BUG/file/audit/dashboard P0 skeletons.

**Architecture:** Use a modular monolith. Keep one Spring Boot application, split code by business module, and communicate across modules through service interfaces rather than direct mapper access.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven, MySQL 8, MyBatis-Plus, Flyway, Spring Security, JWT, Redis, springdoc-openapi, EasyExcel, JUnit 5.

---

## File Structure

Create the following backend structure:

```text
pom.xml
src/main/java/com/jitong/projectflow/ProjectFlowApplication.java
src/main/java/com/jitong/projectflow/common
src/main/java/com/jitong/projectflow/auth
src/main/java/com/jitong/projectflow/system
src/main/java/com/jitong/projectflow/project
src/main/java/com/jitong/projectflow/task
src/main/java/com/jitong/projectflow/requirement
src/main/java/com/jitong/projectflow/bug
src/main/java/com/jitong/projectflow/file
src/main/java/com/jitong/projectflow/notice
src/main/java/com/jitong/projectflow/dashboard
src/main/resources/application.yml
src/main/resources/db/migration/V1__init_schema.sql
src/test/java/com/jitong/projectflow
```

Boundary rules:

- `common` owns response wrappers, exception handling, paging, trace ID, base entities, enums, and shared configuration.
- `auth` owns login, JWT, current-user resolution, and security filters.
- `system` owns users, departments, roles, menus, and operation logs.
- `file` owns file metadata and `FileStorageService`; business modules call this service instead of writing files directly.
- `task`, `bug`, and `project` own their domain state machines and expose services for dashboard aggregation.

## Task 1: Spring Boot Project Scaffold

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/jitong/projectflow/ProjectFlowApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/jitong/projectflow/ProjectFlowApplicationTests.java`

- [ ] **Step 1: Create Maven build file**

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.3</version>
        <relativePath/>
    </parent>

    <groupId>com.jitong</groupId>
    <artifactId>projectflow-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>ProjectFlow Backend</name>
    <description>ProjectFlow backend service</description>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
        <springdoc.version>2.8.9</springdoc.version>
        <easyexcel.version>4.0.3</easyexcel.version>
        <aliyun-oss.version>3.18.1</aliyun-oss.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>${easyexcel.version}</version>
        </dependency>
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>${aliyun-oss.version}</version>
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
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create Spring Boot entry point**

Create `src/main/java/com/jitong/projectflow/ProjectFlowApplication.java`:

```java
package com.jitong.projectflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.jitong.projectflow.**.mapper")
@SpringBootApplication
public class ProjectFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectFlowApplication.class, args);
    }
}
```

- [ ] **Step 3: Create local configuration**

Create `src/main/resources/application.yml`:

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: projectflow-backend
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/projectflow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 60MB

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

projectflow:
  jwt:
    secret: "replace-with-at-least-32-byte-local-secret"
    ttl-minutes: 720
  storage:
    type: local
    local-root: ./data/uploads
```

- [ ] **Step 4: Add context load test**

Create `src/test/java/com/jitong/projectflow/ProjectFlowApplicationTests.java`:

```java
package com.jitong.projectflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProjectFlowApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Verify scaffold**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: build succeeds.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java src/main/resources src/test/java
git commit -m "chore: initialize spring boot backend"
```

## Task 2: Common API, Exceptions, Paging, and Trace ID

**Files:**
- Create: `src/main/java/com/jitong/projectflow/common/api/ApiResponse.java`
- Create: `src/main/java/com/jitong/projectflow/common/api/PageResult.java`
- Create: `src/main/java/com/jitong/projectflow/common/error/ErrorCode.java`
- Create: `src/main/java/com/jitong/projectflow/common/error/BusinessException.java`
- Create: `src/main/java/com/jitong/projectflow/common/error/GlobalExceptionHandler.java`
- Create: `src/main/java/com/jitong/projectflow/common/web/TraceIdFilter.java`
- Test: `src/test/java/com/jitong/projectflow/common/error/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write response wrapper**

Create `ApiResponse.java`:

```java
package com.jitong.projectflow.common.api;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, "success", data, traceId);
    }

    public static <T> ApiResponse<T> failure(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
```

Create `PageResult.java`:

```java
package com.jitong.projectflow.common.api;

import java.util.List;

public record PageResult<T>(long total, long pageNo, long pageSize, List<T> records) {
}
```

- [ ] **Step 2: Write error model**

Create `ErrorCode.java`:

```java
package com.jitong.projectflow.common.error;

public enum ErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "业务状态冲突"),
    INTERNAL_ERROR(500, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
```

Create `BusinessException.java`:

```java
package com.jitong.projectflow.common.error;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 3: Add trace ID filter**

Create `TraceIdFilter.java`:

```java
package com.jitong.projectflow.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader("X-Trace-Id", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

- [ ] **Step 4: Add global exception handler**

Create `GlobalExceptionHandler.java`:

```java
package com.jitong.projectflow.common.error;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.web.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.errorCode();
        return ResponseEntity.status(toStatus(errorCode))
                .body(ApiResponse.failure(errorCode.code(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), ErrorCode.BAD_REQUEST.message(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message(), traceId()));
    }

    private HttpStatus toStatus(ErrorCode errorCode) {
        return HttpStatus.valueOf(errorCode.code());
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId;
    }
}
```

- [ ] **Step 5: Verify common layer**

Run:

```bash
mvn -q test
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/jitong/projectflow/common src/test/java/com/jitong/projectflow/common
git commit -m "feat: add common api and error handling"
```

## Task 3: Initial MySQL Schema with Flyway

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: Create initial schema migration**

Create `V1__init_schema.sql`:

```sql
CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    department_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    job_no VARCHAR(64) NULL,
    position_name VARCHAR(64) NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    hire_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NULL,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    path VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sys_menu_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY,
    module VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    operation_type VARCHAR(64) NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    before_value JSON NULL,
    after_value JSON NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sys_operation_log_business (business_type, business_id),
    INDEX idx_sys_operation_log_operator_time (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project (
    id BIGINT PRIMARY KEY,
    project_type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    stage VARCHAR(64) NULL,
    status VARCHAR(64) NOT NULL,
    contract_status VARCHAR(64) NULL,
    manager_id BIGINT NULL,
    description VARCHAR(2000) NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_type_status (project_type, status),
    INDEX idx_pf_project_manager (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_project_node (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    node_name VARCHAR(200) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    status VARCHAR(64) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_project_node_project (project_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_task (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    role_name VARCHAR(64) NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assignee_id BIGINT NOT NULL,
    planned_start_date DATE NULL,
    planned_end_date DATE NULL,
    actual_start_date DATE NULL,
    actual_end_date DATE NULL,
    description VARCHAR(2000) NULL,
    tags VARCHAR(500) NULL,
    remark VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_task_project (project_id),
    INDEX idx_pf_task_assignee_status (assignee_id, status),
    INDEX idx_pf_task_deadline (planned_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_requirement (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    requirement_type VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    description VARCHAR(2000) NULL,
    tags VARCHAR(500) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_requirement_creator_status (created_by, status),
    INDEX idx_pf_requirement_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_bug (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    description VARCHAR(2000) NOT NULL,
    reproduce_steps VARCHAR(2000) NOT NULL,
    closed_at DATETIME NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_bug_project (project_id),
    INDEX idx_pf_bug_creator_assignee (creator_id, assignee_id),
    INDEX idx_pf_bug_status_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_bug_comment (
    id BIGINT PRIMARY KEY,
    bug_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_bug_comment_bug_time (bug_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_file (
    id BIGINT PRIMARY KEY,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NULL,
    file_size BIGINT NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    storage_type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    uploader_id BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_file_business (business_type, business_id),
    INDEX idx_pf_file_uploader_time (uploader_id, uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pf_notice (
    id BIGINT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    notice_type VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    business_type VARCHAR(64) NULL,
    business_id BIGINT NULL,
    read_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pf_notice_receiver_read (receiver_id, read_flag),
    INDEX idx_pf_notice_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Verify migration syntax with application startup**

Run with a local MySQL database named `projectflow`:

```bash
mvn spring-boot:run
```

Expected: Flyway applies `V1__init_schema.sql` and the application starts on port 8080.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V1__init_schema.sql
git commit -m "feat: add initial mysql schema"
```

## Task 4: Auth and RBAC Foundation

**Files:**
- Create: `src/main/java/com/jitong/projectflow/auth/controller/AuthController.java`
- Create: `src/main/java/com/jitong/projectflow/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/jitong/projectflow/auth/dto/LoginResponse.java`
- Create: `src/main/java/com/jitong/projectflow/auth/security/JwtTokenService.java`
- Create: `src/main/java/com/jitong/projectflow/auth/security/SecurityConfig.java`
- Create: `src/main/java/com/jitong/projectflow/system/entity/SystemUser.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/SystemUserMapper.java`
- Create: `src/main/java/com/jitong/projectflow/system/service/SystemUserService.java`
- Test: `src/test/java/com/jitong/projectflow/auth/security/JwtTokenServiceTest.java`

- [ ] **Step 1: Create login DTOs**

Create `LoginRequest.java`:

```java
package com.jitong.projectflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
```

Create `LoginResponse.java`:

```java
package com.jitong.projectflow.auth.dto;

public record LoginResponse(String token, Long userId, String realName) {
}
```

- [ ] **Step 2: Create user entity and mapper**

Create `SystemUser.java`:

```java
package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class SystemUser {
    private Long id;
    private Long departmentId;
    private String username;
    private String passwordHash;
    private String realName;
    private Boolean enabled;
    private Boolean deleted;
}
```

Create `SystemUserMapper.java`:

```java
package com.jitong.projectflow.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.system.entity.SystemUser;

public interface SystemUserMapper extends BaseMapper<SystemUser> {
}
```

- [ ] **Step 3: Create JWT service**

Create `JwtTokenService.java`:

```java
package com.jitong.projectflow.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey key;
    private final long ttlMinutes;

    public JwtTokenService(@Value("${projectflow.jwt.secret}") String secret,
                           @Value("${projectflow.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
    }

    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }
}
```

- [ ] **Step 4: Create permissive security baseline for first integration**

Create `SecurityConfig.java`:

```java
package com.jitong.projectflow.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 5: Create login controller**

Create `AuthController.java`:

```java
package com.jitong.projectflow.auth.controller;

import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.dto.LoginResponse;
import com.jitong.projectflow.auth.security.JwtTokenService;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SystemUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthController(SystemUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SystemUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemUser>()
                        .eq(SystemUser::getUsername, request.username())
                        .eq(SystemUser::getDeleted, false));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = tokenService.createToken(user.getId(), user.getUsername());
        return ApiResponse.success(new LoginResponse(token, user.getId(), user.getRealName()), MDC.get("traceId"));
    }
}
```

- [ ] **Step 6: Verify auth compilation**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compilation succeeds.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/jitong/projectflow/auth src/main/java/com/jitong/projectflow/system
git commit -m "feat: add auth and user foundation"
```

## Task 5: File Storage Abstraction with Local and Aliyun OSS Boundary

**Files:**
- Create: `src/main/java/com/jitong/projectflow/file/domain/FileStorageService.java`
- Create: `src/main/java/com/jitong/projectflow/file/domain/FileUploadCommand.java`
- Create: `src/main/java/com/jitong/projectflow/file/domain/StoredFile.java`
- Create: `src/main/java/com/jitong/projectflow/file/storage/AliyunOssStorageProperties.java`
- Create: `src/main/java/com/jitong/projectflow/file/storage/LocalFileStorageService.java`
- Create: `src/main/java/com/jitong/projectflow/file/storage/AliyunOssFileStorageService.java`
- Create: `src/main/java/com/jitong/projectflow/file/controller/FileController.java`
- Create: `src/main/java/com/jitong/projectflow/file/entity/FileMetadata.java`
- Create: `src/main/java/com/jitong/projectflow/file/mapper/FileMetadataMapper.java`
- Test: `src/test/java/com/jitong/projectflow/file/storage/LocalFileStorageServiceTest.java`

- [ ] **Step 1: Create storage domain contract**

Create `FileStorageService.java`:

```java
package com.jitong.projectflow.file.domain;

import java.io.InputStream;

public interface FileStorageService {
    StoredFile upload(FileUploadCommand command);
    InputStream download(String storageKey);
    void delete(String storageKey);
}
```

Create `FileUploadCommand.java`:

```java
package com.jitong.projectflow.file.domain;

import java.io.InputStream;

public record FileUploadCommand(
        String originalName,
        String contentType,
        long fileSize,
        InputStream inputStream
) {
}
```

Create `StoredFile.java`:

```java
package com.jitong.projectflow.file.domain;

public record StoredFile(String storageType, String storageKey, long fileSize) {
}
```

- [ ] **Step 2: Implement local storage**

Create `LocalFileStorageService.java`:

```java
package com.jitong.projectflow.file.storage;

import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "projectflow.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    private final Path root;

    public LocalFileStorageService(@Value("${projectflow.storage.local-root}") String localRoot) {
        this.root = Path.of(localRoot);
    }

    @Override
    public StoredFile upload(FileUploadCommand command) {
        try {
            String datePath = LocalDate.now().toString();
            String safeName = UUID.randomUUID() + "-" + command.originalName().replaceAll("[\\\\/]", "_");
            Path target = root.resolve(datePath).resolve(safeName).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(command.inputStream(), target);
            return new StoredFile("LOCAL", root.relativize(target).toString().replace("\\", "/"), command.fileSize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件上传失败", ex);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return Files.newInputStream(root.resolve(storageKey).normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件下载失败", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(root.resolve(storageKey).normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("文件删除失败", ex);
        }
    }
}
```

- [ ] **Step 3: Add Aliyun OSS storage configuration**

Create `AliyunOssStorageProperties.java`:

```java
package com.jitong.projectflow.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectflow.storage.aliyun-oss")
public record AliyunOssStorageProperties(
        String endpoint,
        String accessKeyId,
        String accessKeySecret,
        String bucketName,
        String objectPrefix
) {
}
```

- [ ] **Step 4: Add Aliyun OSS adapter implementation**

Create `AliyunOssFileStorageService.java`:

```java
package com.jitong.projectflow.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.jitong.projectflow.file.domain.FileStorageService;
import com.jitong.projectflow.file.domain.FileUploadCommand;
import com.jitong.projectflow.file.domain.StoredFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service
@EnableConfigurationProperties(AliyunOssStorageProperties.class)
@ConditionalOnProperty(prefix = "projectflow.storage", name = "type", havingValue = "aliyun-oss")
public class AliyunOssFileStorageService implements FileStorageService {
    private final OSS ossClient;
    private final AliyunOssStorageProperties properties;

    public AliyunOssFileStorageService(AliyunOssStorageProperties properties) {
        this.properties = properties;
        this.ossClient = new OSSClientBuilder().build(
                properties.endpoint(),
                properties.accessKeyId(),
                properties.accessKeySecret());
    }

    @Override
    public StoredFile upload(FileUploadCommand command) {
        String objectKey = buildObjectKey(command.originalName());
        ossClient.putObject(properties.bucketName(), objectKey, command.inputStream());
        return new StoredFile("ALIYUN_OSS", objectKey, command.fileSize());
    }

    @Override
    public InputStream download(String storageKey) {
        return ossClient.getObject(properties.bucketName(), storageKey).getObjectContent();
    }

    @Override
    public void delete(String storageKey) {
        ossClient.deleteObject(properties.bucketName(), storageKey);
    }

    private String buildObjectKey(String originalName) {
        String prefix = properties.objectPrefix() == null ? "projectflow" : properties.objectPrefix();
        String safeName = originalName.replaceAll("[\\\\/]", "_");
        return prefix + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
    }
}
```

- [ ] **Step 5: Add storage properties to application config**

Add the following under `projectflow.storage` in `application.yml`:

```yaml
    aliyun-oss:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:local-dev-key}
      access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:local-dev-secret}
      bucket-name: ${ALIYUN_OSS_BUCKET:projectflow-dev}
      object-prefix: projectflow
```

- [ ] **Step 6: Add metadata entity and mapper**

Create `FileMetadata.java`:

```java
package com.jitong.projectflow.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pf_file")
public class FileMetadata {
    private Long id;
    private String businessType;
    private Long businessId;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String versionNo;
    private String storageType;
    private String storageKey;
    private Long uploaderId;
    private LocalDateTime uploadedAt;
    private Boolean deleted;
}
```

Create `FileMetadataMapper.java`:

```java
package com.jitong.projectflow.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jitong.projectflow.file.entity.FileMetadata;

public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
```

- [ ] **Step 7: Verify storage compilation**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compilation succeeds.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/jitong/projectflow/file src/test/java/com/jitong/projectflow/file
git commit -m "feat: add file storage abstraction"
```

## Task 6: Task Status and Operation Log Core

**Files:**
- Create: `src/main/java/com/jitong/projectflow/task/domain/TaskStatus.java`
- Create: `src/main/java/com/jitong/projectflow/task/domain/TaskPriority.java`
- Create: `src/main/java/com/jitong/projectflow/task/domain/TaskStatusCalculator.java`
- Create: `src/main/java/com/jitong/projectflow/task/entity/TaskEntity.java`
- Create: `src/main/java/com/jitong/projectflow/task/mapper/TaskMapper.java`
- Create: `src/main/java/com/jitong/projectflow/system/audit/OperationLogService.java`
- Create: `src/main/java/com/jitong/projectflow/system/entity/OperationLog.java`
- Create: `src/main/java/com/jitong/projectflow/system/mapper/OperationLogMapper.java`
- Test: `src/test/java/com/jitong/projectflow/task/domain/TaskStatusCalculatorTest.java`

- [ ] **Step 1: Create task enums**

Create `TaskStatus.java`:

```java
package com.jitong.projectflow.task.domain;

public enum TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    DUE_SOON,
    OVERDUE,
    COMPLETED,
    PAUSED
}
```

Create `TaskPriority.java`:

```java
package com.jitong.projectflow.task.domain;

public enum TaskPriority {
    URGENT,
    HIGH,
    MEDIUM,
    LOW
}
```

- [ ] **Step 2: Create status calculator**

Create `TaskStatusCalculator.java`:

```java
package com.jitong.projectflow.task.domain;

import java.time.LocalDate;

public class TaskStatusCalculator {
    public TaskStatus calculate(LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate, boolean paused, LocalDate today) {
        if (paused) {
            return TaskStatus.PAUSED;
        }
        if (actualEndDate != null) {
            return TaskStatus.COMPLETED;
        }
        if (plannedEndDate != null && today.isAfter(plannedEndDate)) {
            return TaskStatus.OVERDUE;
        }
        if (plannedEndDate != null && !today.isBefore(plannedEndDate.minusDays(3))) {
            return TaskStatus.DUE_SOON;
        }
        if (actualStartDate != null) {
            return TaskStatus.IN_PROGRESS;
        }
        return TaskStatus.NOT_STARTED;
    }
}
```

- [ ] **Step 3: Add calculator tests**

Create `TaskStatusCalculatorTest.java`:

```java
package com.jitong.projectflow.task.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusCalculatorTest {
    private final TaskStatusCalculator calculator = new TaskStatusCalculator();

    @Test
    void returnsCompletedWhenActualEndExists() {
        TaskStatus status = calculator.calculate(LocalDate.now().minusDays(1), LocalDate.now().minusDays(2), LocalDate.now(), false, LocalDate.now());
        assertThat(status).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void returnsOverdueWhenPastPlannedEndAndNotCompleted() {
        TaskStatus status = calculator.calculate(LocalDate.of(2026, 7, 1), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.OVERDUE);
    }

    @Test
    void returnsDueSoonWithinThreeDays() {
        TaskStatus status = calculator.calculate(LocalDate.of(2026, 7, 8), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.DUE_SOON);
    }
}
```

- [ ] **Step 4: Create audit service contract**

Create `OperationLogService.java`:

```java
package com.jitong.projectflow.system.audit;

public interface OperationLogService {
    void record(String module, String businessType, Long businessId, String operationType, String content);
}
```

- [ ] **Step 5: Verify task rule tests**

Run:

```bash
mvn -q -Dtest=TaskStatusCalculatorTest test
```

Expected: all task status tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/jitong/projectflow/task src/main/java/com/jitong/projectflow/system src/test/java/com/jitong/projectflow/task
git commit -m "feat: add task status and audit core"
```

## Task 7: P0 Domain API Skeletons

**Files:**
- Create: `src/main/java/com/jitong/projectflow/project/controller/ProjectController.java`
- Create: `src/main/java/com/jitong/projectflow/project/entity/ProjectEntity.java`
- Create: `src/main/java/com/jitong/projectflow/project/mapper/ProjectMapper.java`
- Create: `src/main/java/com/jitong/projectflow/task/controller/TaskController.java`
- Create: `src/main/java/com/jitong/projectflow/bug/controller/BugController.java`
- Create: `src/main/java/com/jitong/projectflow/bug/domain/BugStatus.java`
- Create: `src/main/java/com/jitong/projectflow/bug/entity/BugEntity.java`
- Create: `src/main/java/com/jitong/projectflow/bug/mapper/BugMapper.java`
- Create: `src/main/java/com/jitong/projectflow/requirement/controller/RequirementController.java`

- [ ] **Step 1: Create project mapper-backed list endpoint**

Create `ProjectController.java`:

```java
package com.jitong.projectflow.project.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @GetMapping
    public ApiResponse<List<String>> listProjects() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
```

- [ ] **Step 2: Create task list endpoint**

Create `TaskController.java`:

```java
package com.jitong.projectflow.task.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @GetMapping
    public ApiResponse<List<String>> listTasks() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
```

- [ ] **Step 3: Create BUG status enum and endpoint**

Create `BugStatus.java`:

```java
package com.jitong.projectflow.bug.domain;

public enum BugStatus {
    PENDING_FIX,
    FIXING,
    PENDING_VERIFY,
    CLOSED
}
```

Create `BugController.java`:

```java
package com.jitong.projectflow.bug.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
public class BugController {
    @GetMapping
    public ApiResponse<List<String>> listBugs() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
```

- [ ] **Step 4: Verify endpoint paths show in OpenAPI**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compilation succeeds. Start the app and check `/swagger-ui/index.html` includes `/api/projects`, `/api/tasks`, and `/api/bugs`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/project src/main/java/com/jitong/projectflow/task src/main/java/com/jitong/projectflow/bug src/main/java/com/jitong/projectflow/requirement
git commit -m "feat: add p0 domain api skeletons"
```

## Task 8: Dashboard Summary and To-Do Sorting Contract

**Files:**
- Create: `src/main/java/com/jitong/projectflow/dashboard/controller/DashboardController.java`
- Create: `src/main/java/com/jitong/projectflow/dashboard/dto/DashboardSummaryResponse.java`
- Create: `src/main/java/com/jitong/projectflow/dashboard/dto/TodoItemResponse.java`
- Create: `src/main/java/com/jitong/projectflow/dashboard/domain/TodoSortKey.java`
- Test: `src/test/java/com/jitong/projectflow/dashboard/domain/TodoSortKeyTest.java`

- [ ] **Step 1: Create dashboard DTOs**

Create `DashboardSummaryResponse.java`:

```java
package com.jitong.projectflow.dashboard.dto;

public record DashboardSummaryResponse(
        long managementProjectCount,
        long executionProjectCount,
        long inProgressProjectCount,
        long completedProjectCount
) {
}
```

Create `TodoItemResponse.java`:

```java
package com.jitong.projectflow.dashboard.dto;

import java.time.LocalDate;

public record TodoItemResponse(
        String itemType,
        Long businessId,
        String title,
        String priority,
        String status,
        String projectName,
        String ownerName,
        LocalDate plannedEndDate,
        long overdueDays
) {
}
```

- [ ] **Step 2: Create todo sorting key**

Create `TodoSortKey.java`:

```java
package com.jitong.projectflow.dashboard.domain;

import java.time.LocalDate;

public record TodoSortKey(int priorityRank, long overdueDaysDesc, LocalDate plannedEndDate, Long sequenceId) implements Comparable<TodoSortKey> {
    @Override
    public int compareTo(TodoSortKey other) {
        int priority = Integer.compare(this.priorityRank, other.priorityRank);
        if (priority != 0) {
            return priority;
        }
        int overdue = Long.compare(other.overdueDaysDesc, this.overdueDaysDesc);
        if (overdue != 0) {
            return overdue;
        }
        int date = this.plannedEndDate.compareTo(other.plannedEndDate);
        if (date != 0) {
            return date;
        }
        return this.sequenceId.compareTo(other.sequenceId);
    }
}
```

- [ ] **Step 3: Create dashboard controller**

Create `DashboardController.java`:

```java
package com.jitong.projectflow.dashboard.controller;

import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(new DashboardSummaryResponse(0, 0, 0, 0), MDC.get("traceId"));
    }

    @GetMapping("/todos")
    public ApiResponse<List<TodoItemResponse>> todos() {
        return ApiResponse.success(List.of(), MDC.get("traceId"));
    }
}
```

- [ ] **Step 4: Verify dashboard compile**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compilation succeeds.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jitong/projectflow/dashboard src/test/java/com/jitong/projectflow/dashboard
git commit -m "feat: add dashboard api contract"
```

## Task 9: Phase 1 Verification

**Files:**
- Modify only files touched by previous tasks if verification finds a concrete compile or test issue.

- [ ] **Step 1: Run full test suite**

Run:

```bash
mvn test
```

Expected: all tests pass.

- [ ] **Step 2: Run package build**

Run:

```bash
mvn package
```

Expected: jar builds successfully under `target/`.

- [ ] **Step 3: Run application locally**

Run:

```bash
mvn spring-boot:run
```

Expected: application starts on `http://localhost:8080`.

- [ ] **Step 4: Verify Swagger**

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

Expected: Swagger UI loads and shows auth, project, task, bug, file, and dashboard groups that have been implemented so far.

- [ ] **Step 5: Commit verification fixes**

If fixes were needed:

```bash
git add pom.xml src/main/java src/main/resources src/test/java
git commit -m "fix: stabilize phase 1 backend foundation"
```

If no fixes were needed, do not create an empty commit.

## Phase 1 Definition of Done

- Maven project compiles with Java 21.
- MySQL 8 Flyway migration creates the first schema.
- Common response, error, trace ID, and paging contracts exist.
- Auth login path and JWT service exist.
- RBAC tables and initial user/role/menu structure exist.
- Local file storage works through `FileStorageService`.
- Aliyun OSS has a configurable adapter selected by `projectflow.storage.type=aliyun-oss`.
- Task status calculator is covered by unit tests.
- Operation log service contract exists for business modules.
- Project, task, BUG, requirement, and dashboard API skeletons exist.
- Swagger UI exposes the implemented endpoints.
