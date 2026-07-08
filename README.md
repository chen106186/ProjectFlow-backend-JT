# ProjectFlow Backend JT

集通科技项目开发与管理平台后端服务，采用 Java 21、Spring Boot 3、MySQL 8、MyBatis-Plus、Flyway、Spring Security JWT 和模块化单体架构。

## 已实现范围

- 认证与权限：登录、JWT、RBAC、当前用户、角色、菜单、部门、用户管理、数据级业务权限。
- 项目管理：管理类/执行类项目、项目详情、项目节点、甘特图、甘特统计。
- 任务管理：任务创建、查询、我的任务、实际时间填报、状态计算、任务日历。
- 需求管理：需求提交、查询、编辑、状态流转、状态变更通知。
- Bug 管理：Bug 提交、查询、指派、关闭、评论、通知。
- 日报与汇报：个人日报、项目汇报、汇报准备工作。
- 文件附件：本地存储起步，统一 `FileStorageService`，已预留阿里云 OSS 实现。
- 通知中心：首页待办、未读通知、已读处理、任务到期/逾期自动通知。
- 导出与联调：任务、需求、Bug、甘特图、操作日志 Excel 导出；Swagger 全中文 `@Operation` 和 `@Schema` 字段说明。

完整覆盖矩阵见 [docs/requirements-coverage.md](docs/requirements-coverage.md)。

## 本地运行

```powershell
$env:JAVA_HOME='D:\devTool\jdk21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

默认配置：

- 服务地址：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- MySQL：`jdbc:mysql://localhost:3306/projectflow`
- Redis：`localhost:6379`
- 本地文件目录：`./data/uploads`

## 生产配置

生产环境使用 `prod` profile，通过环境变量注入数据库、Redis、JWT 和存储配置：

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/projectflow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='projectflow'
$env:DB_PASSWORD='change-me'
$env:REDIS_HOST='127.0.0.1'
$env:JWT_SECRET='replace-with-a-long-random-secret-at-least-32-bytes'
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

存储默认仍是 `local`，切换阿里云 OSS 时设置：

```powershell
$env:PROJECTFLOW_STORAGE_TYPE='aliyun-oss'
$env:ALIYUN_OSS_ENDPOINT='https://oss-cn-hangzhou.aliyuncs.com'
$env:ALIYUN_OSS_ACCESS_KEY_ID='your-access-key-id'
$env:ALIYUN_OSS_ACCESS_KEY_SECRET='your-access-key-secret'
$env:ALIYUN_OSS_BUCKET='your-bucket'
```

## 验证命令

```powershell
$env:JAVA_HOME='D:\devTool\jdk21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn "-Dmaven.repo.local=.m2/repository" test
mvn "-Dmaven.repo.local=.m2/repository" package
```
