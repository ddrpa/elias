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
    @Index(desc = true)
    @NotNull
    private String name;

    @NotBlank
    @UniqueIndex(name = "uk_email_address")
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

- 已存在且定义一致的索引会自动跳过；
- 索引已存在但定义变化时，会建议 `drop index + create index`；
- 当 `elias.validate.auto-fix=true` 时，索引变更会自动重建。

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

WARN  SchemaChecker - Invalid schema definition in table `tbl_account`: column `order` uses reserved keyword. Identifier conflicts with MySQL reserved keyword.
```

## 类型映射规则

Elias 通过一组 `SpecBuilderFactory` 实现类型推断，按优先级顺序匹配：

| 优先级 | Factory | 匹配条件 | 映射结果 |
|--------|---------|----------|----------|
| 1 | `TypeOverrideSpecBuilderFactory` | 存在 `@TypeOverride` 注解 | 使用注解指定的类型 |
| 2 | `EncryptedSpecBuilderFactory` | `@IsEncrypted` | `varbinary(n)` |
| 3 | 语义化注解 factory（见下） | `@IsEmail` / `@IsPhone` / `@IsURL` / `@IsMimeType` / `@IsMoney` / `@IsPercentage` / `@IsJSON` / `@IsIP` / `@IsMacAddress` | 各注解预设类型 |
| 4 | `AutoSpecBuilderFactory` | `@Auto` | 按字段名 + Java 类型推断，兜底 `varchar(255)` |
| 5 | `TextSpecBuilderFactory` | `String`、`@UseText`、`@CharLength` | `varchar` / `text` / `mediumtext` |
| 6 | `IntegerSpecBuilderFactory` | `int`、`long`、`short`、`byte` 及包装类 | `int` / `bigint` / `smallint` |
| 7 | `DateTimeSpecBuilderFactory` | `LocalDate`、`LocalDateTime`、`Instant` 等 | `date` / `datetime` / `time` |
| 8 | `EnumSpecBuilderFactory` | 枚举类型 | `smallint` |
| 9 | `FloatSpecBuilderFactory` | `float`、`double` 及包装类 | `float` / `double` |
| 10 | `BooleanSpecBuilderFactory` | `boolean`、`Boolean` | `tinyint(1)` |
| 11 | `BigDecimalSpecBuilderFactory` | `BigDecimal`、`@Decimal` | `decimal(p, s)` |
| 12 | `InetAddressSpecBuilderFactory` | `InetAddress` | `varbinary` |
| 13 | `BinarySpecBuilderFactory` | `@IsHash`、`@IsUUID` | `binary(n)` |
| 14 | `BlobSpecBuilderFactory` | `byte[]`、`Blob` | `blob` |
| 15 | `CharSpecBuilderFactory` | `char`、`Character`、`@IsUUIDAsStr` | `char(1)` / `char(36)` |
| 16 | `GeometrySpecBuilderFactory` | `@IsGeo`、Geometry 类型 | `geometry` / `point` 等 |

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

#### @Index

用于字段级声明普通索引，会和 `@EliasTable.indexes` 一起合并。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | `String` | 自动生成 | 索引名称 |
| `group` | `String` | `""` | 索引分组，非空时参与同组联合索引 |
| `pos` | `int` | `0` | 同组中的列顺序，越小越靠前 |
| `desc` | `boolean` | `false` | 是否降序，`false`=ASC, `true`=DESC |

#### @UniqueIndex

用于字段级声明唯一索引，参数与 `@Index` 一致。

注意：
- `group=""` 时生成独立单列索引，`group!=""` 时按组装联合索引；
- 同一列可以同时声明独立索引和联合索引（使用重复注解）；
- 同组内按 `pos ASC` 排序；若 `pos` 相同，按列名字典序排序并输出 `WARN`；
- 对于复杂联合索引仍可继续使用 `@EliasTable.Index(columns = "a, b")`；
- 联合唯一索引要求每个成员列都是 `NOT NULL`（字段上使用 `@UniqueIndex`）。

示例：

```java
@Index(name = "idx_email")
@UniqueIndex(group = "uk_tenant_email", pos = 2)
private String email;

@UniqueIndex(group = "uk_tenant_email", pos = 1)
private Long tenantId;
```

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

列 comment 可从可选的 OpenAPI 注解读取（**无需** swagger 依赖；classpath 上有则生效）：

```java
@Schema(description = "登录用户名")
private String username;
```

- `CREATE TABLE` / `ADD COLUMN`：Spec 有非空 comment 时写入 `COMMENT '...'`
- `MODIFY COLUMN`：仅当库中 `COLUMN_COMMENT` 为空时补写；已有注释永不覆盖
- 任意 `MODIFY` 都会带上最终 comment（保留库侧或补 Spec），避免 MySQL 清空已有 COMMENT

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
@IsJSON(emptyAs = IsJSON.EmptyType.OBJECT)
private String settings;
```

注意：MySQL 5.7 / 8.0.13 之前的版本不允许 JSON 列声明 `DEFAULT` 值，
`emptyAs` 仅作为元数据保留，由应用层在写入时填充默认值；若字段同时声明 `@DefaultValue`，
DDL 生成时会跳过默认值并输出 `WARN` 日志。

#### @IsEmail / @IsPhone / @IsURL

通讯类字段，映射为 `varchar(length)`。

```java
@IsEmail                              // varchar(254)，参考 RFC 5321
private String emailAddress;

@IsPhone(length = 20)                 // varchar(20)
private String mobile;

@IsURL                                // varchar(2048)；length > 5000 时降级为 text
private String website;
```

#### @IsIP / @IsMacAddress

网络地址字段，按二进制存储以节省空间。

