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

数据库迁移 `V33` 会为需求和 Bug 编号建立唯一约束及事务性计数器。若历史数据存在空编号或重复编号，会保留重复组中最早记录的编号，并为其余记录分配新编号；上线前应在隔离的 MySQL 8 数据库中演练迁移并核对受影响的历史编号。

## 本地运行

```powershell
$env:JAVA_HOME='D:\devTool\jdk21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn "-Dmaven.repo.local=.m2/repository" spring-boot:run
```

默认配置：

- 服务地址：`http://localhost:8088`
- Swagger UI：`http://localhost:8088/swagger-ui/index.html`
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

## 项目工作台与交付协作

升级数据库时执行 `src/main/resources/db/migration/V34__work_delivery_and_collaboration.sql`。当前 `application.yml` 关闭了 Flyway，因此目标环境需要按数据库发布流程手工执行；若准备启用 Flyway，应先核对历史迁移记录。新表保存需求与任务的关联、需求验收、任务阻塞、评论、交付版本及个人常用筛选；已有工作项不会自动关联。

使用流程：在需求详情关联本项目或关联管理／执行项目的任务；在任务详情登记阻塞并查看关联需求，通过评论提及项目成员；需求审核人在任务完成、关联 Bug 关闭后记录验收；项目负责人在工作台选择交付范围，满足需求验收通过、任务完成且无阻塞、关联 Bug 关闭等条件后标记版本就绪，填写发布验收说明后发布。关联任务删除或缺失也会阻断就绪。需求关联范围变化会重置原验收结论，已发布版本不可编辑。

工作台可查看可见关联项目的需求、任务、Bug、附件数量和动态；任务、需求和 Bug 列表支持保存个人常用筛选，工作台支持标题及编号快速搜索。相关操作沿用项目、需求和任务的权限规则，个人筛选仅创建者可见。

### 版本迭代档案与上线申请

在 V34 之后依次执行 `src/main/resources/db/migration/V35__release_iteration_traceability.sql` 和 `src/main/resources/db/migration/V36__release_record_files.sql`。当前 `application.yml` 关闭 Flyway，目标环境需按数据库发布流程手工执行。V35 保存十个阶段的责任人、计划／实际日期与结论，以及不可覆盖的过程记录；V36 为过程记录关联直接上传的凭证文件。过程记录仍可填写评审文档、设计稿、测试报告等 http(s) 凭证链接。

迭代档案可在各阶段直接上传不超过 50 MB 的 PDF、Office 文档、图片和文本凭证。每次上传会追加带操作者与时间的记录，并保留可下载文件；迭代凭证不可从通用附件接口删除，含有迭代凭证的项目不能执行物理附件清理。审批中或已批准的申请需先撤回，才能补充上线前的凭证；发布后可继续在上线发布及上线后跟进阶段上传文件。

在工作台的交付版本中打开「迭代档案」，将本期需求和任务纳入版本，依次记录需求提出、需求评审、UI 设计、技术开发、提测、测试验证、产品验收、上线前评估、上线发布和上线后跟进。阶段完成必须填写责任人、实际完成日期与结论；返工需先从后续阶段依次重新打开，所有变更保留前后内容和操作人。

版本满足需求验收、任务完成和 Bug 关闭等条件后先标记「已就绪」。上线申请单记录环境、上线窗口、变更范围、影响、回滚、监控、值守与指定审批人。前八个阶段完成后才能提交申请；申请人不能自行审批。审批中及批准后冻结版本范围和前置阶段，项目负责人可填写原因撤回申请，调整后重新提交。批准后才能确认发布；版本范围变更与发布动作自动写入不可覆盖的记录。发布后仍可在「上线后跟进」追加监控、反馈和问题处理记录。历史已发布版本不会被自动补写阶段与申请记录。

公司 Bug 总列表、详情及导出向所有登录员工开放；「我的 Bug」仅查询本人创建或被指派的记录。所有登录员工均可选择项目提交 Bug，编辑、关闭和删除仍按原有角色及归属权限控制。Bug 导出的严重等级、状态和解决方案使用与页面一致的中文名称。
