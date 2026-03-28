# koog-docs-zh

一个可维护的 Koog 中文文档站脚手架。

这个目录按“独立仓库”组织，后续可以直接拆出去单独托管到 Vercel。

## 目录结构

- `upstream/`: 官方 Koog release 文档快照，禁止手改
- `site/`: 可发布的中文站源码
- `tools/`: 同步、差异分析、翻译、构建脚本
- `bin/`: 维护命令入口

## 维护命令

```bash
./bin/sync-upstream <tag>
./bin/diff-report
./bin/translate-changed
./bin/build-site
```

常用用法：

```bash
# 在当前 koog 仓库里做首个快照
./bin/sync-upstream 0.7.3 --source-repo ..

# 查看哪些页面是 new / changed / outdated / deleted
./bin/diff-report

# 用机器初译刷新新增或过期页面
./bin/translate-changed --provider google-web

# 人工审校完成后，标记页面为 reviewed
python3 tools/cli.py set-status site/docs/quickstart.md reviewed

# 本地构建
./bin/build-site
```

## 发布到 Vercel

这个脚手架已经包含 [`vercel.json`](./vercel.json)，默认构建流程是：

```bash
cd site
uv sync --frozen --all-extras
uv run mkdocs build
```

产物目录固定为仓库根目录下的 `dist/`。

当前默认仓库链接已指向你的 fork `https://github.com/maokaigang/koog`。

发布前仍需要替换的配置：

- `site/mkdocs.yml` 里的 `site_url`
- 页脚里的个人站/仓库链接

## 翻译策略

- 机器翻译只生成草稿，页面状态会被标为 `changed`
- 人工审校完成后，使用 `set-status` 标记为 `reviewed`
- 如果上游更新了已翻译页面，`sync-upstream` 会把它标为 `outdated`，不会静默覆盖旧内容

## 术语和风格

- 术语表见 [TERMINOLOGY.md](./TERMINOLOGY.md)
- 风格指南见 [STYLE_GUIDE.md](./STYLE_GUIDE.md)
