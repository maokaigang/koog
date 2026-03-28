#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from textwrap import dedent
from typing import Iterable
from urllib.parse import quote
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
UPSTREAM_DIR = ROOT / "upstream"
SITE_DIR = ROOT / "site"
SITE_DOCS_DIR = SITE_DIR / "docs"
DIST_DIR = ROOT / "dist"
UPSTREAM_REPO_URL = "https://github.com/JetBrains/koog.git"
UPSTREAM_DOCS_ROOT = Path("docs/docs")
UPSTREAM_MKDOCS = Path("docs/mkdocs.yml")
UPSTREAM_PYPROJECT = Path("docs/pyproject.toml")
UPSTREAM_PYTHON_VERSION = Path("docs/.python-version")
UPSTREAM_UV_LOCK = Path("docs/uv.lock")
UPSTREAM_LICENSE = Path("LICENSE.txt")
META_PATTERN = re.compile(r"^<!-- koog-zh-meta: (?P<meta>\{.*\}) -->\n?", re.DOTALL)
KNIT_COMMENT_RE = re.compile(r"(?ms)^[ \t]*<!---.*?-->[ \t]*\n?")
STATUS_ORDER = ("new", "changed", "outdated", "deleted", "reviewed")
LOCAL_EXTRA_FILES = {
    "stylesheets/translation-extra.css",
}
PLACEHOLDER_PATTERN = re.compile(r"@@koogzh_[a-z]+_(?P<index>\d+)@@")
HEADING_PATTERN = re.compile(r"^#\s+(?P<title>.+?)\s*$", re.MULTILINE)
HEADING_ATTRS_SUFFIX = re.compile(r"\s+\{\s*#[^}]+\}\s*$")
MERGED_TAB_PATTERN = re.compile(r'^(?P<indent>\s*)(?P<header>===\s+"[^"]+")(?P<rest>\S.*)$')
MERGED_COMMENT_PATTERN = re.compile(r"^(?P<indent>\s*)(?P<comment><!--.*?-->)(?P<rest>\S.*)$")
NAV_GROUP_TRANSLATIONS = {
    "Documentation": "文档",
    "Overview": "概览",
    "Agents": "智能体",
    "Planner agents": "规划型智能体",
    "Prompts": "提示词",
    "Creating prompts": "创建提示词",
    "Running prompts": "运行提示词",
    "Strategies": "策略",
    "Tools": "工具",
    "Features": "功能",
    "Chat memory": "聊天记忆",
    "A2A Protocol": "A2A 协议",
    "Backend framework integrations": "后端框架集成",
    "Advanced usage": "高级用法",
    "Subgraphs": "子图",
    "Examples": "示例",
    "API reference": "API 参考",
}
NAV_LEAF_LABEL_TRANSLATIONS = {
    "Parallel node execution": "并行节点执行",
    "Data transfer between nodes": "节点间数据传递",
    "Agent Client Protocol": "智能体客户端协议",
    "Model capabilities": "模型能力",
    "Custom subgraphs": "自定义子图",
}
NAV_PATH_TITLE_OVERRIDES = {
    "parallel-node-execution.md": "并行节点执行",
    "data-transfer-between-nodes.md": "节点间数据传递",
    "agent-client-protocol.md": "智能体客户端协议",
    "model-capabilities.md": "模型能力",
    "custom-subgraphs.md": "自定义子图",
    "why-koog.md": "为什么选择 Koog",
}
PROTECTED_TERMS = [
    "Koog",
    "OpenAI",
    "Azure OpenAI Service",
    "Anthropic",
    "Google",
    "Gemini",
    "DeepSeek",
    "OpenRouter",
    "Amazon Bedrock",
    "Bedrock",
    "Mistral",
    "Ollama",
    "DashScope",
    "Qwen",
    "JetBrains",
    "Grazie Maven",
    "Kotlin",
    "Java",
    "Linux/macOS",
    "Windows",
    "LLM",
    "LLMClient",
    "JSON Schema",
    "Prompt Executor",
    "AgentMemory",
    "EventHandler",
]


@dataclass
class SiteDocument:
    path: Path
    meta: dict[str, str] | None
    body: str


@dataclass
class TabBlock:
    start: int
    end: int
    indent: int


def now_iso() -> str:
    return datetime.now(UTC).replace(microsecond=0).isoformat()


