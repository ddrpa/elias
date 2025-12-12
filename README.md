# Elias

Elias 是一个 Java 实体类到 MySQL Schema 的映射工具，提供两项核心功能：

- **DDL 生成**：将 Java POJOs 转换为 MySQL 建表语句
- **Schema 校验**：在 Spring Boot 启动时检查数据库结构与实体类定义的一致性，并可选择自动修复

## 设计背景

使用 MyBatis-Plus 的项目通常采用「数据库优先」的开发流程：先设计表结构，再用代码生成器生成实体类。这种方式在以下场景中存在局限：

1. **快速原型阶段**：开发初期持久层可能运行在 H2 上，后续迁移到 MySQL 或 NoSQL
2. **频繁迭代**：实体类变更频繁，代码生成器会覆盖手工添加的业务逻辑
3. **团队协作**：多人并行开发时，Schema 变更难以协调，而 Java 代码可通过 Git 管理
4. **类型映射**：开发者需要记忆 Java 类型与 MySQL 类型的对应关系

Elias 采用「代码优先」的思路，以 Java 实体类为 Schema 的唯一真实来源。

## 系统要求

- JDK 17+（JDK 11 支持停留在 2.0.0 版本）
- MySQL 5.7+
- 可选：MyBatis-Plus 3.x（用于识别 `@TableName`、`@TableId` 等注解）

## 安装

当前稳定版本为 `2.5.2`。

### Maven Central

```xml
<dependency>
  <groupId>cc.ddrpa.dorian.elias</groupId>
  <artifactId>elias-generator</artifactId>
  <version>2.5.2</version>
</dependency>
```

### SNAPSHOT 版本

```xml
<repository>
  <id>central-portal-snapshots</id>
  <url>https://central.sonatype.com/repository/maven-snapshots/</url>
</repository>
```

## 模块结构

| 模块 | 说明 |
|------|------|
| `elias-core` | 核心库，包含注解定义、类型映射工厂、规格构建器 |
| `elias-generator` | DDL 生成器，将 TableSpec 渲染为 SQL 语句 |
| `elias-spring-boot-starter` | Spring Boot 集成，提供启动时 Schema 校验功能 |

## 快速开始

### 生成建表语句

添加依赖：

```xml
<dependency>
  <groupId>cc.ddrpa.dorian.elias</groupId>
  <artifactId>elias-generator</artifactId>
  <version>${elias.version}</version>
</dependency>
```

定义实体类：

```java
@EliasTable(
    indexes = {
        @Index(columns = "email_address", unique = true),
        @Index(columns = "username"),
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

    @UseText
    private String biography;
}
```

生成 SQL：

```java
new SchemaFactory()
    .addPackage("cc.ddrpa.dorian")
    .useAnnotation(TableName.class)
    .export("./schema.sql", new MySQL57Generator());
```

输出结果：

```sql
create table `tbl_account` (
  `id` int not null auto_increment primary key,
  `username` varchar(255) not null,
  `email_address` varchar(255) not null,
  `create_time` varchar(500) null,
  `account_status` smallint null,
  `biography` varchar(5000) null
);
create unique index uk_email_address on `tbl_account` (email_address);
create index idx_username on `tbl_account` (username);
```

### Spring Boot 集成

添加依赖：

```xml
<dependency>
  <groupId>cc.ddrpa.dorian.elias</groupId>
  <artifactId>elias-spring-boot-starter</artifactId>
  <version>${elias.version}</version>
</dependency>
```

配置 `application.yaml`：

```yaml
elias:
  validate:
    enable: true
    scan:
      accept-mybatis-plus-table-name-annotation: true
      includes:
        - cc.ddrpa.example.entity
    stop-on-mismatch: false
    auto-fix: false
```

启动时 Elias 会检查数据库 Schema 并输出差异报告：

```
WARN  SchemaChecker - Expect column `create_user` in table `tbl_account` but not found.
Recommending fix with:
alter table `tbl_account` add column `create_user` bigint(20) null;

WARN  SchemaChecker - Column `quantity` in table `tbl_equipment` has specification mismatch:
* Column type not match: expected 'int', actual 'varchar(255)'
* Default value not match: expected '0', actual <null>
Auto-fix is not recommended due to:
* Reducing the size of a data type can cause truncation or loss of precision.
Ensure all values fit within the new constraints and try:
alter table `tbl_equipment` modify column `quantity` int default '0';
```

## 类型映射规则

Elias 通过一组 `SpecBuilderFactory` 实现类型推断，按优先级顺序匹配：

