---
name: ai-file-bridge-air-share
description: "通过 air-share（Java）的 /api/ai-files 接口上传与下载文件：免登录、可选静态密钥。默认公网 base https://file.kmgo.top，备用 https://file.cupb.top。在需要让 AI 或脚本跨环境传文件、拿直链时使用。"
---

# AI 文件桥接（air-share）

本仓库已实现接口，完整约定见 **`docs/ai-file-api.md`**。

## 何时使用

- 需要 **无 Web 登录、无 OAuth** 的机器间传文件
- 希望上传后得到 **可拼给模型的 downloadUrl**（依赖 `ai.file.public-base-url` / `AI_FILE_PUBLIC_BASE_URL`）
- 部署在 `file.kmgo.top` / `file.cupb.top` 且已将 `/api` 反代到本服务

## 基址

- 首选 API：`https://file.kmgo.top/api`
- 备用：`https://file.cupb.top/api`
- 本机：`http://127.0.0.1:<server.port>/api`（端口见 `application.yml` 的 `server.port`）

## 上传

`multipart/form-data`，字段名 **`file`**；可选 **`filename`** 指定展示名。

配置了 `upload_key` 时，使用其一：

- `Authorization: Bearer <key>`
- `X-AI-File-Key: <key>`

示例：

```bash
curl -sS -X POST "https://file.kmgo.top/api/ai-files/upload" \
  -H "Authorization: Bearer $AI_FILE_UPLOAD_KEY" \
  -F "file=@./artifact.zip"
```

成功响应 JSON 含 **`id`**、**`downloadUrl`**（当 `public-base-url` 非空或可通过反向代理头推断时）。

## 下载

```bash
curl -sS -L -o "./out.dat" "https://file.kmgo.top/api/ai-files/<id>" \
  -H "Authorization: Bearer $AI_FILE_UPLOAD_KEY"
```

未配置密钥时可省略 `Authorization`。若使用密钥，也可用查询参数：`?key=<key>`。

## 元数据

```bash
curl -sS "https://file.kmgo.top/api/ai-files/<id>/meta" \
  -H "Authorization: Bearer $AI_FILE_UPLOAD_KEY"
```

## 运维提示

- 功能总开关：`ai.file.enabled` 或环境变量 `AI_FILE_ENABLED=0`
- 密钥优先读环境变量 **`AI_FILE_UPLOAD_KEY`**（覆盖配置文件）
- 上传体积上限：`ai.file.max-upload-mb`

遇到 `503` 时先确认服务配置已开启；遇到 `401` 时检查密钥是否与服务器一致。
