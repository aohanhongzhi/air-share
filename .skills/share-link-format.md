# Skill: 分享链接格式修改与测试

## 背景

分享链接原格式：`/api/file/download?fileUuid={md5}`
问题：链接中没有文件名，下载工具无法获取真实文件名

## 目标格式

新格式：`/api/file/download/{md5}/{fileName}`

- 后端仍使用 md5 查找文件
- 文件名仅用于下载工具识别

## 后端修改点

### 文件: `src/main/java/hxy/dragon/controller/FileController.java`

#### 1. 添加 import

```java
import org.springframework.web.bind.annotation.PathVariable;
```

#### 2. 修改 download 方法

```java
/**
 * 文件下载，直接文件地址
 */
@GetMapping({"/file/download/{fileUuid}", "/file/download/{fileUuid}/{fileName}"})
public void download(@PathVariable String fileUuid,
                     @PathVariable(required = false) String fileName,
                     HttpServletRequest request,
                     HttpServletResponse response,
                     @RequestHeader(name = "Range", required = false) String range) {
    fileService.downloadByFileId(fileUuid, request, response, range);
}
```

### 安全配置 (无需修改)

`SecurityConfig.java` 已配置 `/file/download/**` 为 permitAll：

```java
.requestMatchers("/file/download/**").permitAll()
```

## 测试流程

### 1. 编译后端

```bash
cd /home/eric/Project/Java/air-share
./gradlew build -x test
```

### 2. 启动后端

```bash
java -jar build/libs/air-share-0.0.1-SNAPSHOT.jar > /tmp/backend.log 2>&1 &
sleep 10
```

### 3. 验证下载接口

```bash
# 测试新格式 (带文件名)
curl -sI "http://localhost:8888/file/download/{md5}/{fileName}" | head -10

# 测试旧格式 (向后兼容)
curl -sI "http://localhost:8888/file/download/{md5}" | head -10

# 测试 URL 编码的中文文件名
curl -sI "http://localhost:8888/file/download/b020c740a2c09454d08036d32a5959b8/%E9%9C%80%E8%A6%8121%E5%B0%8F%E6%97%B6.png"
```

### 4. 验证 Content-Disposition

```bash
curl -sI "http://localhost:8888/file/download/{md5}/{fileName}" | grep -i content-disposition
# 预期: content-disposition: inline; filename="{真实文件名}"
```

### 5. 结合前端测试

```bash
# 通过前端代理测试
curl -sI "http://localhost:5173/api/file/download/{md5}/{fileName}" | head -10
```

## 预期结果

1. **HTTP 200 响应**：文件正常下载
2. **Content-Disposition 正确**：返回真实文件名
3. **向后兼容**：两种格式都能正常工作
4. **Range 支持**：大文件支持断点续传

## API 格式说明

| 格式                    | 说明         | 示例                             |
| ----------------------- | ------------ | -------------------------------- |
| `{fileUuid}`            | 仅 MD5       | `/file/download/abc123`          |
| `{fileUuid}/{fileName}` | MD5 + 文件名 | `/file/download/abc123/test.png` |

fileName 参数可选，后端通过 fileUuid 查找文件，fileName 仅用于响应头的 Content-Disposition。

## 注意事项

- 文件名需要 URL 编码：`encodeURIComponent(fileName)`
- Spring 的 `@PathVariable` 会自动解码
- 中文文件名需确保 UTF-8 编码正确
