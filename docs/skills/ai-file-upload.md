# AI 文件上传接口（免认证）

## 概述

本项目提供 `/api/ai-files` 免认证文件上传下载接口，供 AI 代理（Agent）在无登录态下上传文件。

## 接口清单

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/ai-files/upload` | 上传文件 | 否（可选密钥校验） |
| GET | `/api/ai-files/{uuid}` | 下载文件 | 否 |
| GET | `/api/ai-files/{uuid}/meta` | 查询文件元数据 | 否 |

## 上传示例

```bash
curl -X POST "https://file.kmgo.top/api/ai-files/upload" \
  -F "file=@/path/to/file.txt" \
  -F "filename=rename.txt"
```

响应示例：

```json
{
  "code": 200,
  "msg": "success",
  "id": "1f36ec9a-a5b4-42d0-941a-44f8f8acbd1a",
  "fileName": "rename.txt",
  "size": 1024,
  "downloadUrl": "https://file.kmgo.top/api/ai-files/1f36ec9a-a5b4-42d0-941a-44f8f8acbd1a"
}
```

## 关键实现细节

### 1. serverName 域名一致性

文件列表页（`/#/file`）按 `serverName` 字段过滤查询。AI 上传时必须写入**公网域名**（而非实际请求的域名），否则网页端看不到。

实现：`AiFileServiceImpl.upload()` 中从 `AiFileProperties.publicBaseUrl` 提取域名作为 `serverName`：

```java
String publicBase = aiFileProperties.getPublicBaseUrl();
if (StringUtils.hasText(publicBase)) {
    serverName = publicBase.replaceFirst("^https?://", "").split("/")[0].split(":")[0];
} else {
    serverName = request.getServerName();
}
```

配置在 `AiFileProperties.publicBaseUrl`（默认 `https://file.kmgo.top`）。

### 2. Nginx 反代路径兼容

常见 Nginx 反代配置使用 `proxy_pass http://upstream/`（无尾斜杠），会把 `/api` 前缀剥掉。

因此控制器和 SecurityConfig 均同时支持两种路径：

- 控制器：`@RequestMapping({"/api/ai-files", "/ai-files"})`
- SecurityConfig：`permitAll("/api/ai-files/**", "/ai-files/**")`
- 未授权响应：`/ai-files/` 开头也返回 JSON 401，避免重定向到登录页

### 3. 密钥校验（可选）

配置环境变量 `AI_FILE_UPLOAD_KEY` 后，上传请求需携带：

```bash
curl -X POST "https://file.kmgo.top/api/ai-files/upload" \
  -H "Authorization: Bearer <你的密钥>" \
  -F "file=@file.txt"
```

## 相关文件

| 文件 | 说明 |
|------|------|
| `src/main/java/hxy/dragon/controller/AiFileController.java` | 控制器，同时映射双路径 |
| `src/main/java/hxy/dragon/service/impl/AiFileServiceImpl.java` | 上传逻辑，serverName 一致性处理 |
| `src/main/java/hxy/dragon/config/AiFileProperties.java` | publicBaseUrl 配置 |
| `src/main/java/hxy/dragon/config/security/SecurityConfig.java` | 放行规则与 401 响应处理 |

## 部署

```bash
./gradlew clean bootJar -x test
scp build/libs/air-share-0.0.1-SNAPSHOT.jar <user>@<host>:/media/data/app/air-share
ssh <user>@<host> sh /media/data/app/air-share/start.sh
```