```java
@IsIP                                 // varbinary(16)，兼容 IPv4 / IPv6
private byte[] clientIp;

@IsIP(version = IPVersion.V4)         // binary(4)
private byte[] serverIp;

@IsMacAddress                         // binary(6)
private byte[] hardwareAddress;

@IsMacAddress(asString = true)        // char(17)
private String macText;
```

#### @IsMoney / @IsPercentage

金融数值字段，映射为 `decimal(p, s)`。

```java
@IsMoney                              // decimal(19, 4)
private BigDecimal totalAmount;

@IsPercentage                         // decimal(5, 2)
private BigDecimal taxRate;
```

#### @IsMimeType / @IsSlug

媒体与资源标识字段。

```java
@IsMimeType                           // varchar(127)，RFC 6838
private String contentType;
```

#### @Auto

根据字段的 Java 类型与字段名自动选择列类型；未命中任何规则时兜底为 `varchar(255)`。

```java
@Auto
private String emailAddress;          // 推断为 varchar(254)

@Auto
private Long userId;                  // 推断为 bigint(20) unsigned

@Auto
private byte[] passwordHash;          // 推断为 binary(32)

@Auto
private BigDecimal totalAmount;       // 推断为 decimal(19, 4)
```

推断规则（字段名先转 snake_case 全小写）：

| Java 类型 | 名称命中关键字 | 映射结果 |
|-----------|----------------|----------|
| `String` | `email` | `varchar(254)` |
| `String` | `phone` / `mobile` / `tel` | `varchar(32)` |
| `String` | `url` / `link` / `href` / `website` | `varchar(2048)` |
| `String` | `slug` | `varchar(255)` |
| `String` | `mime_type` / `content_type` | `varchar(127)` |
| `String` | `timezone` / `tz_name` | `varchar(64)` |
| `String` | `country_code` | `char(2)` |
| `String` | `currency_code` | `char(3)` |
| `String` | `language_code` / `locale` | `varchar(35)` |
| `String` | `color` / `colour` | `char(7)` |
| `String` | `mac_address` | `char(17)` |
| `String` | `uuid` / `guid` | `char(36)` |
| `String` | `ip_address` / `ip_addr` | `varchar(45)` |
| `String` | `password` / `secret` / `token` / `api_key` | `char(64)` |
| `String` | `description` / `content` / `bio` / `biography` / `remark` / `note` / `comment` / `body` / `summary` / `detail` | `text` |
| `String` | （兜底） | `varchar(255)` |
| `byte[]` | `sha512` | `binary(64)` |
| `byte[]` | `sha384` | `binary(48)` |
| `byte[]` | `sha256` / `sha_256` / `hash` / `digest` | `binary(32)` |
| `byte[]` | `sha1` / `sha_1` | `binary(20)` |
| `byte[]` | `md5` / `uuid` / `guid` | `binary(16)` |
| `byte[]` | `ip_address` / `ip_addr` | `varbinary(16)` |
| `byte[]` | （兜底） | `blob` |
| `Long` / `long` / `BigInteger` | 名字以 `_id` 结尾 | `bigint(20) unsigned` |
| `Long` / `long` / `BigInteger` | （兜底） | `bigint(20)` |
| `Integer` / `int` | 任意 | `int` |
| `Short` / `short` / `Byte` / `byte` | 任意 | `smallint` |
| `BigDecimal` | `price` / `amount` / `cost` / `fee` / `balance` / `money` / `salary` / `revenue` / `total` | `decimal(19, 4)` |
| `BigDecimal` | `percent` / `percentage` / `rate` / `ratio` | `decimal(5, 2)` |
| `BigDecimal` | （兜底） | `decimal(10, 2)` |
| `Boolean` / `boolean` | 任意 | `tinyint(1)` |
| `Float` / `Double` 及包装类 | 任意 | `double` |
| `LocalDate` / `java.sql.Date` | 任意 | `date` |
| `LocalTime` / `java.sql.Time` | 任意 | `time` |
| `LocalDateTime` / `Instant` / `ZonedDateTime` / `OffsetDateTime` / `Timestamp` / `java.util.Date` | 任意 | `datetime` |
| `enum` | 任意 | `smallint` |
| 其他类型 | （兜底） | `varchar(255)` |

注意事项：
- 若字段同时声明 `@CharLength`，且推断结果为 `varchar` / `char` / `text`，则用 `@CharLength.length()` 覆盖长度；
- `@TypeOverride` / `@IsEncrypted` 以及其他具体语义化注解（如 `@IsEmail`）的优先级高于 `@Auto`，同时声明时具体注解胜出；
- `_id` 后缀推断为 `bigint(20) unsigned` 时仅对非主键字段生效（主键由 `@TableId` 决定）。

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
4. **定义级风险检查**：在访问数据库元数据前检查实体定义是否存在高风险问题（例如标识符非法、命中 SQL 保留关键字、索引定义非法）

### 阻断级规则

以下问题被归类为“阻断级”：

- 表名/列名/索引名命中 MySQL 保留关键字（例如 `order`、`group`）
- 标识符不合法（空值、超长、非法字符、包含反引号）
- 索引定义非法（空列列表、引用不存在列、排序关键字不是 `ASC` / `DESC`、同表重复索引名）

当 `elias.validate.stop-on-mismatch=true` 时，命中阻断级问题会终止应用启动。

### 自动修复策略

启用 `auto-fix: true` 后，Elias 会自动执行以下修复：

- 创建缺失的表
- 添加缺失的列
- 修改列定义（仅限安全操作）

以下情况不会自动修复，需人工确认：

- 缩小数据类型（如 `BIGINT` 改为 `INT`）
- 缩短字符串长度
- 从 `NULL` 改为 `NOT NULL`
- 阻断级定义风险（关键字冲突、非法标识符、非法索引定义）

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