def run(cmd: list[str], cwd: Path | None = None) -> None:
    subprocess.run(cmd, cwd=cwd, check=True)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def copy_file(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def reset_dir(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def sha256_text(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def slugify_heading(text: str) -> str:
    text = text.strip().lower()
    text = re.sub(r"\{[^}]+\}\s*$", "", text).strip()
    text = re.sub(r"[^a-z0-9]+", "-", text)
    text = re.sub(r"-{2,}", "-", text).strip("-")
    return text


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def list_files(base: Path) -> list[Path]:
    return sorted(path for path in base.rglob("*") if path.is_file())


def strip_knit_comments(text: str) -> str:
    stripped = KNIT_COMMENT_RE.sub("", text)
    return re.sub(r"\n{3,}", "\n\n", stripped)


def load_site_doc(path: Path) -> SiteDocument:
    text = read_text(path)
    match = META_PATTERN.match(text)
    if not match:
        return SiteDocument(path=path, meta=None, body=strip_knit_comments(text))
    meta = json.loads(match.group("meta"))
    body = strip_knit_comments(text[match.end() :])
    return SiteDocument(path=path, meta=meta, body=body)


def dump_site_doc(meta: dict[str, str], body: str) -> str:
    meta_json = json.dumps(meta, ensure_ascii=False, sort_keys=True)
    return f"<!-- koog-zh-meta: {meta_json} -->\n{strip_knit_comments(body)}"


def find_repo_root(path: Path) -> Path:
    current = path.resolve()
    while current != current.parent:
        if (current / ".git").exists():
            return current
        current = current.parent
    raise SystemExit(f"找不到 git 仓库根目录: {path}")


def extract_top_level_section(text: str, key: str) -> str:
    lines = text.splitlines()
    start = None
    for index, line in enumerate(lines):
        if re.match(rf"^{re.escape(key)}:", line):
            start = index
            break
    if start is None:
        raise ValueError(f"在 mkdocs.yml 里找不到顶层键: {key}")

    end = len(lines)
    for index in range(start + 1, len(lines)):
        line = lines[index]
        if line and not line.startswith(" ") and re.match(r"^[A-Za-z_][A-Za-z0-9_-]*:", line):
            end = index
            break
    return "\n".join(lines[start:end]).rstrip()


def append_extra_css(section: str) -> str:
    if "translation-extra.css" in section:
        return section
    return f"{section}\n  - stylesheets/translation-extra.css"


def extract_markdown_title(text: str) -> str | None:
    match = HEADING_PATTERN.search(text)
    if not match:
        return None
    title = HEADING_ATTRS_SUFFIX.sub("", match.group("title")).strip()
    return title or None


def nav_leaf_title(relative_path: str, fallback_label: str, upstream_docs_dir: Path) -> str:
    if relative_path in NAV_PATH_TITLE_OVERRIDES:
        return NAV_PATH_TITLE_OVERRIDES[relative_path]

    site_path = SITE_DOCS_DIR / relative_path
    if site_path.exists():
        title = extract_markdown_title(load_site_doc(site_path).body)
        if title:
            return title

    upstream_path = upstream_docs_dir / relative_path
    if upstream_path.exists():
        title = extract_markdown_title(read_text(upstream_path))
        if title:
            return title

    return NAV_LEAF_LABEL_TRANSLATIONS.get(fallback_label, fallback_label)


def localize_nav_section(section: str, upstream_docs_dir: Path) -> str:
    lines: list[str] = []
    markdown_leaf_pattern = re.compile(r"^(\s*-\s+)([^:]+):\s+(.+?\.md)\s*$")
    external_leaf_pattern = re.compile(r"^(\s*-\s+)([^:]+):\s+(https?://\S+)\s*$")
    group_pattern = re.compile(r"^(\s*-\s+)([^:]+):\s*$")

    for line in section.splitlines():
        leaf_match = markdown_leaf_pattern.match(line)
        if leaf_match:
            path = leaf_match.group(3).strip()
            label = nav_leaf_title(path, leaf_match.group(2).strip(), upstream_docs_dir)
            lines.append(f"{leaf_match.group(1)}{label}: {path}")
            continue

        external_match = external_leaf_pattern.match(line)
        if external_match:
            label = NAV_GROUP_TRANSLATIONS.get(external_match.group(2).strip(), external_match.group(2).strip())
            lines.append(f"{external_match.group(1)}{label}: {external_match.group(3)}")
            continue

        group_match = group_pattern.match(line)
        if group_match:
            label = NAV_GROUP_TRANSLATIONS.get(group_match.group(2).strip(), group_match.group(2).strip())
            lines.append(f"{group_match.group(1)}{label}:")
            continue

        lines.append(line)

    return "\n".join(lines)


def localize_markdown_extensions(section: str) -> str:
    return section.replace("title: On this page", "title: 本页目录")


def localize_theme_section(section: str) -> str:
    if re.search(r"^\s+language:\s*", section, flags=re.MULTILINE):
        return re.sub(r"^(\s+language:\s*).*$", r"\1zh", section, flags=re.MULTILINE)
    return section.replace("  name: material\n", "  name: material\n  language: zh\n", 1)


def parse_yaml_list(section: str) -> list[str]:
    values: list[str] = []
    for line in section.splitlines()[1:]:
        match = re.match(r"^\s*-\s+(.*)$", line)
        if match:
            values.append(match.group(1).strip())
    return values


def generate_site_mkdocs(upstream_mkdocs_path: Path) -> str:
    upstream = read_text(upstream_mkdocs_path)
    upstream_docs_dir = upstream_mkdocs_path.parent / "docs"
    nav = localize_nav_section(extract_top_level_section(upstream, "nav"), upstream_docs_dir)
    markdown_extensions = localize_markdown_extensions(extract_top_level_section(upstream, "markdown_extensions"))
    plugins = extract_top_level_section(upstream, "plugins")
    theme = localize_theme_section(extract_top_level_section(upstream, "theme"))
    extra_css = append_extra_css(extract_top_level_section(upstream, "extra_css"))
    extra_javascript = extract_top_level_section(upstream, "extra_javascript")
    hooks = extract_top_level_section(upstream, "hooks")
    exclude_docs = extract_top_level_section(upstream, "exclude_docs")
    validation = extract_top_level_section(upstream, "validation")

    sections = [
        "site_name: Koog 中文文档",
        "site_url: https://koog.example.com/",
        "docs_dir: docs",
        "site_dir: ../dist",
        nav,
        markdown_extensions,
        plugins,
        theme,
        extra_css,
        'repo_url: https://github.com/maokaigang/koog',
        'repo_name: "maokaigang/koog"',
        'edit_uri: edit/main/koog-docs-zh/site/docs/',
        dedent(
            """\
            copyright: >
              非官方中文翻译，基于 <a href="https://docs.koog.ai/" target="_blank" rel="noopener">JetBrains Koog 文档</a>
              构建，原文与版权归原作者所有，遵循 <a href="https://www.apache.org/licenses/LICENSE-2.0"
              target="_blank" rel="noopener">Apache 2.0</a>。

            extra:
              announcement: >-
                这是非官方中文翻译站点。正文为中文翻译，API 参考仍跳转到官方英文
                <a href="https://api.koog.ai/" target="_blank" rel="noopener">api.koog.ai</a>。
              original_docs_url: https://docs.koog.ai/
              original_repo_url: https://github.com/JetBrains/koog
              original_api_url: https://api.koog.ai/
              support_url: https://docs.koog.ai/koog-slack-channel/
              issue_tracker_url: https://youtrack.jetbrains.com/issues/KG/
            """
        ).rstrip(),
        extra_javascript,
        hooks,
        exclude_docs,
        validation,
    ]
    return "\n\n".join(section.rstrip() for section in sections if section).rstrip() + "\n"


def build_manifest(tag: str) -> dict[str, object]:
    files: list[dict[str, str]] = []
    docs_root = UPSTREAM_DIR / "docs"
    for path in list_files(docs_root):
        files.append(
            {
                "path": path.relative_to(docs_root).as_posix(),
                "sha256": file_sha256(path),
                "kind": "markdown" if path.suffix == ".md" else "asset",
            }
        )
    return {
        "tag": tag,
        "synced_at": now_iso(),
        "source_repo": UPSTREAM_REPO_URL,
        "files": files,
    }


def load_manifest() -> dict[str, object]:
    manifest_path = UPSTREAM_DIR / "manifest.json"
    if not manifest_path.exists():
        raise SystemExit("缺少 upstream/manifest.json，请先执行 ./bin/sync-upstream <tag>")
    return json.loads(read_text(manifest_path))


def manifest_markdown_map(manifest: dict[str, object]) -> dict[str, str]:
    result: dict[str, str] = {}
    for item in manifest["files"]:
        entry = dict(item)
        if entry["kind"] == "markdown":
            result[entry["path"]] = entry["sha256"]
    return result


def site_mirrored_markdown_files() -> list[Path]:
    files: list[Path] = []
    for path in list_files(SITE_DOCS_DIR):
        if path.suffix != ".md":
            continue
        doc = load_site_doc(path)
        if doc.meta:
            files.append(path)
    return sorted(files)


def copy_tree_files(src: Path, dst: Path, *, skip_names: set[str] | None = None) -> None:
    skip_names = skip_names or set()
    for path in list_files(src):
        if path.name in skip_names:
            continue
        copy_file(path, dst / path.relative_to(src))


def sync_upstream(args: argparse.Namespace) -> None:
    source_repo = Path(args.source_repo).resolve() if args.source_repo else None
    source_url = str(source_repo) if source_repo else args.repo_url
    hook_entries: list[str] = []

    with tempfile.TemporaryDirectory(prefix="koog-docs-zh-") as tempdir:
        temp_path = Path(tempdir) / "koog-upstream"
        run(
            [
                "git",
                "clone",
                "--depth",
                "1",
                "--branch",
                args.tag,
                "--single-branch",
                source_url,
                str(temp_path),
            ]
        )

        docs_root = temp_path / UPSTREAM_DOCS_ROOT
        if not docs_root.exists():
            raise SystemExit(f"上游仓库缺少 {UPSTREAM_DOCS_ROOT}")

        reset_dir(UPSTREAM_DIR)
        copy_tree_files(docs_root, UPSTREAM_DIR / "docs")
        if (temp_path / "docs/overrides").exists():
            copy_tree_files(temp_path / "docs/overrides", UPSTREAM_DIR / "overrides")
        copy_file(temp_path / UPSTREAM_MKDOCS, UPSTREAM_DIR / "mkdocs.yml")
        copy_file(temp_path / UPSTREAM_PYPROJECT, UPSTREAM_DIR / "pyproject.toml")
        copy_file(temp_path / UPSTREAM_PYTHON_VERSION, UPSTREAM_DIR / ".python-version")
        copy_file(temp_path / UPSTREAM_UV_LOCK, UPSTREAM_DIR / "uv.lock")
        copy_file(temp_path / UPSTREAM_LICENSE, UPSTREAM_DIR / "LICENSE.txt")
        try:
            hook_entries = parse_yaml_list(extract_top_level_section(read_text(temp_path / UPSTREAM_MKDOCS), "hooks"))
        except ValueError:
            hook_entries = []
        for hook_entry in hook_entries:
            hook_source = temp_path / "docs" / hook_entry
            if hook_source.exists():
                copy_file(hook_source, UPSTREAM_DIR / hook_entry)

    manifest = build_manifest(args.tag)
    write_text(UPSTREAM_DIR / "manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

    reset_dir(SITE_DIR / "hooks")
    copy_file(UPSTREAM_DIR / ".python-version", SITE_DIR / ".python-version")
    copy_file(UPSTREAM_DIR / "uv.lock", SITE_DIR / "uv.lock")
    for hook_entry in hook_entries:
        upstream_hook = UPSTREAM_DIR / hook_entry
        if upstream_hook.exists():
            copy_file(upstream_hook, SITE_DIR / hook_entry)

    for path in list_files(UPSTREAM_DIR / "docs"):
        relative = path.relative_to(UPSTREAM_DIR / "docs")
        if relative.as_posix() in LOCAL_EXTRA_FILES:
            continue
        if path.suffix == ".md":
            continue
        if relative.name == "CNAME":
            continue
        copy_file(path, SITE_DOCS_DIR / relative)

    markdown_map = manifest_markdown_map(manifest)
    synced_at = now_iso()
    new_count = 0
    outdated_count = 0
    deleted_count = 0

    for relative, source_sha in markdown_map.items():
        upstream_path = UPSTREAM_DIR / "docs" / relative
        site_path = SITE_DOCS_DIR / relative
        upstream_body = strip_knit_comments(read_text(upstream_path))
        meta = {
            "source_path": relative,
            "source_sha256": source_sha,
            "source_tag": args.tag,
            "last_synced_at": synced_at,
            "translation_status": "new",
        }
        if not site_path.exists():
            write_text(site_path, dump_site_doc(meta, upstream_body))
            new_count += 1
            continue

        current = load_site_doc(site_path)
        if not current.meta:
            write_text(site_path, dump_site_doc(meta, current.body))
            new_count += 1
            continue

        current_meta = dict(current.meta)
        current_status = current_meta.get("translation_status", "new")
        previous_sha = current_meta.get("source_sha256")
        current_meta.update(
            {
                "source_path": relative,
                "source_sha256": source_sha,
                "source_tag": args.tag,
                "last_synced_at": synced_at,
            }
        )
        if previous_sha != source_sha:
            current_meta["translation_status"] = "outdated"
            outdated_count += 1
        else:
            current_meta["translation_status"] = current_status
        write_text(site_path, dump_site_doc(current_meta, current.body))

    for site_path in site_mirrored_markdown_files():
        doc = load_site_doc(site_path)
        assert doc.meta is not None
        source_path = doc.meta.get("source_path")
        if not source_path or source_path not in markdown_map:
            doc.meta["translation_status"] = "deleted"
            doc.meta["last_synced_at"] = synced_at
            write_text(site_path, dump_site_doc(doc.meta, doc.body))
            deleted_count += 1

    write_text(SITE_DIR / "mkdocs.yml", generate_site_mkdocs(UPSTREAM_DIR / "mkdocs.yml"))

    print(f"Synced upstream tag {args.tag}")
    print(f"  new pages: {new_count}")
    print(f"  outdated pages: {outdated_count}")
    print(f"  deleted pages: {deleted_count}")


def extract_nav_block(path: Path) -> str:
    return extract_top_level_section(read_text(path), "nav")


def build_status_report() -> dict[str, object]:
    manifest = load_manifest()
    markdown_map = manifest_markdown_map(manifest)
    report: dict[str, list[str]] = {status: [] for status in STATUS_ORDER}
    local_extra: list[str] = []

    for relative, source_sha in markdown_map.items():
        site_path = SITE_DOCS_DIR / relative
        if not site_path.exists():
            report["new"].append(relative)
            continue

        doc = load_site_doc(site_path)
        if not doc.meta:
            report["outdated"].append(relative)
            continue

        status = doc.meta.get("translation_status", "new")
        if status not in STATUS_ORDER:
            status = "outdated"
        if doc.meta.get("source_sha256") != source_sha:
            status = "outdated"
        report[status].append(relative)

    for path in list_files(SITE_DOCS_DIR):
        if path.suffix != ".md":
            continue
        doc = load_site_doc(path)
        relative = path.relative_to(SITE_DOCS_DIR).as_posix()
        if doc.meta:
            source_path = doc.meta.get("source_path")
            if source_path not in markdown_map:
                report["deleted"].append(relative)
        else:
            local_extra.append(relative)

    nav_changed = extract_nav_block(UPSTREAM_DIR / "mkdocs.yml") != extract_nav_block(SITE_DIR / "mkdocs.yml")
    deduped = {key: sorted(set(values)) for key, values in report.items()}
    return {
        "tag": manifest["tag"],
        "counts": {key: len(values) for key, values in deduped.items()},
        "files": deduped,
        "local_extras": sorted(local_extra),
        "nav_changed": nav_changed,
    }


def diff_report(args: argparse.Namespace) -> None:
    report = build_status_report()
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return

    print(f"Upstream tag: {report['tag']}")
    print(f"Navigation changed: {'yes' if report['nav_changed'] else 'no'}")
    for status in STATUS_ORDER:
        files = report["files"][status]
        print(f"{status}: {len(files)}")
        for relative in files[: args.limit]:
            print(f"  - {relative}")
    if report["local_extras"]:
        print(f"local extras: {len(report['local_extras'])}")
        for relative in report["local_extras"][: args.limit]:
            print(f"  - {relative}")


def google_translate(text: str, target_lang: str = "zh-CN") -> str:
    if not re.search(r"[A-Za-z]", text):
        return text
    url = (
        "https://translate.googleapis.com/translate_a/single"
        f"?client=gtx&sl=en&tl={target_lang}&dt=t&q={quote(text)}"
    )
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urlopen(request, timeout=20) as response:
                data = json.loads(response.read().decode("utf-8"))
            return "".join(chunk[0] for chunk in data[0])
        except Exception as error:  # noqa: BLE001
            last_error = error
            if attempt < 2:
                time.sleep(1.5 * (attempt + 1))
    assert last_error is not None
    raise last_error


def deepseek_translate(text: str) -> str:
    if not re.search(r"[A-Za-z]", text):
        return text

    api_key = os.environ.get("DEEPSEEK_API_KEY") or os.environ.get("KOOG_DOCS_DEEPSEEK_API_KEY")
    if not api_key:
        raise RuntimeError("缺少 DEEPSEEK_API_KEY 或 KOOG_DOCS_DEEPSEEK_API_KEY 环境变量")

    base_url = os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    model = os.environ.get("DEEPSEEK_TRANSLATION_MODEL", "deepseek-chat")
    timeout = int(os.environ.get("DEEPSEEK_HTTP_TIMEOUT", "120"))
    max_attempts = int(os.environ.get("DEEPSEEK_MAX_ATTEMPTS", "4"))
    url = f"{base_url}/chat/completions"
    payload = {
        "model": model,
        "temperature": 0.1,
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a technical translator. Translate the user's Markdown from English into Simplified Chinese. "
                    "Preserve Markdown structure exactly. Do not translate code, URLs, file paths, API names, env vars, "
                    "identifiers, snippet syntax, KNIT comments, or placeholders. Tokens like @@koogzh_*@@ must remain "
                    "byte-for-byte unchanged. Return only the translated Markdown fragment."
                ),
            },
            {
                "role": "user",
                "content": text,
            },
        ],
    }

    last_error: Exception | None = None
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    for attempt in range(max_attempts):
        try:
            request = Request(
                url,
                data=body,
                headers={
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {api_key}",
                },
                method="POST",
            )
            with urlopen(request, timeout=timeout) as response:
                data = json.loads(response.read().decode("utf-8"))
            return data["choices"][0]["message"]["content"]
        except Exception as error:  # noqa: BLE001
            last_error = error
            if attempt < max_attempts - 1:
                time.sleep(2 * (attempt + 1))
    assert last_error is not None
    raise last_error


def translate_text(text: str, provider: str) -> str:
    if provider == "copy":
        return text
    if provider == "google-web":
        return google_translate(text)
    if provider == "deepseek":
        return deepseek_translate(text)
    raise ValueError(f"不支持的翻译 provider: {provider}")


def translate_markdown_link(match: re.Match[str], provider: str) -> str:
    label = match.group(1)
    target = match.group(2)
    translated_label = translate_inline_text(label, provider)
    return f"[{translated_label}]({target})"


def translate_inline_text(text: str, provider: str) -> str:
    placeholders: list[str] = []
    link_placeholders: list[str] = []

    def protect_link(match: re.Match[str]) -> str:
        token = f"[[[koogzhlink{len(link_placeholders)}]]]"
        label = match.group(1)
        target = match.group(2)
        translated_label = translate_inline_text(label, provider)
        link_placeholders.append(f"[{translated_label}]({target})")
        return token

    def protect(pattern: str, current: str) -> str:
        regex = re.compile(pattern)

        def repl(match: re.Match[str]) -> str:
            token = f"[[[koogzh{len(placeholders)}]]]"
            placeholders.append(match.group(0))
            return token

        return regex.sub(repl, current)

    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", protect_link, text)
    text = protect(r"`[^`]+`", text)
    text = protect(r"https?://\S+", text)
    text = protect(r":[A-Za-z0-9_-]+:\{[^}]+\}", text)
    text = protect(r"\b[A-Z][A-Z0-9_]{2,}\b", text)
    text = protect(r"api:[^\s)]+", text)
    for term in sorted(PROTECTED_TERMS, key=len, reverse=True):
        text = protect(re.escape(term), text)
    translated = translate_text(text, provider)
    for index, original in enumerate(placeholders):
        translated = translated.replace(f"[[[koogzh{index}]]]", original)
    for index, original in enumerate(link_placeholders):
        translated = translated.replace(f"[[[koogzhlink{index}]]]", original)
    return translated


def is_table_separator(line: str) -> bool:
    cleaned = line.replace("|", "").replace(":", "").replace("-", "").strip()
    return cleaned == ""


def add_placeholder(placeholders: list[str], value: str, kind: str) -> str:
    token = f"@@koogzh_{kind.lower()}_{len(placeholders)}@@"
    placeholders.append(value)
    return token


def protect_markdown_targets(text: str, placeholders: list[str]) -> str:
    pattern = re.compile(r"(!?\[[^\]]+\]\()([^)]+)(\))")

    def repl(match: re.Match[str]) -> str:
        token = add_placeholder(placeholders, match.group(2), "LINK")
        return f"{match.group(1)}{token}{match.group(3)}"

    return pattern.sub(repl, text)


def protect_patterns(text: str, placeholders: list[str], patterns: list[str], kind: str, *, flags: int = 0) -> str:
    for pattern in patterns:
        regex = re.compile(pattern, flags)

        def repl(match: re.Match[str]) -> str:
            return add_placeholder(placeholders, match.group(0), kind)

        text = regex.sub(repl, text)
    return text


def protect_terms(text: str, placeholders: list[str]) -> str:
    for term in sorted(PROTECTED_TERMS, key=len, reverse=True):
        pattern = re.compile(re.escape(term))

        def repl(match: re.Match[str]) -> str:
            return add_placeholder(placeholders, match.group(0), "TERM")

        text = pattern.sub(repl, text)
    return text


def restore_placeholders(text: str, placeholders: list[str]) -> str:
    def repl(match: re.Match[str]) -> str:
        index = int(match.group("index"))
        if index >= len(placeholders):
            return match.group(0)
        return placeholders[index]

    return PLACEHOLDER_PATTERN.sub(repl, text)


def split_translation_chunks(text: str, max_chars: int = 3500) -> list[str]:
    if len(text) <= max_chars:
        return [text]

    parts = re.split(r"(\n\s*\n)", text)
    chunks: list[str] = []
    current = ""

    for part in parts:
        if len(current) + len(part) <= max_chars or not current:
            current += part
            continue
        chunks.append(current)
        current = part

    if current:
        chunks.append(current)

    return chunks


def preserve_heading_anchors(source_text: str, translated_text: str) -> str:
    source_lines = source_text.splitlines()
    translated_lines = translated_text.splitlines()
    source_headings: list[tuple[int, re.Match[str]]] = []
    translated_headings: list[tuple[int, re.Match[str]]] = []
    heading_pattern = re.compile(r"^(#{1,6}\s+)(.*?)(\s+\{[^}]+\})?\s*$")

    for index, line in enumerate(source_lines):
        match = heading_pattern.match(line)
        if match:
            source_headings.append((index, match))

    for index, line in enumerate(translated_lines):
        match = heading_pattern.match(line)
        if match:
            translated_headings.append((index, match))

    for (_, source_match), (line_index, translated_match) in zip(source_headings, translated_headings, strict=False):
        if source_match.group(1) != translated_match.group(1):
            continue
        original_heading = source_match.group(2).strip()
        if not re.search(r"[A-Za-z]", original_heading):
            continue
        if translated_match.group(3):
            continue
        slug = slugify_heading(original_heading)
        if not slug:
            continue
        translated_heading = translated_match.group(2).strip()
        translated_lines[line_index] = f"{translated_match.group(1)}{translated_heading} {{ #{slug} }}"

    result = "\n".join(translated_lines)
    if translated_text.endswith("\n"):
        result += "\n"
    return result


def leading_spaces(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def ensure_nested_tab_indent(line: str, base_indent: str) -> str:
    if not line.strip():
        return line
    if leading_spaces(line) > len(base_indent):
        return line
    return f"{base_indent}    {line}"


def normalize_merged_tab_blocks(text: str) -> str:
    lines = text.splitlines()
    output: list[str] = []
    index = 0

    while index < len(lines):
        match = MERGED_TAB_PATTERN.match(lines[index])
        if not match:
            output.append(lines[index])
            index += 1
            continue

        base_indent = match.group("indent")
        header_line = f"{base_indent}{match.group('header')}"
        rest = match.group("rest")

        output.append(header_line)
        output.append("")
        output.append(ensure_nested_tab_indent(rest, base_indent))
        index += 1

        if not (rest.startswith("<!--") or rest.startswith("```") or rest.startswith("--8<--")):
            continue

        in_comment = rest.startswith("<!--") and "-->" not in rest
        in_fence = rest.startswith("```")
        in_include = rest.startswith("--8<--")

        while index < len(lines):
            line = lines[index]
            stripped = line.strip()
            structural = stripped.startswith("<!--") or stripped.startswith("```") or stripped.startswith("--8<--")

            if not stripped:
                output.append(line)
                index += 1
                continue

            if not in_comment and not in_fence and not in_include:
                if leading_spaces(line) <= len(base_indent) and not structural:
                    break

            output.append(ensure_nested_tab_indent(line, base_indent))

            if stripped.startswith("<!--"):
                if "-->" not in stripped:
                    in_comment = True
            elif in_comment and "-->" in stripped:
                in_comment = False

            if stripped.startswith("```"):
                in_fence = not in_fence

            if stripped.startswith("--8<--"):
                in_include = not in_include

            index += 1

    result = "\n".join(output)
    if text.endswith("\n"):
        result += "\n"
    return result


def normalize_comment_tails(text: str) -> str:
    lines = text.splitlines()
    output: list[str] = []

    for line in lines:
        match = MERGED_COMMENT_PATTERN.match(line)
        if not match:
            output.append(line)
            continue

        indent_width = len(match.group("indent"))
        comment_line = f"{match.group('indent')}{match.group('comment')}"
        rest = match.group("rest")

        if rest.startswith("==="):
            rest_indent = " " * max(indent_width - 4, 0)
        else:
            rest_indent = " " * indent_width

        output.append(comment_line)
        output.append("")
        output.append(f"{rest_indent}{rest}")

    result = "\n".join(output)
    if text.endswith("\n"):
        result += "\n"
    return result


def normalize_stray_fence_lines(text: str) -> str:
    lines = text.splitlines()
    output: list[str] = []
    in_fence = False

    for line in lines:
        if re.match(r"^\s*```", line):
            if in_fence:
                stray_match = re.match(r"^(?P<indent>\s*)```(?:\s+)(?P<rest>\S.*)$", line)
                if stray_match:
                    output.append(f"{stray_match.group('indent')}{stray_match.group('rest')}")
                    continue
            in_fence = not in_fence
            output.append(line)
            continue
        output.append(line)

    result = "\n".join(output)
    if text.endswith("\n"):
        result += "\n"
    return result


def normalize_post_translation_structure(text: str) -> str:
    normalized = normalize_merged_tab_blocks(text)
    normalized = normalize_comment_tails(normalized)
    normalized = normalize_stray_fence_lines(normalized)
    return normalized


def parse_tab_blocks(lines: list[str]) -> list[TabBlock]:
    header_pattern = re.compile(r'^(\s*)===\s+"[^"]+"\s*$')
    stack: list[TabBlock] = []
    blocks: list[TabBlock] = []

    for index, line in enumerate(lines):
        match = header_pattern.match(line)
        if not match:
            continue
        indent = len(match.group(1))
        while stack and indent <= stack[-1].indent:
            block = stack.pop()
            block.end = index
            blocks.append(block)
        stack.append(TabBlock(start=index, end=len(lines), indent=indent))

    while stack:
        blocks.append(stack.pop())

    return sorted(blocks, key=lambda block: block.start)


def is_structural_tab_body(lines: list[str]) -> bool:
    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue
        return stripped.startswith("<!--") or stripped.startswith("```") or stripped.startswith("--8<--")
    return False


def restore_structural_tab_blocks(source_text: str, translated_text: str) -> str:
    source_lines = source_text.splitlines()
    translated_lines = translated_text.splitlines()
    source_blocks = parse_tab_blocks(source_lines)
    translated_blocks = parse_tab_blocks(translated_lines)

    if len(source_blocks) != len(translated_blocks):
        return translated_text

    updated = translated_lines[:]
    for source_block, translated_block in reversed(list(zip(source_blocks, translated_blocks, strict=False))):
        source_body = source_lines[source_block.start + 1 : source_block.end]
        if not is_structural_tab_body(source_body):
            continue
        updated[translated_block.start + 1 : translated_block.end] = source_body

    result = "\n".join(updated)
    if translated_text.endswith("\n"):
        result += "\n"
    return result


def translate_markdown_with_deepseek(text: str) -> str:
    placeholders: list[str] = []
    protected = text
    protected = protect_patterns(
        protected,
        placeholders,
        [
            r"(?ms)^```.*?^```[ \t]*$",
            r"(?s)<!--.*?-->",
        ],
        "BLOCK",
        flags=re.MULTILINE | re.DOTALL,
    )
    protected = protect_markdown_targets(protected, placeholders)
    protected = protect_patterns(
        protected,
        placeholders,
        [
            r"`[^`\n]+`",
            r"https?://[^\s)\]>]+",
            r":[A-Za-z0-9_-]+:\{[^}]+\}",
            r"\b[A-Z][A-Z0-9_]{2,}\b",
            r"api:[^\s)]+",
            r"--8<--[^\n]*",
        ],
        "INLINE",
    )
    protected = protect_terms(protected, placeholders)

    chunk_size = int(os.environ.get("KOOG_DOCS_TRANSLATION_CHUNK_CHARS", "3500"))
    translated_parts: list[str] = []
    for chunk in split_translation_chunks(protected, max_chars=chunk_size):
        translated_parts.append(deepseek_translate(chunk))

    restored = restore_placeholders("".join(translated_parts), placeholders)
    normalized = normalize_post_translation_structure(restored)
    structural = restore_structural_tab_blocks(text, normalized)
    residual = translate_residual_english_prose(structural, "deepseek")
    return preserve_heading_anchors(text, residual)


def translate_structured_line(line: str, provider: str) -> str:
    if line.lstrip().startswith("# --8<--"):
        return line

    heading = re.match(r"^(#{1,6}\s+)(.*)$", line)
    if heading:
        original_text = heading.group(2).strip()
        translated_text = translate_inline_text(original_text, provider)
        if re.search(r"\{[^}]+\}\s*$", original_text):
            return f"{heading.group(1)}{translated_text}"
        original_slug = slugify_heading(original_text)
        if original_slug and re.search(r"[A-Za-z]", original_text):
            return f"{heading.group(1)}{translated_text} {{ #{original_slug} }}"
        return f"{heading.group(1)}{translated_text}"

    tabbed = re.match(r'^(\s*===\s+")([^"]+)(")\s*$', line)
    if tabbed:
        return f'{tabbed.group(1)}{translate_inline_text(tabbed.group(2), provider)}{tabbed.group(3)}'

    admonition = re.match(r'^(\s*(?:!!!|\?\?\?)\s+\w+\s+")([^"]+)(")\s*$', line)
    if admonition:
        return f'{admonition.group(1)}{translate_inline_text(admonition.group(2), provider)}{admonition.group(3)}'

    bare_admonition = re.match(r'^(\s*(?:!!!|\?\?\?)\s+\w+)\s*$', line)
    if bare_admonition:
        return bare_admonition.group(1)

    bullet = re.match(r"^(\s*[-*+]\s+)(.*)$", line)
    if bullet:
        return f"{bullet.group(1)}{translate_inline_text(bullet.group(2), provider)}"

    ordered = re.match(r"^(\s*\d+\.\s+)(.*)$", line)
    if ordered:
        return f"{ordered.group(1)}{translate_inline_text(ordered.group(2), provider)}"

    quote_line = re.match(r"^(\s*>\s+)(.*)$", line)
    if quote_line:
        return f"{quote_line.group(1)}{translate_inline_text(quote_line.group(2), provider)}"

    return translate_inline_text(line, provider)


def translate_table_row(line: str, provider: str) -> str:
    prefix = ""
    suffix = ""
    working = line
    if working.startswith("|"):
        prefix = "|"
        working = working[1:]
    if working.endswith("|"):
        suffix = "|"
        working = working[:-1]
    cells = working.split("|")
    translated_cells = [translate_inline_text(cell.strip(), provider) for cell in cells]
    return prefix + " " + " | ".join(translated_cells) + " " + suffix


def flush_paragraph(paragraph: list[str], output: list[str], provider: str) -> None:
    if not paragraph:
        return
    indent = re.match(r"^(\s*)", paragraph[0]).group(1)
    block = " ".join(line.strip() for line in paragraph)
    translated = translate_inline_text(block, provider)
    output.append(f"{indent}{translated}")
    paragraph.clear()


def should_translate_residual_text(text: str, *, min_words: int = 4) -> bool:
    if not re.search(r"[A-Za-z]", text):
        return False

    cleaned = re.sub(r"`[^`]+`", " ", text)
    cleaned = re.sub(r"!\[[^\]]*\]\([^)]*\)", " ", cleaned)
    cleaned = re.sub(r"\[[^\]]+\]\([^)]*\)", " ", cleaned)
    cleaned = re.sub(r"https?://\S+", " ", cleaned)
    cleaned = re.sub(r"\{\s*#[^}]+\}", " ", cleaned)
    cleaned = re.sub(r"\b[a-z_][\w.<>:/-]*\([^)]*\)", " ", cleaned)
    cleaned = re.sub(r"\b[a-z_][\w.<>:/-]*\b", lambda match: " " if "." in match.group(0) else match.group(0), cleaned)
    cleaned = cleaned.strip()

    if not cleaned:
        return False

    words = re.findall(r"[A-Za-z][A-Za-z'-]+", cleaned)
    if len(words) < min_words:
        return False

    english_chars = len(re.findall(r"[A-Za-z]", cleaned))
    cjk_chars = len(re.findall(r"[\u4e00-\u9fff]", cleaned))
    return english_chars > cjk_chars


def is_probably_indented_code_line(line: str) -> bool:
    if not (line.startswith("    ") or line.startswith("\t")):
        return False

    stripped = line.strip()
    if not stripped:
        return False

    if stripped.startswith(("<!--", "--8<--", "```")):
        return False

    if re.match(r"^(?:[A-Za-z_][\w<>.,? ]*\([^)]*\)|[A-Za-z_][\w<>.,? ]+)\s*\{?$", stripped):
        return True

    code_patterns = [
        r"^(?:import|package|val|var|fun|class|interface|object|data class)\b",
        r"^(?:public|private|protected|internal|override|suspend|static|final|abstract)\b",
        r"^(?:if|else|for|while|when|switch|case|return|throw|try|catch)\b",
        r"^(?://|/\*|\*|@|#include\b)",
        r"(?:;|\{|\}|=>)$",
        r"^[A-Za-z_][\w.<>]*\s*=",
        r"^[A-Za-z_][\w.<>]*\(",
    ]
    return any(re.search(pattern, stripped) for pattern in code_patterns)


def flush_residual_paragraph(paragraph: list[str], output: list[str], provider: str) -> None:
    if not paragraph:
        return
    indent = re.match(r"^(\s*)", paragraph[0]).group(1)
    block = " ".join(line.strip() for line in paragraph)
    if should_translate_residual_text(block):
        output.append(f"{indent}{translate_inline_text(block, provider)}")
    else:
        output.extend(paragraph)
    paragraph.clear()


def translate_residual_english_prose(text: str, provider: str) -> str:
    lines = text.splitlines()
    output: list[str] = []
    paragraph: list[str] = []
    in_fence = False
    in_comment = False

    for line in lines:
        stripped = line.strip()

        if in_comment:
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            if "-->" in line:
                in_comment = False
            continue

        if stripped.startswith("<!--"):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            if "-->" not in line:
                in_comment = True
            continue

        if re.match(r"^\s*```", line):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            in_fence = not in_fence
            continue

        if in_fence:
            output.append(line)
            continue

        if is_probably_indented_code_line(line):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if not stripped:
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("--8<--") or stripped == "---":
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("<") and stripped.endswith(">"):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("![") or re.fullmatch(r"https?://\S+", stripped):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("# --8<--"):
            flush_residual_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if "|" in line and line.count("|") >= 2:
            flush_residual_paragraph(paragraph, output, provider)
            if is_table_separator(stripped) or not should_translate_residual_text(line):
                output.append(line)
            else:
                output.append(translate_table_row(line, provider))
            continue

        if re.match(r"^(#{1,6}\s+|[*+-]\s+|\d+\.\s+|>\s+)", stripped):
            flush_residual_paragraph(paragraph, output, provider)
            if should_translate_residual_text(stripped, min_words=2):
                output.append(translate_structured_line(line, provider))
            else:
                output.append(line)
            continue

        if re.match(r'^\s*(?:!!!|\?\?\?|===)\s+', line):
            flush_residual_paragraph(paragraph, output, provider)
            if should_translate_residual_text(stripped, min_words=2):
                output.append(translate_structured_line(line, provider))
            else:
                output.append(line)
            continue

        paragraph.append(line)

    flush_residual_paragraph(paragraph, output, provider)
    return "\n".join(output) + ("\n" if text.endswith("\n") else "")


def translate_markdown(text: str, provider: str) -> str:
    if provider == "deepseek":
        return translate_markdown_with_deepseek(text)

    lines = text.splitlines()
    output: list[str] = []
    paragraph: list[str] = []
    in_fence = False
    in_comment = False

    for line in lines:
        stripped = line.strip()

        if in_comment:
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            if "-->" in line:
                in_comment = False
            continue

        if stripped.startswith("<!--"):
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            if "-->" not in line:
                in_comment = True
            continue

        if re.match(r"^\s*```", line):
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            in_fence = not in_fence
            continue

        if in_fence:
            output.append(line)
            continue

        if not stripped:
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("--8<--") or stripped == "---":
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if stripped.startswith("<") and stripped.endswith(">"):
            flush_paragraph(paragraph, output, provider)
            output.append(line)
            continue

        if "|" in line and line.count("|") >= 2:
            flush_paragraph(paragraph, output, provider)
            if is_table_separator(stripped):
                output.append(line)
            else:
                output.append(translate_table_row(line, provider))
            continue

        if re.match(r"^(#{1,6}\s+|[*+-]\s+|\d+\.\s+|>\s+)", stripped):
            flush_paragraph(paragraph, output, provider)
            output.append(translate_structured_line(line, provider))
            continue

        if re.match(r'^\s*(?:!!!|\?\?\?|===)\s+', line):
            flush_paragraph(paragraph, output, provider)
            output.append(translate_structured_line(line, provider))
            continue

        paragraph.append(line)

    flush_paragraph(paragraph, output, provider)
    translated = "\n".join(output) + ("\n" if text.endswith("\n") else "")
    normalized = normalize_post_translation_structure(translated)
    return restore_structural_tab_blocks(text, normalized)


def candidate_paths(statuses: set[str], explicit_paths: list[str] | None) -> list[Path]:
    if explicit_paths:
        paths = []
        for entry in explicit_paths:
            path = Path(entry)
            if not path.is_absolute():
                path = (ROOT / entry).resolve()
            paths.append(path)
        return paths

    paths: list[Path] = []
    for path in site_mirrored_markdown_files():
        meta = load_site_doc(path).meta or {}
        if meta.get("translation_status") in statuses:
            paths.append(path)
    return sorted(paths)


def shard_paths(paths: list[Path], shard_index: int | None, shard_count: int | None) -> list[Path]:
    if shard_index is None and shard_count is None:
        return paths
    if shard_index is None or shard_count is None:
        raise SystemExit("--shard-index 和 --shard-count 必须同时提供")
    if shard_count <= 0:
        raise SystemExit("--shard-count 必须大于 0")
    if shard_index < 0 or shard_index >= shard_count:
        raise SystemExit("--shard-index 必须在 [0, --shard-count) 范围内")
    return paths[shard_index::shard_count]


def translate_changed(args: argparse.Namespace) -> None:
    manifest = load_manifest()
    markdown_map = manifest_markdown_map(manifest)
    statuses = {item.strip() for item in args.statuses.split(",") if item.strip()}
    paths = candidate_paths(statuses, args.paths)
    paths = shard_paths(paths, args.shard_index, args.shard_count)
    if args.limit:
        paths = paths[: args.limit]

    translated = 0
    failed = 0
    for path in paths:
        doc = load_site_doc(path)
        if not doc.meta:
            continue
        source_path = doc.meta["source_path"]
        upstream_path = UPSTREAM_DIR / "docs" / source_path
        if not upstream_path.exists():
            continue
        print(f"translating: {path.relative_to(ROOT)}", flush=True)
        try:
            body = translate_markdown(read_text(upstream_path), args.provider)
        except Exception as error:  # noqa: BLE001
            failed += 1
            print(f"failed: {path.relative_to(ROOT)} -> {error}", file=sys.stderr, flush=True)
            continue
        doc.meta["source_sha256"] = markdown_map[source_path]
        doc.meta["source_tag"] = str(manifest["tag"])
        doc.meta["last_synced_at"] = now_iso()
        if args.provider != "copy":
            doc.meta["translation_status"] = "changed"
        write_text(path, dump_site_doc(doc.meta, body))
        translated += 1
        print(f"translated: {path.relative_to(ROOT)}", flush=True)

    write_text(SITE_DIR / "mkdocs.yml", generate_site_mkdocs(UPSTREAM_DIR / "mkdocs.yml"))
    print(f"translated files: {translated}", flush=True)
    print(f"failed files: {failed}", flush=True)


def set_status(args: argparse.Namespace) -> None:
    path = Path(args.path)
    if not path.is_absolute():
        path = (ROOT / args.path).resolve()
    doc = load_site_doc(path)
    if not doc.meta:
        raise SystemExit(f"文件缺少 koog-zh metadata: {path}")
    doc.meta["translation_status"] = args.status
    doc.meta["last_synced_at"] = now_iso()
    write_text(path, dump_site_doc(doc.meta, doc.body))
    print(f"updated: {path.relative_to(ROOT)} -> {args.status}")


def build_site(_args: argparse.Namespace) -> None:
    if shutil.which("uv") is None:
        raise SystemExit("找不到 uv，请先安装 uv: https://docs.astral.sh/uv/")
    DIST_DIR.mkdir(parents=True, exist_ok=True)
    run(["uv", "sync", "--frozen", "--all-extras"], cwd=SITE_DIR)
    run(["uv", "run", "mkdocs", "build"], cwd=SITE_DIR)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Maintainable Koog Chinese docs site toolkit.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    sync_parser = subparsers.add_parser("sync-upstream")
    sync_parser.add_argument("tag", help="Koog release tag, for example 0.7.3")
    sync_parser.add_argument("--source-repo", help="Local Koog repository path for cloning from disk")
    sync_parser.add_argument("--repo-url", default=UPSTREAM_REPO_URL, help="Upstream repository URL")
    sync_parser.set_defaults(func=sync_upstream)

    diff_parser = subparsers.add_parser("diff-report")
    diff_parser.add_argument("--json", action="store_true", help="Print JSON output")
    diff_parser.add_argument("--limit", type=int, default=20, help="Max files shown per status in text mode")
    diff_parser.set_defaults(func=diff_report)

    translate_parser = subparsers.add_parser("translate-changed")
    translate_parser.add_argument(
        "--provider",
        choices=["google-web", "deepseek", "copy"],
        default="google-web",
        help="Machine translation provider",
    )
    translate_parser.add_argument(
        "--statuses",
        default="new,outdated",
        help="Comma-separated statuses to translate",
    )
    translate_parser.add_argument("--limit", type=int, help="Max files to translate")
    translate_parser.add_argument("--shard-index", type=int, help="Zero-based shard index")
    translate_parser.add_argument("--shard-count", type=int, help="Total shard count")
    translate_parser.add_argument("paths", nargs="*", help="Optional explicit site markdown paths")
    translate_parser.set_defaults(func=translate_changed)

    build_parser = subparsers.add_parser("build-site")
    build_parser.set_defaults(func=build_site)

    status_parser = subparsers.add_parser("set-status")
    status_parser.add_argument("path", help="Site markdown path")
    status_parser.add_argument("status", choices=STATUS_ORDER, help="New translation status")
    status_parser.set_defaults(func=set_status)

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
