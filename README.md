# JdbStress - 数据库压力测试工具

一个基于Spring Boot的数据库压力测试工具，提供Web界面进行实时监控和配置管理，支持多种数据库类型的性能测试。

## 🚀 主要功能

- **多数据库支持**: MySQL、PostgreSQL、Oracle、SQL Server、SQLite、DuckDB等
- **实时监控**: TPS、响应时间、成功率等关键指标实时展示
- **可视化图表**: 使用ECharts展示性能趋势图表
- **SQL执行器**: 直接执行SQL语句并查看结果
- **日志管理**: 实时查看应用程序日志
- **配置管理**: Web界面配置压测参数和数据源
- **自定义变量**: 支持时间戳、随机数、自增序列等动态变量生成

## 🛠️ 技术栈

### 后端技术
- **框架**: Spring Boot 2.7.5
- **数据库连接**: Spring JDBC + Druid连接池
- **构建工具**: Maven
- **日志**: SLF4J + Logback

### 前端技术
- **UI框架**: Bootstrap 5.1.3
- **图表库**: ECharts 5.4.3
- **样式**: 自定义CSS变量系统

### 支持的数据库驱动

#### 内置支持数据库
- MySQL Connector/J
- PostgreSQL JDBC Driver  
- Oracle JDBC Driver
- SQL Server JDBC Driver
- SQLite JDBC
- DuckDB JDBC

#### 扩展数据库支持

JdbStress通过自定义驱动加载机制支持任意JDBC兼容的数据库类型。系统会自动扫描并加载`src/main/resources/jdbc/`目录下的所有JDBC驱动jar文件。

**自定义驱动加载流程：**
1. 将目标数据库的JDBC驱动jar文件放入`src/main/resources/jdbc/`目录
2. 在`application.yml`中配置新的数据源连接信息
3. 系统启动时自动加载所有驱动并注册到ClassLoader
4. 通过反射机制动态添加驱动到系统类路径

**配置示例：**
```yaml
jdbc:
  datasources:
    your-db:  # 自定义数据库标识
      name: 您的数据库名称
      url: jdbc:yourdb://host:port/database
      driver-class-name: com.yourdb.jdbc.Driver  # 驱动类全限定名
      username: your_username
      password: your_password
```

**驱动加载特性：**
- 支持通配符路径匹配：`classpath*:jdbc/*.jar`
- 自动递归扫描指定目录下的所有jar文件
- 动态添加到系统ClassLoader，无需重启应用
- 详细的加载日志输出，便于调试

### 新增数据库压测指南

#### 连接池配置建议
对于不同的数据库类型，建议调整Druid连接池参数以获得最佳性能：

```yaml
druid:
  initial-size: 5      # 初始连接数，根据数据库并发能力调整
  min-idle: 3         # 最小空闲连接数
  max-active: 20       # 最大活动连接数
  max-wait: 60000     # 获取连接最大等待时间(ms)
  validation-query: "SELECT 1"  # 数据库健康检查SQL
  
  # 针对特定数据库的优化参数
  test-on-borrow: true
  test-on-return: false
  test-while-idle: true
  time-between-eviction-runs-millis: 60000
  min-evictable-idle-time-millis: 300000
```

#### 典型SQL操作模板

**1. OLTP数据库（如MySQL、PostgreSQL）**
```sql
-- 插入性能测试
INSERT INTO test_table (id, name, value, created_at) 
VALUES (:PI[1000], 'test_user', :pr, :pt)

-- 查询性能测试  
SELECT * FROM test_table WHERE id = :p1 AND status = 'ACTIVE'

-- 更新性能测试
UPDATE test_table SET value = :PR, updated_at = :pt 
WHERE id = :p2

-- 事务性能测试
BEGIN;
UPDATE accounts SET balance = balance - :p3 WHERE id = :p1;
UPDATE accounts SET balance = balance + :p3 WHERE id = :p2;
COMMIT;
```

**2. 分析型数据库（如ClickHouse、DuckDB）**
```sql
-- 聚合查询测试
SELECT category, COUNT(*), AVG(price), MAX(:pt) as ts
FROM products 
WHERE created_date >= :p1
GROUP BY category

-- 复杂Join测试
SELECT o.order_id, c.name, SUM(oi.quantity * oi.price)
FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN order_items oi ON o.order_id = oi.order_id
WHERE o.order_date BETWEEN :p2 AND :p3
GROUP BY o.order_id, c.name
```

