# Elias

隆重介绍 Elias，可以：

- 把 Java POJOs 类转换成 MySQL Schema DDL
- 在 Spring Boot 项目启动时检查数据库 schema 是否和 Java POJOs 一致（并自动应用修改）

使用 Mybatis-plus 作为 ORM 层的 Java 项目，通常的工作路径是先创建数据库 schema，然后用代码生成器生成 Java POJOs 和相关的 DAO 层对象。可能是受了 JPA 影响，偏好「充血模型」的缘故，笔者不太喜欢这个工作流程：

1. 笔者习惯先编写 MVP 证实业务思路是可行的，这个时候持久层往往还在 H2 上，之后会迁移到 MySQL，有的时候随着设计的演进，还会迁移到 NoSQL 上；
2. 开发早期阶段改动最多的是 Java POJOs（和相应的 DTOs、VOs），笔者也会把一些简单的逻辑写在 POJOs 中，重新生成代码就会覆盖这些内容；
3. 笔者的团队只在具有一定规模的项目的 RC+ 分支中使用 Liquibase 控制 schema 的变更，团队成员如果合作一个模块，在没有协调好的情况下只有 Git 控制的 Java 代码能够拯救他们；
4. <del>笔者有时候会忘了应该在 MySQL 中为列设置什么类型；</del>

## How-To

- 你需要使用 JDK 11+ 来运行 Elias；
- Elias 设计为配合 Mybatis-plus 使用，缺失这项依赖也许会产生一些问题；

Elias 目前的版本为 `2.0.0`，你也可以通过 Maven SNAPSHOT 仓库访问 SNAPSHOT 版本。

```xml
<repositories>
    <repository>
        <id>snapshots</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>
```

### 使用 Elias 生成数据库建表语句

Elias 会扫描项目中的实体类，生成对应的 MySQL 建表语句，支持：

- 推断设置列的类型、长度
- 设置列是否可为空
- 设置默认值
- 声明并创建索引

在项目中添加如下依赖：

```xml
<dependency>
    <groupId>cc.ddrpa.dorian.elias</groupId>
    <artifactId>elias-generator</artifactId>
    <version>${elias.version}</version>
</dependency>
```

使用如下语句，Elias 会查找使用 `cc.ddrpa.dorian.elias.core.annotation.EliasTable` 或 `com.baomidou.mybatisplus.annotation.TableName` 注解标注的实体类。

```java
new SchemaFactory()
    .dropIfExists(true)
    .addAllAnnotatedClass("cc.ddrpa.dorian")
    .useAnnotation(com.baomidou.mybatisplus.annotation.TableName.class)
    .export("./target/generateTest.sql");
```

在实体类中，你可以使用 `cc.ddrpa.dorian.elias.core.annotation.EliasTable` 注解来声明表需要建立的索引，也可以使用其他一些注解来声明列的名称和类型。

```java
@EliasTable(
    enable = true,
    indexes = {
        @Index(columnList = "email_address", unique = true),
        @Index(columnList = "username"),
        @Index(columnList = "username, email_address", unique = true),
    }
)
@TableName("tbl_account")
public class Account {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("username")
    @NotNull
    private String name;
    @NotBlank
    private String emailAddress;
    @TypeOverride(type = "varchar", length = 500)
    private LocalDate createTime;
    private AccountStatus accountStatus;
    private byte[] avatar;
    @UseText
    private String biography;
}
```

最终得到这样的 SQL 语句：

```sql
drop table if exists `tbl_account`;
create table `tbl_account` (
   `id` int not null auto_increment
       primary key,
   `username` varchar(255) not null,
   `email_address` varchar(255) not null,
   `create_time` varchar(500) null,
   `account_status` tinyint(4) null,
   `avatar` blob(64000) null,
   `biography` varchar(16383) null
);
create unique index idx_unique_email_address on `tbl_account` (email_address);
create index idx_username on `tbl_account` (username);
create unique index idx_unique_username_email_address on `tbl_account` (username, email_address);
```

### 使用 Elias-Spring-Boot-Starter 在项目启动时检查数据库 schema

- 如果找不到对应实体类的表，输出建表 SQL 语句
- 如果找不到对应实体类属性的列，输出增加列 SQL 语句
- 如果实体类中的属性与表中列的属性不能匹配，输出修改 SQL 语句
- 在满足条件的情况下自动应用修改

在项目中添加如下依赖：

```xml
<dependency>
  <groupId>cc.ddrpa.dorian.elias</groupId>
  <artifactId>elias-spring-boot-starter</artifactId>
  <version>${elias.version}</version>
</dependency>
```

在 application.yaml 中添加配置：

```yaml
elias:
  validate:
    enable: true # 启用检查
    scan:
      # 为 com.baomidou.mybatisplus.annotation.TableName 注解标注的类也启用支持
      accept-mybatis-plus-table-name-annotation: true
      includes: # 在这些路径下寻找 EliasTable 标注的类 
        - cc.ddrpa.virke
    stop-on-mismatch: false # 如果 schema 不匹配，是否要停止应用
    auto-fix: false # 如果 schema 不匹配，是否要自动修复
```

在 `elias.validate.scan.includes` 中指定的包路径下，Elias 会寻找符合搜索要求的实体类，然后检查数据库 schema 是否和这些类的定义一致。其他配置保持默认的情况下，Elias 会在 Spring Boot 项目启动时输出类似这样的日志，可以看到其给出了创建表、创建 / 修改列的 SQL 建议，如果开启了 `elias.validate.auto-fix`，Elias 会尝试执行其中一部分 SQL。

```log
2024-09-04 17:03:36 [main] INFO  c.d.d.e.s.a.EliasAutoConfiguration - 
 _____ _ _           
|  ___| (_)          
| |__ | |_  __ _ ___ 
|  __|| | |/ _` / __|
| |___| | | (_| \__ \
\____/|_|_|\__,_|___/
              2.0.0

2024-09-04 17:03:36 [main] WARN  c.d.d.elias.spring.SchemaChecker - Expect column `create_user` in table `tbl_account` but not found.
Recommending fix with:
alter table `tbl_account` add column `create_user` bigint(20) null;

2024-09-04 17:03:36 [main] WARN  c.d.d.elias.spring.SchemaChecker - Column `quantity` in table `tbl_equipment` has specification mismatch:
* Column type not match: expected 'int', actual 'varchar(255)'
* Default value not match: expected '0', actual <null>
Auto-fix is not recommended due to:
* Reducing the size of a data type—like converting BIGINT to INT or DATETIME to DATE can cause truncation or loss of precision.
Ensure all values fit within the new constraints and try:
alter table `tbl_equipment` modify column `quantity` int default '0';

2024-09-04 17:03:36 [main] WARN  c.d.d.elias.spring.SchemaChecker - Expect table `tbl_maintenance_plan` but not found.
Recommending fix with:
create table `tbl_maintenance_plan` (
  `id` bigint(20) not null
      primary key,
  `device` varchar(255) null,
);
```

## Java POJOs 属性与数据库元素的转换规则

// TODO

## Schema 检查与 auto-fix

// TODO

## Roadmap

- 更多列的控制选项
    - [ ] 支持 Jakarta Persistence API 注解
    - [ ] 支持 com.baomidou.mybatisplus.annotation.TableField 注解中的 JDBC 类型声明
- [ ] 支持检查索引
- [ ] 支持多数据源
- [ ] 支持分表场景
