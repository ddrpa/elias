# Elias

隆重介绍 Elias

可以：

- 把 Java POJOs 类转换成 MySQL Schema DDL
- 在 Spring Boot 项目启动时检查数据库 schema 是否和 Java POJOs 一致（并自动应用修改）

使用 Mybatis-plus 作为 ORM 层的 Java 项目，通常的工作路径是先创建数据库 schema，然后用代码生成器生成
Java POJOs 和相关的 DAO 层对象。可能是受了 JPA 影响，偏好「充血模型」的缘故，笔者不太喜欢这个工作流程：

1. 笔者习惯先编写 MVP 证实业务思路是可行的，这个时候持久层往往还在 H2 上，之后会迁移到
   MySQL，有的时候随着设计的演进，还会迁移到 NoSQL 上；
2. 开发早期阶段改动最多的是 Java POJOs（和相应的 DTOs、VOs），笔者也会把一些简单的逻辑写在 POJOs
   中，重新生成代码就会覆盖这些内容；
3. 笔者的团队只在具有一定规模的项目的 RC+ 分支中使用 Liquibase 控制 schema
   的变更，团队成员如果合作一个模块，在没有协调好的情况下只有 Git 控制的 Java 代码能够拯救他们；
4. <del>笔者有时候会忘了应该在 MySQL 中为列设置什么类型；</del>

## How-To

- 你需要使用 JDK 17+ 来运行 Elias；
- Elias 设计为配合 Mybatis-plus 使用，缺失这项依赖也许会产生一些问题；

Elias 目前的版本为 `2.0.0`，你也可以通过 Maven SNAPSHOT 仓库访问 SNAPSHOT 版本，目前为
`2.5.2-SNAPSHOT`，对 JDK 11 的支持停留在 `2.0.0` 和 `2.1.0-SNAPSHOT` 版本。

```xml
<repository>
  <id>central-portal-snapshots</id>
  <url>https://central.sonatype.com/repository/maven-snapshots/</url>
</repository>
```

### 使用 Elias 生成数据库建表语句

Elias 会扫描项目中的实体类，生成对应的 MySQL 建表语句，支持：

- 推断设置列的类型、长度
- 设置列是否可为空
- 设置默认值
- 声明并创建索引（和空间索引）

在项目中添加如下依赖：

```xml
<dependency>
  <groupId>cc.ddrpa.dorian.elias</groupId>
  <artifactId>elias-generator</artifactId>
  <version>${elias.version}</version>
</dependency>
```

使用如下语句，Elias 会查找使用 `cc.ddrpa.dorian.elias.core.annotation.EliasTable` 或
`com.baomidou.mybatisplus.annotation.TableName` 注解标注的实体类。

```java
new SchemaFactory()
    .dropIfExists(true)
    .addPackage("cc.ddrpa.dorian")
    .useAnnotation(com.baomidou.mybatisplus.annotation.TableName .class)
    .export("./target/generateTest.sql");
```

在实体类中，你可以使用 `cc.ddrpa.dorian.elias.core.annotation.EliasTable`
注解来声明表需要建立的索引，也可以使用其他一些注解来声明列的名称和类型。

```java
@EliasTable(
    enable = true,
    indexes = {
        @Index(columns = "email_address", unique = true),
        @Index(columns = "username"),
        @Index(columns = "username, email_address", unique = true),
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
create table `tbl_account`
(
    `id`             int            not null auto_increment
        primary key,
    `username`       varchar(255)   not null,
    `email_address`  varchar(255)   not null,
    `create_time`    varchar(500)   null,
    `account_status` tinyint(4)     null,
    `avatar`         blob(64000)    null,
    `biography`      varchar(16383) null
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

在 `elias.validate.scan.includes` 中指定的包路径下，Elias 会寻找符合搜索要求的实体类，然后检查数据库
schema 是否和这些类的定义一致。其他配置保持默认的情况下，Elias 会在 Spring Boot
项目启动时输出类似这样的日志，可以看到其给出了创建表、创建 / 修改列的 SQL 建议，如果开启了
`elias.validate.auto-fix`，Elias 会尝试执行其中一部分 SQL。

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

参见 `cc.ddrpa.dorian.elias.core.factory` 下的 `SchemaFactory` 实现类。

## 语义化注解

Elias 提供了一组语义化注解（Preset Annotations），用于声明字段的语义类型，自动映射到合适的 MySQL 数据类型和存储格式。这些注解位于 `cc.ddrpa.dorian.elias.core.annotation.preset` 包下。

### @IsHash - 哈希值存储

用于声明字段存储哈希值，支持多种哈希算法，自动映射为 `BINARY` 类型并设置合适的长度。

```java
import cc.ddrpa.dorian.elias.core.annotation.preset.IsHash;