**3. NoSQL数据库（如MongoDB、Cassandra）**
```sql
-- MongoDB示例（使用JSON格式）
INSERT INTO mongo_collection VALUES ('{
  "_id": ":PI[1000]", 
  "name": "test_doc", 
  "timestamp": ":pt",
  "data": {"field1": ":pr", "field2": ":PR"}
}')

-- 范围查询测试
SELECT * FROM mongo_collection 
WHERE timestamp > :p1 AND data.field1 = :p2
```

#### 性能指标采集方式

**关键性能指标：**
1. **TPS（每秒事务数）** - 衡量数据库处理能力
2. **响应时间** - 平均响应时间和P95/P99分位值
3. **成功率** - 请求成功比例
4. **连接池使用率** - 活跃连接数/最大连接数
5. **错误率** - 各类错误发生频率

**监控配置示例：**
```yaml
stress:
  sample-rate: 1      # 采样频率(秒)
  duration: 600       # 测试持续时间(秒)
  thread-pool:
    core-size: 10     # 根据数据库并发能力调整
    max-size: 50
    queue-capacity: 10000
```

**性能优化建议：**
1. 根据数据库类型调整批处理大小
2. 合理设置连接池参数避免资源浪费
3. 使用合适的索引策略
4. 监控数据库服务器资源使用情况
5. 逐步增加并发用户数观察性能拐点

## 📦 安装指南

### 环境要求
- Java 8+
- Maven 3.6+
- 支持的数据库之一

### 快速开始

1. **克隆项目**
```bash
git clone <项目地址>
cd JdbStress
```

2. **配置数据源**
编辑 `src/main/resources/application.yml`，配置您的数据库连接信息：

```yaml
spring:
  datasource:
    active: mysql  # 选择激活的数据源

jdbc:
  datasources:
    mysql:
      name: MySQL数据库
      url: jdbc:mysql://localhost:3306/your_database
      username: your_username
      password: your_password
```

3. **安装JDBC驱动**
将需要的JDBC驱动jar文件放入 `src/main/resources/jdbc/` 目录

4. **构建并运行**
```bash
mvn clean package
mvn spring-boot:run
```

5. **访问应用**
打开浏览器访问: http://localhost:8080

## 🎯 使用说明

### 1. 压力测试

#### 基本配置
1. 在Web界面选择"压力测试"标签页
2. 配置压测参数：
   - 线程池配置（核心线程数、最大线程数、队列容量）
   - 运行时间（秒）
   - 采样率（秒）
   - SQL语句和参数

3. 点击"开始压测"按钮启动测试
4. 实时查看性能指标和图表

#### 自定义变量功能

JdbStress支持在SQL语句中使用自定义变量，提供灵活的测试数据生成能力。变量语法格式如下：

##### 变量类型和使用规则

**1. 时间戳变量**
- `:pt` - 生成当前时间戳长整数（所有出现位置使用相同值）
  ```sql
  UPDATE users SET last_login = :pt WHERE id = :p1
  ```

**2. 时间戳+随机数变量**
- `:ps` - 生成时间戳字符串拼接随机数（所有出现位置使用相同值）
  ```sql
  INSERT INTO logs (timestamp, message) VALUES (:ps, 'test message')
  ```

**3. 随机数变量**
- `:pr` - 生成随机正整数（所有出现位置使用相同值）
- `:PR` - 生成随机正整数（每个出现位置生成不同值）
  ```sql
  -- :pr 所有位置相同值，:PR 每个位置不同值
  UPDATE products SET stock = :pr, price = :PR WHERE category = 'electronics'
  ```

**4. 自增整数变量**
- `:pi[起始值]` - 从指定值开始自增整数（所有出现位置使用相同值）
- `:PI[起始值]` - 从指定值开始自增整数（每个出现位置递增不同值）
  ```sql
  -- :pi[100] 所有位置相同值，:PI[1000] 每个位置递增不同值
  INSERT INTO orders (order_id, customer_id) VALUES (:pi[100], :PI[1000])
  ```

**5. 自增长整数变量**
- `:pl[起始值]` - 从指定值开始自增长整数（所有出现位置使用相同值）
- `:PL[起始值]` - 从指定值开始自增长整数（每个出现位置递增不同值）
  ```sql
  -- :pl[1000] 所有位置相同值，:PL[10000] 每个位置递增不同值
  UPDATE transactions SET amount = :pl[1000], sequence = :PL[10000]
  ```

**6. 参数替换变量**
- `:p1, :p2, :p3` - 使用CSV参数文件中对应位置的参数值
  ```sql
  SELECT * FROM users WHERE name = :p1 AND age > :p2
  ```