| 优先级 | Factory | 匹配条件 | 映射结果 |
|--------|---------|----------|----------|
| 1 | `TypeOverrideSpecBuilderFactory` | 存在 `@TypeOverride` 注解 | 使用注解指定的类型 |
| 2 | `TextSpecBuilderFactory` | `String`、`@UseText`、`@CharLength` | `varchar` / `text` / `mediumtext` |
| 3 | `IntegerSpecBuilderFactory` | `int`、`long`、`short`、`byte` 及包装类 | `int` / `bigint` / `smallint` |
| 4 | `DateTimeSpecBuilderFactory` | `LocalDate`、`LocalDateTime`、`Instant` 等 | `date` / `datetime` / `time` |
| 5 | `EnumSpecBuilderFactory` | 枚举类型 | `smallint` |
| 6 | `FloatSpecBuilderFactory` | `float`、`double` 及包装类 | `float` / `double` |
| 7 | `BooleanSpecBuilderFactory` | `boolean`、`Boolean` | `tinyint(1)` |
| 8 | `BigDecimalSpecBuilderFactory` | `BigDecimal`、`@Decimal` | `decimal(p, s)` |
| 9 | `InetAddressSpecBuilderFactory` | `InetAddress` | `varbinary` |
| 10 | `BinarySpecBuilderFactory` | `@IsHash`、`@IsUUID` | `binary(n)` |
| 11 | `BlobSpecBuilderFactory` | `byte[]`、`Blob` | `blob` |
| 12 | `CharSpecBuilderFactory` | `char`、`Character` | `char(1)` |
| 13 | `GeometrySpecBuilderFactory` | `@IsGeo`、Geometry 类型 | `geometry` / `point` 等 |

若无匹配，回退到 `varchar(5000)`。

### 整数类型映射

| Java 类型 | MySQL 类型 |
|-----------|------------|
| `byte` / `Byte` / `short` / `Short` | `smallint` |
| `int` / `Integer` | `int` |
| `long` / `Long` / `BigInteger` | `bigint(20)` |

### 日期时间类型映射

| Java 类型 | MySQL 类型 |
|-----------|------------|
| `LocalDate` / `java.sql.Date` | `date` |
| `LocalTime` / `java.sql.Time` | `time` |
| `LocalDateTime` / `Instant` / `ZonedDateTime` / `Timestamp` | `datetime` |

### 字符串类型映射

| 条件 | MySQL 类型 |
|------|------------|
| 默认 | `varchar(255)` |
| `@CharLength(length = n)` | `varchar(n)` |
| `@CharLength(length = n, fixed = true)` | `char(n)` |
| `@UseText` | 根据 `estimated` 值选择 `varchar` / `text` / `mediumtext` / `longtext` |

## 注解参考

### 表级注解

#### @EliasTable

标记实体类参与 Schema 生成和校验。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | `boolean` | `true` | 是否启用 |
| `tablePrefix` | `String` | `""` | 表名前缀 |
| `indexes` | `Index[]` | `{}` | 索引定义 |
| `spatialIndexes` | `Index[]` | `{}` | 空间索引定义 |
| `autoSpatialIndexForGeometry` | `boolean` | `true` | 自动为非空几何列创建空间索引 |

#### @EliasTable.Index

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | 自动生成 | 索引名称 |
| `columns` | `String` | 必填 | 列名列表，逗号分隔，支持 `ASC` / `DESC` |
| `unique` | `boolean` | `false` | 是否为唯一索引 |

### 列级注解

#### @TypeOverride

覆盖默认类型推断，优先级最高。

```java
@TypeOverride(type = "varchar", length = 500)
private String description;

// or
@TypeOverride(type = "varchar(500)")
private String description;
```

#### @DefaultValue

设置列的默认值。

```java
@DefaultValue("0")
private Integer status;
```

#### @EliasIgnore

忽略该字段，不生成对应列。

#### @UseText

将字符串映射为 TEXT 系列类型。

```java
@UseText(estimated = 100000)  // 根据预估长度选择 text/mediumtext/longtext
private String content;
```

#### @CharLength

指定字符串长度。

```java
@CharLength(length = 32, fixed = true)  // char(32)
private String code;
```

#### @Decimal

指定 BigDecimal 的精度和小数位。

```java
@Decimal(precision = 18, scale = 4)
private BigDecimal amount;
```

### 语义化注解

位于 `cc.ddrpa.dorian.elias.core.annotation.preset` 包下。

#### @IsHash

存储哈希值，映射为 `BINARY(n)`。

```java
@IsHash(HashType.SHA256)  // BINARY(32)
private byte[] contentHash;
```

支持的算法：

| 算法 | 长度 |
|------|------|
| `XX_HASH64` | 8 |
| `MD5` | 16 |
| `SHA1` | 20 |
| `SHA256` | 32 |
| `SHA384` | 48 |
| `SHA512` | 64 |
| `MURMUR3_128` | 16 |
| `BLAKE2B_256` | 32 |
| `BLAKE2B_512` | 64 |

#### @IsUUID / @IsUUIDAsStr

存储 UUID。

```java
@IsUUID           // BINARY(16)
private byte[] id;

@IsUUIDAsStr      // CHAR(36)
private String id;
```

#### @IsJSON

