# AI 文件接口（免登录）

面向 AI、脚本与自动化工具的文件上传与下载，**不依赖** Web 登录态与 OAuth。可选 **静态上传密钥**（与站内用户 JWT 无关）。

部署在公网时，建议：

- 使用 HTTPS（如 `https://file.kmgo.top`）
- 生产环境配置 `AI_FILE_UPLOAD_KEY` 或 `ai.file.upload-key`
- 在反向代理上限制 `/api/ai-files` 的速率与体积

## 基址

| 环境 | API 根路径 |
|------|------------|
| 默认 | `https://file.kmgo.top` |
| 备用 | `https://file.cupb.top` |

接口路径统一为 **`/api/ai-files`**（若应用有 `context-path`，需加前缀）。

本机调试：`http://127.0.0.1:<server.port>/api/ai-files`（默认端口见 `application.yml` 的 `server.port`）。

## 配置项（application.yml / 环境变量）

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `ai.file.enabled` | `AI_FILE_ENABLED` | `false` / `0` 关闭接口，返回 `503` |
| `ai.file.upload-key` | `AI_FILE_UPLOAD_KEY` | 非空则上传/下载/查元数据需携带密钥；**环境变量优先** |
| `ai.file.public-base-url` | `AI_FILE_PUBLIC_BASE_URL` | 上传成功响应里的 `downloadUrl` 基址（无尾斜杠） |
| `ai.file.max-upload-mb` | `AI_FILE_MAX_UPLOAD_MB` | 单文件大小上限（MB），默认 `100` |

## 密钥传递方式（配置密钥后必填其一）

与 `Authorization: Bearer <用户JWT>` 可并存；此处校验的是 **静态密钥**，不是用户 JWT。

1. `Authorization: Bearer <upload-key>`
2. `X-AI-File-Key: <upload-key>`
3. 查询参数 `?key=<upload-key>`（适合仅 GET 的下载/元数据）

未配置密钥时，无需携带上述头或参数。

---

## 1. 上传

`POST /api/ai-files/upload`

- **Content-Type**: `multipart/form-data`
- **字段**
  - `file`（必填）：文件内容
  - `filename`（可选）：展示文件名；不传则使用客户端原始文件名

**成功**（HTTP `200`，JSON）：

```json
{
  "code": 200,
  "msg": "success",
  "id": "<fileUuid>",
  "fileName": "artifact.zip",
  "size": 1024,
  "downloadUrl": "https://file.kmgo.top/api/ai-files/<fileUuid>"
}
```

**失败示例**

| HTTP | code | 说明 |
|------|------|------|
| 400 | 400 | 非 multipart、缺少 `file` 字段等 |
| 401 | 401 | 配置了密钥但未携带或错误 |
| 503 | 503 | 功能关闭（`ai.file.enabled=false`）或磁盘剩余空间过低（与站内上传一致，约小于 5GB 时拒绝） |

**curl 示例**

```bash
curl -sS -X POST "https://file.kmgo.top/api/ai-files/upload" \
  -H "Authorization: Bearer $AI_FILE_UPLOAD_KEY" \
  -F "file=@./artifact.zip" \
  -F "filename=artifact.zip"
```

---

## 2. 下载

`GET /api/ai-files/{id}`

- `{id}` 为上传响应中的 `id`（即库表中的 `file_uuid`）
- 支持 `Range` 断点续传（实现与站内 `/file/download/{id}` 相同）
- 若配置密钥：使用 `Authorization` / `X-AI-File-Key` / `?key=`

```bash
curl -sS -L -o "./out.dat" "https://file.kmgo.top/api/ai-files/<id>" \
  -H "Authorization: Bearer $AI_FILE_UPLOAD_KEY"
```

---

## 3. 元数据

`GET /api/ai-files/{id}/meta`

```json
{
  "code": 200,
  "id": "<fileUuid>",
  "fileName": "artifact.zip",
  "size": 1024,
  "fileMd5": null,
  "createTime": "2026-04-06T12:00:00.000+00:00",
  "downloadUrl": "https://file.kmgo.top/api/ai-files/<fileUuid>"
}
```

不存在或磁盘文件缺失时 HTTP `404`。

---

## 与站内接口的关系

| 能力 | 站内 | AI 接口 |
|------|------|---------|
| 上传 | `POST /file/upload`（需登录） | `POST /api/ai-files/upload`（免登录，可选密钥） |
| 下载 | `GET /file/download/{id}` | `GET /api/ai-files/{id}` |

数据存储与 `FileModel` / 物理目录规则一致；AI 上传由服务端生成新的 `file_uuid`（随机 UUID）。

## Nginx 示例

将 API 反代到 Spring Boot 服务（示例端口 `8888`）：

```nginx
location /api/ai-files/ {
    proxy_pass http://127.0.0.1:8888;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    client_max_body_size 200m;
}
```

若未设置 `ai.file.public-base-url`，`downloadUrl` 会根据 `X-Forwarded-Proto` / `X-Forwarded-Host` 自动拼接。