##### 作用域规则

- **全局相同值变量** (`:pt`, `:ps`, `:pr`, `:pi[n]`, `:pl[n]`)：在一次SQL执行中，所有相同变量标记使用相同的生成值
- **局部不同值变量** (`:PR`, `:PI[n]`, `:PL[n]`)：每次出现都生成新的值，适用于需要唯一值的场景

##### 兼容性处理

自定义变量功能与现有SQL生成逻辑完全兼容：
1. 变量解析在SQL执行前进行，不影响原有参数替换机制
2. 支持与传统的`:p1`, `:p2`参数混合使用
3. 变量解析错误会抛出明确异常，便于调试

##### 应用示例

```sql
-- 压力测试示例：插入带时间戳和唯一ID的记录
INSERT INTO user_actions (action_id, user_id, action_type, timestamp, details)
VALUES (:PI[1000], :p1, 'LOGIN', :pt, 'IP: 192.168.1.:pr')

-- 查询示例：使用随机范围条件
SELECT * FROM products 
WHERE price BETWEEN :pr AND :PR 
AND category_id = :pi[10]
LIMIT :p2
```

### 2. SQL执行

### 2. SQL执行

1. 切换到"SQL执行"标签页
2. 选择数据源
3. 输入SQL语句
4. 点击"执行SQL"查看结果

### 3. 日志查看

1. 切换到"日志"标签页
2. 选择要查看的行数
3. 点击"查看"按钮刷新日志

## ⚙️ 配置选项

### 主要配置参数

```yaml
# 服务器配置
server:
  port: 8080

# 数据源配置
jdbc:
  datasources:
    mysql:
      name: MySQL数据库
      url: jdbc:mysql://host:port/database
      driver-class-name: com.mysql.cj.jdbc.Driver
      username: username
      password: password

# Druid连接池配置
druid:
  initial-size: 10
  min-idle: 5
  max-active: 20
  max-wait: 60000

# 压测配置
stress:
  thread-pool:
    core-size: 10
    max-size: 32
    queue-capacity: 1000000
  duration: 300
  sample-rate: 3
  sql:
    file-path: classpath:sql/test.sql
    params-path: classpath:sql/params.csv
```

### 配置文件位置
- 主配置文件: `src/main/resources/application.yml`
- SQL模板文件: `src/main/resources/sql/`
- JDBC驱动: `src/main/resources/jdbc/`

## 🏗️ 项目结构

```
src/main/java/com/meigy/jstress/
├── config/          # 配置类
├── controller/      # Web控制器
├── core/           # 核心业务逻辑
├── exception/      # 异常处理
├── model/          # 数据模型
├── properties/     # 配置属性类
├── report/         # 报告生成
├── service/        # 服务层
└── types/          # 类型定义
```

## 🔧 API接口

### 压力测试接口
- `GET /api/stress/metrics` - 获取压测指标
- `POST /api/stress/start` - 开始压测
- `POST /api/stress/stop` - 停止压测
- `GET /api/stress/status` - 获取压测状态
- `GET /api/stress/config` - 获取配置
- `PUT /api/stress/config` - 更新配置

### 数据源接口
- `GET /api/stress/datasources` - 获取数据源列表
- `POST /api/stress/datasource/switch` - 切换数据源

### SQL执行接口
- `POST /api/stress/execute-sql` - 执行SQL语句

### 日志接口
- `GET /api/logs` - 获取日志内容

## 🐛 故障排除

### 常见问题

1. **端口占用**
   ```bash
   # 查找占用8080端口的进程
   netstat -ano | findstr :8080
   ```

2. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接字符串和凭据
   - 确认网络连通性

3. **缺少JDBC驱动**
   - 将驱动jar文件放入 `src/main/resources/jdbc/` 目录

### 日志查看
应用日志默认输出到 `logs/spring-boot.log` 文件

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

### 开发流程

1. Fork本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### 代码规范
- 遵循Java编码规范
- 使用Lombok减少样板代码
- 保持代码注释清晰

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- Spring Boot团队
- Bootstrap团队
- ECharts团队
- 所有贡献者

## 📞 支持

如果您遇到问题或有建议，请：
1. 查看[故障排除](#-故障排除)章节
2. 提交[Issue](https://github.com/your-repo/issues)
3. 联系开发团队

---

**注意**: 请确保在进行压力测试时获得相关数据库的授权，避免对生产环境造成影响。