存储 JSON 数据，映射为 MySQL `JSON` 类型。

```java
@IsJSON(emptyAs = IsJSON.EmptyType.OBJECT)  // 空值默认为 {}
private String settings;
```

#### @IsGeo

存储地理空间数据。

```java
@IsGeo(type = SpatialDataType.POINT, srid = 4326)
private Object location;
```

支持的空间类型：`GEOMETRY`、`POINT`、`LINESTRING`、`POLYGON`、`MULTIPOINT`、`MULTILINESTRING`、`MULTIPOLYGON`、`GEOMETRYCOLLECTION`

### MyBatis-Plus 注解兼容

Elias 识别以下 MyBatis-Plus 注解：

| 注解 | 作用 |
|------|------|
| `@TableName` | 指定表名 |
| `@TableId` | 标记主键，支持 `IdType.AUTO` 自增 |
| `@TableField` | 指定列名，`exist = false` 时忽略该字段 |
| `@TableLogic` | 逻辑删除字段，默认值设为 `0` |

### Jakarta Validation 注解兼容

以下注解会将列设为 `NOT NULL`：

- `@NotNull`
- `@NotEmpty`
- `@NotBlank`

## Schema 校验与自动修复

### 校验行为

Elias 在 Spring Boot 启动时执行以下检查：

1. **表不存在**：输出完整的 `CREATE TABLE` 语句
2. **列不存在**：输出 `ALTER TABLE ... ADD COLUMN` 语句
3. **列定义不匹配**：比较类型、长度、是否可空、默认值，输出 `ALTER TABLE ... MODIFY COLUMN` 语句

### 自动修复策略

启用 `auto-fix: true` 后，Elias 会自动执行以下修复：

- 创建缺失的表
- 添加缺失的列
- 修改列定义（仅限安全操作）

以下情况不会自动修复，需人工确认：

- 缩小数据类型（如 `BIGINT` 改为 `INT`）
- 缩短字符串长度
- 从 `NULL` 改为 `NOT NULL`

### 配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `elias.validate.enable` | `boolean` | `false` | 启用 Schema 校验 |
| `elias.validate.scan.includes` | `List<String>` | `[]` | 扫描的包路径 |
| `elias.validate.scan.accept-mybatis-plus-table-name-annotation` | `boolean` | `true` | 识别 `@TableName` 注解 |
| `elias.validate.stop-on-mismatch` | `boolean` | `false` | 发现不匹配时停止应用启动 |
| `elias.validate.auto-fix` | `boolean` | `false` | 自动执行修复 SQL |

## 技术实现

### 架构概览

```
Java Entity Class
       |
       v
  SpecMaker.makeTableSpec()
       |
       v
  TableSpec (表规格对象)
       |
       +---> MySQL57Generator.createTable() ---> DDL SQL
       |
       +---> SchemaChecker.check() ---> 差异报告 / 修复 SQL
```

### 核心组件

- **SpecMaker**：遍历实体类字段，调用 SpecBuilderFactory 链生成 ColumnSpec
- **SpecBuilderFactory**：类型映射工厂接口，每种 Java 类型对应一个实现
- **TableSpec / ColumnSpec**：中间表示，与具体数据库无关
- **MySQL57Generator**：使用 Pebble 模板引擎渲染 SQL
- **SchemaChecker**：通过 `INFORMATION_SCHEMA.COLUMNS` 获取数据库元数据并比对

### 扩展点

实现 `SpecBuilderFactory` 接口可添加自定义类型映射：

```java
public class CustomTypeFactory implements SpecBuilderFactory {
    @Override
    public boolean fit(String fieldTypeName, Field field) {
        return field.isAnnotationPresent(CustomAnnotation.class);
    }

    @Override
    public ColumnSpecBuilder builder(Field field) {
        return SpecBuilderFactory.super.builder(field)
            .setDataType("custom_type")
            .setLength(100);
    }
}
```

## 常见问题

### Bean 初始化顺序问题

**问题**：`InitializingBean` 或 `@PostConstruct` 方法在 Elias 之前访问数据库。

**解决方案**：在相关 Bean 中注入 `EliasAutoConfiguration`，强制 Spring 先初始化 Elias：

```java
@Component
public class MyBean implements InitializingBean {
    @Autowired
    private EliasAutoConfiguration eliasAutoConfiguration;

    @Override
    public void afterPropertiesSet() {
        // 此时 Elias 已完成 Schema 校验
    }
}
```

### H2 兼容模式

生成可在 H2 数据库执行的 SQL：

```java
new MySQL57Generator()
    .enableH2Compatibility()
    .createTable(tableSpec);
```

## Roadmap

- [ ] 支持 Jakarta Persistence API 注解（`@Column`、`@Table` 等）
- [ ] 支持 `@TableField` 中的 JDBC 类型声明
- [ ] 索引定义校验
- [ ] 多数据源支持
- [ ] 分表场景支持

## 许可证

Apache License 2.0