public class FileRecord {
    // 默认使用 xxHash64，映射为 BINARY(8)
    @IsHash
    private byte[] fileHash;
    
    // 使用 SHA-256，映射为 BINARY(32)
    @IsHash(HashType.SHA256)
    private byte[] contentHash;
    
    // 使用 MD5，映射为 BINARY(16)
    @IsHash(HashType.MD5)
    private byte[] checksum;
}
```

**支持的哈希算法：**

| 算法 | 长度（字节） | 说明 |
|------|------------|------|
| `XX_HASH64` | 8 | 默认，高性能非加密哈希 |
| `MD5` | 16 | 128 位消息摘要 |
| `SHA1` | 20 | 160 位安全哈希 |
| `SHA256` | 32 | 256 位安全哈希 |
| `SHA384` | 48 | 384 位安全哈希 |
| `SHA512` | 64 | 512 位安全哈希 |
| `MURMUR3_128` | 16 | 128 位 MurmurHash3 |
| `BLAKE2B_256` | 32 | 256 位 BLAKE2b |
| `BLAKE2B_512` | 64 | 512 位 BLAKE2b |

### @IsUUID - UUID 二进制存储

用于声明字段存储 UUID，以二进制格式存储（128 位），映射为 `BINARY(16)`，相比字符串存储节省空间。

```java
import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUID;

public class Entity {
    // 映射为 BINARY(16)
    @IsUUID
    private byte[] entityId;
}
```

### @IsUUIDAsStr - UUID 字符串存储

用于声明字段存储 UUID，以字符串格式存储，映射为 `CHAR(36)`。

```java
import cc.ddrpa.dorian.elias.core.annotation.preset.IsUUIDAsStr;

public class Entity {
    // 映射为 CHAR(36)
    @IsUUIDAsStr
    private String entityId;
}
```

### @IsJSON - JSON 数据存储

用于声明字段存储 JSON 数据，映射为 MySQL 5.7.8+ 支持的 `JSON` 类型。

```java
import cc.ddrpa.dorian.elias.core.annotation.preset.IsJSON;

public class Configuration {
    // 空值默认为 JSON 对象 {}
    @IsJSON(emptyAs = IsJSON.EmptyType.OBJECT)
    private String settings;
    
    // 空值默认为 JSON 数组 []
    @IsJSON(emptyAs = IsJSON.EmptyType.ARRAY)
    private String tags;
}
```

### @IsGeo - 地理空间数据存储

用于声明字段存储地理空间数据，映射为 MySQL 5.7.5+ 支持的空间数据类型。

```java
import cc.ddrpa.dorian.elias.core.annotation.preset.IsGeo;
import cc.ddrpa.dorian.elias.core.annotation.enums.SpatialDataType;

public class Location {
    // 默认使用 GEOMETRY 类型，WGS84 坐标系（SRID 4326）
    @IsGeo
    private Object position;
    
    // 使用 POINT 类型存储点坐标
    @IsGeo(type = SpatialDataType.POINT, nullable = true)
    private Object coordinates;
    
    // 使用 POLYGON 类型存储多边形区域
    @IsGeo(type = SpatialDataType.POLYGON, srid = 4326)
    private Object area;
}
```

**支持的空间数据类型：**

- `GEOMETRY` - 通用几何类型（默认）
- `POINT` - 点
- `LINESTRING` - 线串
- `POLYGON` - 多边形
- `MULTIPOINT` - 多点
- `MULTILINESTRING` - 多线串
- `MULTIPOLYGON` - 多多边形
- `GEOMETRYCOLLECTION` - 几何集合

**参数说明：**

- `type` - 空间数据类型，默认为 `GEOMETRY`
- `srid` - 空间参考系统标识符，默认为 `4326`（WGS84 坐标系）
- `nullable` - 是否允许 NULL 值，默认为 `false`

## Schema 检查与 auto-fix

// TODO

## Roadmap

- 更多列的控制选项
    - [ ] 支持 Jakarta Persistence API 注解
    - [ ] 支持 com.baomidou.mybatisplus.annotation.TableField 注解中的 JDBC 类型声明
- [ ] 支持检查索引
- [ ] 支持多数据源
- [ ] 支持分表场景

## 常见问题

Q: 我的项目中有一些 `org.springframework.beans.factory.InitializingBean` 实现类 / 使用
`@PostConstruct` 修饰的方法在 Elias 之前访问了数据库，有什么办法可以指定顺序吗？

A: 目前没想到什么好方法。可以试试在这些 Bean 中注入
`cc.ddrpa.dorian.elias.spring.autoconfigure.EliasAutoConfiguration` 实例，向 Spring Boot 强调先后顺序
