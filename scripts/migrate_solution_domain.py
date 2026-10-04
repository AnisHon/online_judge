#!/usr/bin/env python3
"""Explicit, resumable MySQL maintenance migration for the solution domain."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import struct
import subprocess
import sys
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import NoReturn


ROOT = Path(__file__).resolve().parent.parent
MIGRATION_DIR = ROOT / "resources/sql/migration"
MANIFEST = MIGRATION_DIR / "manifest.tsv"
MIGRATION_KEY = "solution-domain-v1"
COMMUNITY_TYPES = (
    "SOLUTION_PUBLISHED", "SOLUTION_LIKED", "SOLUTION_COMMENTED", "COMMENT_REPLIED",
    "COMMENT_LIKED", "SOLUTION_MODERATED", "COMMENT_MODERATED",
)
OTHER_PROBLEM_TYPES = ("POINTS_AWARDED", "JUDGE_DISPATCH")
NULL_SENTINEL = "~NULL~"
SCHEMA_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]{0,63}$")
UUID_RE = re.compile(r"^[0-9a-fA-F-]{36}$")
INT_RE = re.compile(r"^-?[0-9]+$")


def fail(message: str) -> NoReturn:
    raise RuntimeError(message)


def read_manifest() -> tuple[list[dict[str, str]], str]:
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for line_no, line in enumerate(MANIFEST.read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 7:
            fail(f"迁移 manifest 第 {line_no} 行字段数量错误")
        row = dict(zip(("migration_id", "file", "sha256", "source_mode", "phase", "schema", "order"), fields))
        if row["migration_id"] in seen or Path(row["file"]).name != row["file"]:
            fail(f"迁移 manifest 第 {line_no} 行存在重复 ID 或不安全路径")
        if not re.fullmatch(r"[a-f0-9]{64}", row["sha256"]):
            fail(f"迁移 manifest 尚未固定 SHA-256：{row['file']}")
        seen.add(row["migration_id"])
        path = MIGRATION_DIR / row["file"]
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != row["sha256"]:
            fail(f"迁移文件缺失或 SHA-256 不匹配：{row['file']}")
        rows.append(row)
    if not rows:
        fail("迁移 manifest 为空")
    workflow_ids = {f"V20261004_{index}" for index in range(1, 6)}
    workflow_rows = [row for row in rows if row["migration_id"] in workflow_ids]
    if {row["migration_id"] for row in workflow_rows} != workflow_ids:
        fail("manifest 缺少完整的 V20261004_1.._5 solution-domain workflow")
    # Bind migration state to the entire manifest, including the standard
    # migration order and checksums, not only the solution workflow rows.
    return rows, hashlib.sha256(MANIFEST.read_bytes()).hexdigest()


def manifest_migration_path(migration_id: str, phase: str, source_mode: str, schema: str) -> Path:
    rows, _ = read_manifest()
    matches = [row for row in rows if row["migration_id"] == migration_id]
    if len(matches) != 1:
        fail(f"manifest 中迁移编号不唯一或缺失：{migration_id}")
    row = matches[0]
    if (row["phase"] != phase or row["source_mode"] not in ("ALL", source_mode)
            or row["schema"] != schema):
        fail(f"manifest 中 {migration_id} 的 phase/source_mode/schema 与调用不匹配")
    return MIGRATION_DIR / row["file"]


TABLES: tuple[dict, ...] = (
    {"name": "solution_explanation", "pk": ("solution_id",), "columns": (
        ("solution_id", "int"), ("title", "text"), ("problem_id", "int"), ("user_id", "int"),
        ("top_up", "bool"), ("private", "bool"), ("del_flag", "bool"), ("moderation_state", "text"),
        ("comments_open", "bool"), ("like_count", "int"), ("comment_count", "int"),
        ("first_published_at", "time"), ("version", "int"), ("create_time", "time"), ("update_time", "time"),
    )},
    {"name": "solution_explanation_content", "pk": ("solution_id",), "columns": (
        ("solution_id", "int"), ("content", "text"),
    )},
    {"name": "solution_like", "pk": ("solution_id", "user_id"), "columns": (
        ("solution_id", "int"), ("user_id", "int"), ("active", "bool"), ("first_notified", "bool"),
        ("created_at", "time"), ("updated_at", "time"),
    )},
    {"name": "solution_comment", "pk": ("comment_id",), "columns": (
        ("comment_id", "int"), ("solution_id", "int"), ("user_id", "int"), ("root_id", "int"),
        ("parent_id", "int"), ("reply_to_user_id", "int"), ("content", "text"), ("state", "text"),
        ("deleted_by", "int"), ("deleted_at", "time"), ("like_count", "int"), ("reply_count", "int"),
        ("client_request_id", "text"), ("created_at", "time"), ("updated_at", "time"),
    )},
    {"name": "comment_like", "pk": ("comment_id", "user_id"), "columns": (
        ("comment_id", "int"), ("user_id", "int"), ("active", "bool"), ("first_notified", "bool"),
        ("created_at", "time"), ("updated_at", "time"),
    )},
    {"name": "solution_moderation_action", "pk": ("action_id",), "columns": (
        ("action_id", "int"), ("solution_id", "int"), ("target_type", "text"), ("target_id", "int"),
        ("author_id", "int"), ("operator_id", "int"), ("action", "text"), ("reason", "text"),
        ("created_at", "time"),
    )},
    {"name": "community_outbox", "table": "problem_event_outbox", "target_table": "content_event_outbox",
     "pk": ("event_id",), "filter": "community", "columns": (
        ("event_id", "text"), ("dedupe_key", "text"), ("event_type", "text"), ("exchange_name", "text"),
        ("routing_key", "text"), ("payload", "json"), ("status", "text"), ("attempts", "int"),
        ("next_attempt_at", "time"), ("lease_owner", "text"), ("lease_until", "time"),
        ("last_error", "text"), ("created_at", "time"), ("sent_at", "time"),
    )},
)


class Mysql:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.password = os.environ.get("OJ_MIGRATION_MYSQL_PASSWORD") or os.environ.get("MYSQL_PWD")
        if not self.password:
            fail("需要通过 OJ_MIGRATION_MYSQL_PASSWORD 提供迁移账号密码；不会从仓库读取凭据")
        self.base = [
            "mysql", "--protocol=tcp", "--default-character-set=utf8mb4", "--batch", "--raw",
            "--skip-column-names", "--host", args.host, "--port", str(args.port), "--user", args.user,
        ]

    def env(self) -> dict[str, str]:
        env = os.environ.copy()
        env["MYSQL_PWD"] = self.password
        return env

    def query(self, sql: str) -> list[str]:
        result = subprocess.run(self.base + ["--execute", sql], env=self.env(), text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            fail("MySQL 查询失败：" + result.stderr.strip().splitlines()[-1])
        return result.stdout.splitlines()

    def execute(self, sql: str) -> None:
        result = subprocess.run(self.base + ["--execute", sql], env=self.env(), text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            fail("MySQL 操作失败：" + result.stderr.strip().splitlines()[-1])

    def execute_file(self, path: Path) -> None:
        text = path.read_text(encoding="utf-8")
        text = re.sub(r"\bdb_problem\b", self.args.problem_schema, text)
        text = re.sub(r"\bdb_content\b", self.args.content_schema, text)
        result = subprocess.run(self.base, env=self.env(), input=text, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            fail(f"执行 {path.name} 失败：" + result.stderr.strip().splitlines()[-1])

    def stream(self, sql: str):
        process = subprocess.Popen(self.base + ["--execute", sql], env=self.env(),
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        assert process.stdout is not None
        for raw in process.stdout:
            yield raw.rstrip(b"\n").decode("ascii")
        stderr = process.stderr.read().decode("utf-8", errors="replace") if process.stderr else ""
        if process.wait() != 0:
            fail("MySQL 流式读取失败：" + stderr.strip().splitlines()[-1])


def require_mysql8(db: Mysql) -> None:
    version = db.query("SELECT VERSION()")[0]
    if not re.match(r"^8\.", version):
        fail(f"题解迁移只支持 MySQL 8；当前数据库版本为 {version}")


def validate_schema_names(args: argparse.Namespace) -> None:
    for value in (args.problem_schema, args.content_schema, args.user_schema):
        if not SCHEMA_RE.fullmatch(value):
            fail("数据库 schema 名称不安全")
    if args.problem_schema == args.content_schema:
        fail("problem 与 content 必须是同一 MySQL 实例上的两个不同 schema")


def sql_name(name: str) -> str:
    if not re.fullmatch(r"[a-zA-Z0-9_]+", name):
        fail("内部 SQL 标识符不安全")
    return "`" + name + "`"


def sql_text(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def table_exists(db: Mysql, schema: str, table: str) -> bool:
    rows = db.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema="
                    + sql_text(schema) + " AND table_name=" + sql_text(table))
    return bool(rows and rows[0] == "1")


def has_schema(db: Mysql, schema: str) -> bool:
    return db.query("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=" + sql_text(schema))[0] == "1"


def state_row(db: Mysql, args: argparse.Namespace) -> list[str] | None:
    if not table_exists(db, args.content_schema, "content_solution_migration_state"):
        return None
    rows = db.query("SELECT source_mode,status,manifest_sha256,COALESCE(source_digest,''),COALESCE(target_digest,'') "
                    f"FROM {sql_name(args.content_schema)}.content_solution_migration_state "
                    "WHERE migration_key='solution-domain-v1'")
    return rows[0].split("\t") if rows else None


def table_spec(spec: dict) -> tuple[str, str, tuple[str, ...], tuple[tuple[str, str], ...]]:
    return (spec.get("table", spec["name"]), spec.get("target_table", spec.get("table", spec["name"])),
            spec["pk"], spec["columns"])


def outbox_filter(alias: str = "") -> str:
    prefix = alias + "." if alias else ""
    types = ",".join(sql_text(x) for x in COMMUNITY_TYPES)
    return f"{prefix}event_type IN ({types})"


def source_mode_detected(db: Mysql, args: argparse.Namespace) -> str:
    return "LEGACY" if table_exists(db, args.problem_schema, "solution_explanation") else "FRESH"


def validate_mode(db: Mysql, args: argparse.Namespace) -> None:
    actual = source_mode_detected(db, args)
    if actual != args.source_mode:
        fail(f"指定 source-mode={args.source_mode} 与实际源 schema={actual} 不一致；停止以避免误判")


def normalize_legacy_source(db: Mysql, args: argparse.Namespace) -> None:
    schema = args.problem_schema
    columns = set()
    if table_exists(db, schema, "solution_explanation"):
        columns = set(db.query("SELECT column_name FROM information_schema.columns WHERE table_schema="
                               + sql_text(schema) + " AND table_name='solution_explanation'"))
    required_columns = {"del_flag", "moderation_state", "comments_open", "like_count", "comment_count",
                        "first_published_at", "version"}
    required_tables = {"solution_like", "solution_comment", "comment_like", "solution_moderation_action",
                       "problem_event_outbox"}
    missing = required_columns - columns
    missing.update(table for table in required_tables if not table_exists(db, schema, table))
    if "first_published_at" in columns:
        marker = db.query("SELECT column_comment FROM information_schema.columns WHERE table_schema="
                          + sql_text(schema) + " AND table_name='solution_explanation' "
                          "AND column_name='first_published_at'")
        if marker and marker[0] == "T02_BACKFILL_PENDING":
            missing.add("first_published_at backfill marker")
        elif marker and marker[0] != "First public publication time":
            fail("first_published_at 的 T02 backfill marker 未知；不能假定历史回填已完成")
    if not missing:
        return
    if not args.confirm_source_backup:
        fail("旧源缺少 T02 schema 项（" + ",".join(sorted(missing))
             + "）；先完成并验证 source backup，再显式传 --confirm-source-backup 才允许运行冻结的 _2")
    if not table_exists(db, schema, "solution_explanation") or not table_exists(db, schema, "problem"):
        fail("旧源缺少 solution_explanation/problem，不能安全应用冻结的 T02 normalization")
    db.execute_file(manifest_migration_path(
        "V20261004_2", "source-normalize", "LEGACY", "db_problem"))
    columns = set(db.query("SELECT column_name FROM information_schema.columns WHERE table_schema="
                           + sql_text(schema) + " AND table_name='solution_explanation'"))
    still_missing = required_columns - columns
    still_missing.update(table for table in required_tables if not table_exists(db, schema, table))
    marker = db.query("SELECT column_comment FROM information_schema.columns WHERE table_schema="
                      + sql_text(schema) + " AND table_name='solution_explanation' "
                      "AND column_name='first_published_at'")
    if not marker or marker[0] != "First public publication time":
        still_missing.add("first_published_at finalized backfill marker")
    if still_missing:
        fail("历史 T02 normalization 执行后 schema 仍不完整：" + ",".join(sorted(still_missing)))


def validate_outbox_types(db: Mysql, schema: str, table: str, *, target: bool = False) -> None:
    if not table_exists(db, schema, table):
        return
    allowed = COMMUNITY_TYPES if target else COMMUNITY_TYPES + OTHER_PROBLEM_TYPES
    allowed_sql = ",".join(sql_text(x) for x in allowed)
    rows = db.query(f"SELECT event_type,COUNT(*) FROM {sql_name(schema)}.{sql_name(table)} "
                    f"WHERE event_type NOT IN ({allowed_sql}) GROUP BY event_type LIMIT 10")
    if rows:
        names = [row.split("\t", 1)[0] for row in rows]
        fail(f"{schema}.{table} 有未知/不属于此域的 outbox event_type：{','.join(names)}")


def source_table_exists(db: Mysql, args: argparse.Namespace, spec: dict) -> bool:
    source_table, _, _, _ = table_spec(spec)
    return table_exists(db, args.problem_schema, source_table)


def migration_state_prepare(db: Mysql, args: argparse.Namespace, manifest_sha: str) -> None:
    existing = state_row(db, args)
    if existing:
        mode, status, saved_sha, _, _ = existing
        if status == "CUTOVER":
            fail("solution-domain 已 CUTOVER；禁止重新 prepare/copy")
        if mode != args.source_mode or saved_sha != manifest_sha:
            fail("已有 migration state 的 source_mode/manifest SHA 与本次不一致")
        return
    for table in ("solution_explanation", "solution_explanation_content", "solution_like", "solution_comment",
                  "comment_like", "solution_moderation_action", "content_event_outbox"):
        if table_exists(db, args.content_schema, table):
            count = db.query(f"SELECT COUNT(*) FROM {sql_name(args.content_schema)}.{sql_name(table)}")[0]
            if count != "0":
                fail(f"目标表 {table} 已有数据但没有迁移 state；不猜测其来源，请先人工核对")
    db.execute("INSERT INTO " + sql_name(args.content_schema) + ".content_solution_migration_state "
               "(migration_key,source_mode,status,manifest_sha256,started_at) VALUES "
               "('solution-domain-v1'," + sql_text(args.source_mode) + ",'PREPARED'," + sql_text(manifest_sha)
               + ",NOW(3))")


def require_state(db: Mysql, args: argparse.Namespace, manifest_sha: str, allowed: set[str]) -> list[str]:
    row = state_row(db, args)
    if not row:
        fail("未找到 content migration state；先显式执行 --phase prepare")
    mode, status, saved_sha, _, _ = row
    if mode != args.source_mode or saved_sha != manifest_sha:
        fail("migration state 的 source_mode/manifest SHA 不匹配")
    if status not in allowed:
        fail(f"当前迁移状态 {status} 不允许此阶段")
    return row


def encoded_expr(alias: str, column: str, kind: str) -> str:
    ref = f"{alias}.{sql_name(column)}"
    if kind == "bool":
        value = f"IF({ref},'1','0')"
    elif kind == "time":
        value = f"CONCAT(DATE_FORMAT({ref},'%Y-%m-%d %H:%i:%s'),'.',LEFT(DATE_FORMAT({ref},'%f'),3))"
    elif kind == "json":
        value = f"CAST({ref} AS CHAR CHARACTER SET utf8mb4)"
    elif kind in ("int", "decimal"):
        value = f"CAST({ref} AS CHAR CHARACTER SET ascii)"
    else:
        value = f"CAST({ref} AS BINARY)"
    return f"CASE WHEN {ref} IS NULL THEN '{NULL_SENTINEL}' ELSE REPLACE(TO_BASE64({value}),CHAR(10),'') END"


def decode_row(line: str, expected: int) -> tuple[bytes | None, ...]:
    parts = line.split("\t")
    if len(parts) != expected:
        fail("MySQL 流式结果列数不符；停止以防摘要不完整")
    result: list[bytes | None] = []
    for cell in parts:
        if cell == NULL_SENTINEL:
            result.append(None)
        else:
            try:
                result.append(base64.b64decode(cell, validate=True))
            except Exception as exc:
                fail("MySQL base64 流损坏：" + str(exc))
    return tuple(result)


def canonical_json(raw: bytes) -> bytes:
    try:
        value = json.loads(raw.decode("utf-8"), parse_float=Decimal, parse_int=int)
    except (UnicodeDecodeError, json.JSONDecodeError, InvalidOperation) as exc:
        fail("outbox JSON 无法安全规范化：" + str(exc))

    def emit(item) -> str:
        if item is None:
            return "null"
        if item is True:
            return "true"
        if item is False:
            return "false"
        if isinstance(item, int):
            return str(item)
        if isinstance(item, Decimal):
            if not item.is_finite():
                fail("outbox JSON 含非有限数字")
            rendered = format(item, "f")
            if "." in rendered:
                rendered = rendered.rstrip("0").rstrip(".")
            return rendered or "0"
        if isinstance(item, str):
            return json.dumps(item, ensure_ascii=False, separators=(",", ":"))
        if isinstance(item, list):
            return "[" + ",".join(emit(v) for v in item) + "]"
        if isinstance(item, dict):
            ordered_keys = sorted(item, key=lambda key: str(key).encode("utf-16-be"))
            return "{" + ",".join(emit(str(k)) + ":" + emit(item[k]) for k in ordered_keys) + "}"
        fail("outbox JSON 类型不受支持")

    return emit(value).encode("utf-8")


def canonical_values(spec: dict, row: tuple[bytes | None, ...]) -> tuple[bytes | None, ...]:
    result = []
    for (_, kind), value in zip(spec["columns"], row):
        result.append(canonical_json(value) if value is not None and kind == "json" else value)
    return tuple(result)


def table_filter(spec: dict, alias: str) -> str:
    if spec.get("filter") == "community":
        return " WHERE " + outbox_filter(alias)
    return ""


def stream_digest(db: Mysql, schema: str, spec: dict, exists: bool, *, target: bool = False) -> tuple[str, int]:
    if not exists:
        return hashlib.sha256(b"").hexdigest(), 0
    source_table, target_table, pk, columns = table_spec(spec)
    table = target_table if target else source_table
    selected = ",".join(encoded_expr("s", column, kind) for column, kind in columns)
    order = ",".join("s." + sql_name(name) for name in pk)
    sql = f"SELECT {selected} FROM {sql_name(schema)}.{sql_name(table)} s{table_filter(spec, 's')} ORDER BY {order}"
    digest = hashlib.sha256()
    count = 0
    for line in db.stream(sql):
        values = canonical_values(spec, decode_row(line, len(columns)))
        for value in values:
            if value is None:
                digest.update(struct.pack(">i", -1))
            else:
                digest.update(struct.pack(">i", len(value)))
                digest.update(value)
        count += 1
    return digest.hexdigest(), count


def aggregate_digest(details: dict[str, tuple[str, int]]) -> str:
    digest = hashlib.sha256()
    for name in sorted(details):
        table_digest, count = details[name]
        encoded_name = name.encode("ascii")
        digest.update(struct.pack(">i", len(encoded_name)))
        digest.update(encoded_name)
        digest.update(struct.pack(">q", count))
        digest.update(bytes.fromhex(table_digest))
    return digest.hexdigest()


def count_table(db: Mysql, schema: str, spec: dict, exists: bool) -> int:
    if not exists:
        return 0
    table, _, _, _ = table_spec(spec)
    return int(db.query(f"SELECT COUNT(*) FROM {sql_name(schema)}.{sql_name(table)}"
                        + table_filter(spec, ""))[0])


def table_row_key(raw: str) -> str | int:
    if INT_RE.fullmatch(raw):
        return int(raw)
    if not UUID_RE.fullmatch(raw):
        fail("数据库返回了不符合预期的主键格式")
    return raw


def get_key_batch(db: Mysql, args: argparse.Namespace, spec: dict, last: tuple | None) -> list[tuple]:
    if not source_table_exists(db, args, spec):
        return []
    table, _, pk, _ = table_spec(spec)
    where = table_filter(spec, "s").strip()
    if last is not None:
        if where:
            where += " AND "
        else:
            where = "WHERE "
        if len(pk) == 1:
            value = sql_text(last[0]) if isinstance(last[0], str) else str(last[0])
            where += f"s.{sql_name(pk[0])}>{value}"
        else:
            where += f"(s.{sql_name(pk[0])},s.{sql_name(pk[1])})>({last[0]},{last[1]})"
    order = ",".join("s." + sql_name(key) for key in pk)
    raw = db.query(f"SELECT {','.join('s.' + sql_name(k) for k in pk)} FROM "
                   f"{sql_name(args.problem_schema)}.{sql_name(table)} s {where} ORDER BY {order} LIMIT 500")
    batch = []
    for line in raw:
        parts = line.split("\t")
        batch.append(tuple(table_row_key(part) for part in parts))
    return batch


def keys_where(pk: tuple[str, ...], keys: list[tuple]) -> str:
    if len(pk) == 1:
        values = ",".join(sql_text(k[0]) if isinstance(k[0], str) else str(k[0]) for k in keys)
        return f"{sql_name(pk[0])} IN ({values})"
    tuples = ",".join("(" + ",".join(str(value) for value in key) + ")" for key in keys)
    return f"({sql_name(pk[0])},{sql_name(pk[1])}) IN ({tuples})"


def keyed_rows(db: Mysql, schema: str, spec: dict, keys: list[tuple]) -> dict[tuple, tuple[bytes | None, ...]]:
    source_table, target_table, pk, columns = table_spec(spec)
    table = source_table if schema == db.args.problem_schema else target_table
    if not keys or not table_exists(db, schema, table):
        return {}
    encoded = ",".join(encoded_expr("r", column, kind) for column, kind in columns)
    output = ",".join("CAST(r." + sql_name(key) + " AS CHAR CHARACTER SET ascii)" for key in pk)
    raw = db.query(f"SELECT {output},{encoded} FROM {sql_name(schema)}.{sql_name(table)} r "
                   f"WHERE {keys_where(pk, keys)}")
    rows = {}
    for line in raw:
        parts = line.split("\t")
        key = tuple(table_row_key(part) for part in parts[:len(pk)])
        values = canonical_values(spec, decode_row("\t".join(parts[len(pk):]), len(columns)))
        rows[key] = values
    return rows


def copy_call(db: Mysql, spec: dict, last: tuple | None, upper: tuple) -> None:
    entity = spec["name"]
    if len(spec["pk"]) == 1 and spec["pk"][0] == "event_id":
        args = ["NULL", "NULL", "NULL", "NULL", "NULL" if last is None else sql_text(str(last[0])), sql_text(str(upper[0]))]
    elif len(spec["pk"]) == 1:
        args = ["NULL" if last is None else str(last[0]), "NULL", str(upper[0]), "NULL", "NULL", "NULL"]
    else:
        args = ["NULL" if last is None else str(last[0]), "NULL" if last is None else str(last[1]),
                str(upper[0]), str(upper[1]), "NULL", "NULL"]
    db.execute("CALL " + sql_name(db.args.content_schema) + ".content_solution_copy_batch("
               + ",".join([sql_text(entity)] + args) + ")")


def require_maintenance(args: argparse.Namespace) -> None:
    missing = []
    if not args.confirm_maintenance:
        missing.append("--confirm-maintenance")
    if not args.confirm_source_writes_stopped:
        missing.append("--confirm-source-writes-stopped")
    if not args.confirm_relays_stopped:
        missing.append("--confirm-relays-stopped")
    if missing:
        fail("此阶段必须确认维护窗口、旧题解写入已停止、相关 outbox producer/relay 已停止；缺少 "
             + ",".join(missing))


def seed_problem_references(db: Mysql, args: argparse.Namespace) -> None:
    source = sql_name(args.problem_schema)
    target = sql_name(args.content_schema)
    db.execute(f"INSERT INTO {target}.solution_problem_reference "
               "(problem_id,exists_flag,del_flag,auth,title,checked_at,requested_at) "
               f"SELECT DISTINCT s.problem_id,(p.problem_id IS NOT NULL),COALESCE(p.del_flag,1),p.auth,"
               "CASE WHEN p.problem_id IS NOT NULL AND p.del_flag=0 AND p.auth=1 THEN p.title ELSE NULL END,NOW(3),NOW(3) "
               f"FROM {target}.solution_explanation s LEFT JOIN {source}.problem p ON p.problem_id=s.problem_id "
               f"LEFT JOIN {target}.solution_problem_reference r ON r.problem_id=s.problem_id "
               "WHERE r.problem_id IS NULL")


def prepare(db: Mysql, args: argparse.Namespace, manifest_sha: str) -> None:
    if not has_schema(db, args.problem_schema) or not has_schema(db, args.content_schema):
        fail("源与目标必须是同一 MySQL 实例上已存在的 problem/content schema；本工具不创建数据库")
    validate_mode(db, args)
    existing = state_row(db, args)
    if existing:
        mode, status, saved_sha, _, _ = existing
        if status == "CUTOVER":
            fail("solution-domain 已 CUTOVER；禁止重新 prepare")
        if mode != args.source_mode or saved_sha != manifest_sha:
            fail("已有 migration state 的 source_mode/manifest SHA 与本次不一致")
    db.execute_file(manifest_migration_path(
        "V20261004_4", "content-prepare", args.source_mode, "db_content"))
    validate_content_schema(db, args.content_schema)
    # _4 can be applied to an empty target only. The state row remains monotonic.
    migration_state_prepare(db, args, manifest_sha)
    print(json.dumps({"phase": "prepare", "sourceMode": args.source_mode,
                      "state": (state_row(db, args) or [None, "UNKNOWN"])[1]}, ensure_ascii=False))


def copy(db: Mysql, args: argparse.Namespace, manifest_sha: str) -> None:
    require_maintenance(args)
    require_state(db, args, manifest_sha, {"PREPARED", "COPIED"})
    validate_mode(db, args)
    if args.source_mode == "LEGACY":
        # The frozen T02 normalization includes a one-time data backfill. Keep it
        # inside the explicitly confirmed maintenance copy phase, never prepare.
        normalize_legacy_source(db, args)
    validate_outbox_types(db, args.problem_schema, "problem_event_outbox")
    validate_outbox_types(db, args.content_schema, "content_event_outbox", target=True)
    if table_exists(db, args.problem_schema, "problem_event_outbox"):
        community_count = db.query(f"SELECT COUNT(*) FROM {sql_name(args.problem_schema)}.problem_event_outbox "
                                   f"WHERE event_type IN ({','.join(sql_text(x) for x in COMMUNITY_TYPES)})")[0]
        if args.source_mode == "FRESH" and community_count != "0":
            fail("FRESH 源没有旧题解表却包含 community outbox 事件；不能把失去业务来源的事件迁入新域")
        active = db.query(f"SELECT COUNT(*) FROM {sql_name(args.problem_schema)}.problem_event_outbox "
                          f"WHERE event_type IN ({','.join(sql_text(x) for x in COMMUNITY_TYPES)}) "
                          "AND status='SENDING' AND (lease_until IS NULL OR lease_until>NOW(3))")[0]
        if active != "0":
            fail("仍有未过期或无 lease 的社区 outbox SENDING 行；等待 lease 到期并确认 relay 已停止")
        db.execute(f"UPDATE {sql_name(args.problem_schema)}.problem_event_outbox "
                   "SET status='PENDING',lease_owner=NULL,lease_until=NULL "
                   f"WHERE event_type IN ({','.join(sql_text(x) for x in COMMUNITY_TYPES)}) "
                   "AND status='SENDING' AND lease_until<=NOW(3)")
    db.execute_file(manifest_migration_path(
        "V20261004_5", "copy", args.source_mode, "db_problem,db_content"))
    batches = 0
    for spec in TABLES:
        last = None
        while True:
            batch = get_key_batch(db, args, spec, last)
            if not batch:
                break
            source = keyed_rows(db, args.problem_schema, spec, batch)
            target = keyed_rows(db, args.content_schema, spec, batch)
            if len(source) != len(batch):
                fail(f"源表 {spec['name']} 批次主键集合变化；维护写入未冻结或源不稳定")
            for key, target_row in target.items():
                if key in source and source[key] != target_row:
                    fail(f"目标冲突且字段不完全相同：{spec['name']} 主键 {key}；未覆盖目标行")
            copy_call(db, spec, last, batch[-1])
            batches += 1
            if args.test_fail_after_batches and batches >= args.test_fail_after_batches:
                if (os.environ.get("OJ_SOLUTION_MIGRATION_TEST_MODE") != "1"
                        or args.problem_schema != "oj_it_problem" or args.content_schema != "oj_it_content"):
                    fail("批次故障注入仅允许 OJ_SOLUTION_MIGRATION_TEST_MODE=1 的隔离 oj_it schema")
                fail("test-only batch interruption injected after committed batch")
            last = batch[-1]
    seed_problem_references(db, args)
    db.execute("UPDATE " + sql_name(args.content_schema) + ".content_solution_migration_state "
               "SET status='COPIED' WHERE migration_key='solution-domain-v1' AND status IN ('PREPARED','COPIED') "
               "AND manifest_sha256=" + sql_text(manifest_sha))
    db.execute("DROP PROCEDURE IF EXISTS " + sql_name(args.content_schema) + ".content_solution_copy_batch")
    print(json.dumps({"phase": "copy", "committedBatches": batches, "state": "COPIED"}, ensure_ascii=False))


def check_integrity(db: Mysql, schema: str, args: argparse.Namespace, *, target: bool) -> list[str]:
    issues: list[str] = []
    for spec in TABLES:
        table = table_spec(spec)[1] if target else table_spec(spec)[0]
        if not table_exists(db, schema, table):
            if args.source_mode == "LEGACY":
                issues.append(f"缺少表 {schema}.{table}")
            continue
        if spec.get("filter") == "community":
            validate_outbox_types(db, schema, table, target=target)

    schema_sql = sql_name(schema)
    if table_exists(db, schema, "solution_explanation"):
        required = {"solution_explanation", "solution_explanation_content", "solution_like", "solution_comment",
                    "comment_like", "solution_moderation_action"}
        if target:
            required.update({"solution_problem_reference", "content_event_outbox"})
        missing = sorted(table for table in required if not table_exists(db, schema, table))
        if missing:
            issues.append("关系检查缺少依赖表：" + ",".join(missing) + f"（{schema}）")
            return issues
        solution_columns = set(db.query("SELECT column_name FROM information_schema.columns WHERE table_schema="
                                        + sql_text(schema) + " AND table_name='solution_explanation'"))
        needed_columns = {name for name, _ in TABLES[0]["columns"]}
        if not needed_columns.issubset(solution_columns):
            issues.append("题解元数据列不完整：" + ",".join(sorted(needed_columns - solution_columns)) + f"（{schema}）")
            return issues
        checks = {
            "题解缺正文": f"SELECT COUNT(*) FROM {schema_sql}.solution_explanation s LEFT JOIN {schema_sql}.solution_explanation_content c USING(solution_id) WHERE c.solution_id IS NULL",
            "正文找不到题解": f"SELECT COUNT(*) FROM {schema_sql}.solution_explanation_content c LEFT JOIN {schema_sql}.solution_explanation s USING(solution_id) WHERE s.solution_id IS NULL",
            "点赞计数不一致": f"SELECT COUNT(*) FROM {schema_sql}.solution_explanation s WHERE s.like_count<>(SELECT COUNT(*) FROM {schema_sql}.solution_like l WHERE l.solution_id=s.solution_id AND l.active=1)",
            "评论计数不一致": f"SELECT COUNT(*) FROM {schema_sql}.solution_explanation s WHERE s.comment_count<>(SELECT COUNT(*) FROM {schema_sql}.solution_comment c WHERE c.solution_id=s.solution_id AND c.state='VISIBLE')",
            "评论点赞计数不一致": f"SELECT COUNT(*) FROM {schema_sql}.solution_comment c WHERE c.like_count<>(SELECT COUNT(*) FROM {schema_sql}.comment_like l WHERE l.comment_id=c.comment_id AND l.active=1)",
            "回复计数不一致": f"SELECT COUNT(*) FROM {schema_sql}.solution_comment c WHERE c.root_id IS NULL AND c.reply_count<>(SELECT COUNT(*) FROM {schema_sql}.solution_comment r WHERE r.root_id=c.comment_id AND r.state='VISIBLE')",
            "评论 root/parent 关系孤儿或跨题解": f"SELECT COUNT(*) FROM {schema_sql}.solution_comment c LEFT JOIN {schema_sql}.solution_comment p ON p.comment_id=c.parent_id LEFT JOIN {schema_sql}.solution_comment r ON r.comment_id=c.root_id WHERE (c.parent_id IS NOT NULL AND (p.comment_id IS NULL OR p.solution_id<>c.solution_id OR c.root_id<>COALESCE(p.root_id,p.comment_id))) OR (c.root_id IS NOT NULL AND (c.parent_id IS NULL OR r.comment_id IS NULL OR r.solution_id<>c.solution_id OR r.root_id IS NOT NULL)) OR (c.root_id IS NULL AND c.parent_id IS NOT NULL)",
            "回复目标作者或层级关系错误": f"SELECT COUNT(*) FROM {schema_sql}.solution_comment c JOIN {schema_sql}.solution_comment p ON p.comment_id=c.parent_id WHERE c.root_id IS NOT NULL AND (c.reply_to_user_id IS NULL OR c.reply_to_user_id<>p.user_id)",
            "非根评论存在回复计数": f"SELECT COUNT(*) FROM {schema_sql}.solution_comment WHERE root_id IS NOT NULL AND reply_count<>0",
            "审核记录目标孤儿": f"SELECT COUNT(*) FROM {schema_sql}.solution_moderation_action a LEFT JOIN {schema_sql}.solution_explanation s ON s.solution_id=a.solution_id LEFT JOIN {schema_sql}.solution_comment c ON c.comment_id=a.target_id AND c.solution_id=a.solution_id WHERE s.solution_id IS NULL OR (a.target_type='COMMENT' AND c.comment_id IS NULL) OR a.target_type NOT IN ('SOLUTION','COMMENT')",
        }
        if target:
            problem_schema = sql_name(args.problem_schema)
            checks["题解关联题目投影缺失或过期"] = (
                f"SELECT COUNT(DISTINCT s.problem_id) FROM {schema_sql}.solution_explanation s "
                f"LEFT JOIN {schema_sql}.solution_problem_reference r ON r.problem_id=s.problem_id "
                f"LEFT JOIN {problem_schema}.problem p ON p.problem_id=s.problem_id "
                "WHERE r.problem_id IS NULL OR r.exists_flag<>(p.problem_id IS NOT NULL) "
                "OR r.del_flag<>COALESCE(p.del_flag,1) OR NOT (r.auth <=> p.auth) "
                "OR NOT (BINARY r.title <=> BINARY CASE WHEN p.problem_id IS NOT NULL AND p.del_flag=0 AND p.auth=1 THEN p.title ELSE NULL END)")
        for label, sql in checks.items():
            count = int(db.query(sql)[0])
            if count:
                issues.append(f"{label}：{count} 行（{schema}）")
    return issues


def validate_content_schema(db: Mysql, schema: str) -> None:
    required = {
        "solution_explanation": {name for name, _ in TABLES[0]["columns"]},
        "solution_explanation_content": {name for name, _ in TABLES[1]["columns"]},
        "solution_like": {name for name, _ in TABLES[2]["columns"]},
        "solution_comment": {name for name, _ in TABLES[3]["columns"]},
        "comment_like": {name for name, _ in TABLES[4]["columns"]},
        "solution_moderation_action": {name for name, _ in TABLES[5]["columns"]},
        "content_event_outbox": {name for name, _ in TABLES[6]["columns"]},
        "solution_problem_reference": {"problem_id", "exists_flag", "del_flag", "auth", "title", "checked_at", "requested_at"},
        "content_solution_migration_state": {"migration_key", "source_mode", "status", "manifest_sha256",
                                               "source_digest", "target_digest", "counts_json", "started_at",
                                               "verified_at", "cutover_at"},
    }
    for table, expected in required.items():
        if not table_exists(db, schema, table):
            fail(f"content schema 缺少表：{table}")
        columns = set(db.query("SELECT column_name FROM information_schema.columns WHERE table_schema="
                               + sql_text(schema) + " AND table_name=" + sql_text(table)))
        missing = expected - columns
        if missing:
            fail(f"content schema {table} 缺列：" + ",".join(sorted(missing)))
        collations = db.query("SELECT table_collation FROM information_schema.tables WHERE table_schema="
                              + sql_text(schema) + " AND table_name=" + sql_text(table))
        if not collations or collations[0] != "utf8mb4_unicode_ci":
            fail(f"content schema {table} collation 不符合 utf8mb4_unicode_ci")

    required_checks = {
        "solution_explanation": {"chk_solution_explanation_moderation_state",
                                 "chk_solution_explanation_counts_nonnegative"},
        "solution_like": {"chk_solution_like_first_notification"},
        "solution_comment": {"chk_solution_comment_state", "chk_solution_comment_counts_nonnegative"},
        "comment_like": {"chk_comment_like_first_notification"},
        "solution_moderation_action": {"chk_solution_moderation_target_type"},
        "content_event_outbox": {"chk_content_event_outbox_status", "chk_content_event_outbox_attempts",
                                 "chk_content_event_outbox_type"},
        "content_solution_migration_state": {"chk_content_solution_migration_source",
                                              "chk_content_solution_migration_status"},
    }
    present_checks = db.query("SELECT table_name,constraint_name FROM information_schema.table_constraints "
                              "WHERE table_schema=" + sql_text(schema) + " AND constraint_type='CHECK'")
    checks_by_table: dict[str, set[str]] = {}
    for row in present_checks:
        table, constraint = row.split("\t", 1)
        checks_by_table.setdefault(table, set()).add(constraint)
    for table, expected in required_checks.items():
        missing = expected - checks_by_table.get(table, set())
        if missing:
            fail(f"content solution 表 {table} 缺少 CHECK：" + ",".join(sorted(missing)))

    required_fks = {
        "solution_explanation_content_id_fk": "solution_explanation_content",
        "fk_solution_like_solution": "solution_like",
        "fk_solution_comment_solution": "solution_comment",
        "fk_comment_like_comment": "comment_like",
    }
    present_fks = db.query("SELECT constraint_name,table_name FROM information_schema.key_column_usage "
                           "WHERE table_schema=" + sql_text(schema) + " AND referenced_table_name IS NOT NULL")
    fks_by_name = {row.split("\t", 1)[0]: row.split("\t", 1)[1] for row in present_fks}
    missing_fks = {name for name, table in required_fks.items() if fks_by_name.get(name) != table}
    if missing_fks:
        fail("content solution 缺少本域外键：" + ",".join(sorted(missing_fks)))
    bad_delete_rules = db.query("SELECT constraint_name FROM information_schema.referential_constraints "
                                "WHERE constraint_schema=" + sql_text(schema) + " AND constraint_name IN ("
                                + ",".join(sql_text(name) for name in required_fks)
                                + ") AND delete_rule<>'RESTRICT'")
    if bad_delete_rules:
        fail("content solution 外键不是 RESTRICT：" + ",".join(bad_delete_rules))

    tables_sql = ",".join(sql_text(table) for table in required)
    cross_schema_fk = db.query("SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema="
                               + sql_text(schema) + " AND table_name IN (" + tables_sql + ") "
                               "AND referenced_table_schema IS NOT NULL "
                               "AND referenced_table_schema<>" + sql_text(schema))
    if cross_schema_fk and cross_schema_fk[0] != "0":
        fail("content solution 表存在跨 schema 外键；停止以避免运行态跨域约束")


def calculate(db: Mysql, args: argparse.Namespace) -> tuple[str, str, dict]:
    source_details: dict[str, tuple[str, int]] = {}
    target_details: dict[str, tuple[str, int]] = {}
    counts = {}
    for spec in TABLES:
        name = spec["name"]
        src_exists = source_table_exists(db, args, spec)
        dst_table = table_spec(spec)[1]
        dst_exists = table_exists(db, args.content_schema, dst_table)
        if args.source_mode == "LEGACY" and not src_exists:
            fail(f"旧源表缺失：{table_spec(spec)[0]}")
        src_hash, src_rows = stream_digest(db, args.problem_schema, spec, src_exists, target=False)
        dst_hash, dst_rows = stream_digest(db, args.content_schema, spec, dst_exists, target=True)
        source_details[name] = (src_hash, src_rows)
        target_details[name] = (dst_hash, dst_rows)
        counts[name] = {"source": src_rows, "target": dst_rows}
    return aggregate_digest(source_details), aggregate_digest(target_details), counts


def verify(db: Mysql, args: argparse.Namespace, manifest_sha: str, *, for_cutover: bool = False) -> None:
    row = require_state(db, args, manifest_sha, {"VERIFIED"} if for_cutover else {"COPIED", "VERIFIED", "CUTOVER"})
    validate_mode(db, args)
    if not for_cutover and row[1] == "CUTOVER":
        issues = check_integrity(db, args.content_schema, args, target=True)
        if issues:
            fail("CUTOVER 后 content 自身完整性检查失败：" + "; ".join(issues))
        _, target_digest, counts = calculate(db, argparse.Namespace(
            source_mode="FRESH", problem_schema=args.content_schema, content_schema=args.content_schema))
        print(json.dumps({"phase": "verify", "status": "CUTOVER", "sourceComparison": "skipped-after-cutover",
                          "targetDigest": target_digest, "counts": counts}, ensure_ascii=False, sort_keys=True))
        return
    src_issues = check_integrity(db, args.problem_schema, args, target=False)
    dst_issues = check_integrity(db, args.content_schema, args, target=True)
    if src_issues or dst_issues:
        fail("迁移完整性检查发现问题（不自动修复/丢弃数据）：" + "; ".join(src_issues + dst_issues))
    source_digest, target_digest, counts = calculate(db, args)
    if source_digest != target_digest:
        fail("源/目标逐行 SHA-256 或行数不一致；不会推进 VERIFIED/CUTOVER。counts="
             + json.dumps(counts, ensure_ascii=False, sort_keys=True))
    counts_json = json.dumps(counts, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    if for_cutover:
        if row[3] != source_digest or row[4] != target_digest:
            fail("CUTOVER 前摘要与已 VERIFIED 摘要不同；源在验证后发生变化或目标变化")
        update = ("UPDATE " + sql_name(args.content_schema) + ".content_solution_migration_state "
                  "SET status='CUTOVER',cutover_at=NOW(3) WHERE migration_key='solution-domain-v1' "
                  "AND status='VERIFIED' AND manifest_sha256=" + sql_text(manifest_sha)
                  + " AND source_digest=" + sql_text(source_digest) + " AND target_digest=" + sql_text(target_digest))
        updated = db.query(update + "; SELECT ROW_COUNT()")
        if not updated or updated[-1] != "1":
            fail("CUTOVER CAS 未更新一行；迁移状态已变化")
        status = "CUTOVER"
    elif row[1] == "VERIFIED":
        if row[3] != source_digest or row[4] != target_digest:
            fail("重复 verify 的摘要与已 VERIFIED 状态不同")
        status = "VERIFIED"
    else:
        update = ("UPDATE " + sql_name(args.content_schema) + ".content_solution_migration_state "
                  "SET status='VERIFIED',source_digest=" + sql_text(source_digest) + ",target_digest="
                  + sql_text(target_digest) + ",counts_json=" + sql_text(counts_json) + ",verified_at=NOW(3) "
                  "WHERE migration_key='solution-domain-v1' AND status='COPIED' AND manifest_sha256="
                  + sql_text(manifest_sha))
        updated = db.query(update + "; SELECT ROW_COUNT()")
        if not updated or updated[-1] != "1":
            fail("VERIFIED 状态更新未成功；检查 migration state")
        status = "VERIFIED"
    print(json.dumps({"phase": "cutover" if for_cutover else "verify", "status": status,
                      "sourceDigest": source_digest, "targetDigest": target_digest, "counts": counts},
                     ensure_ascii=False, sort_keys=True))


def inspect(db: Mysql, args: argparse.Namespace, manifest_sha: str) -> None:
    schemas = {"problem": has_schema(db, args.problem_schema), "content": has_schema(db, args.content_schema)}
    source_exists = schemas["problem"] and table_exists(db, args.problem_schema, "solution_explanation")
    target_state = state_row(db, args)
    args.source_mode = "LEGACY" if source_exists else "FRESH"
    source_outbox_types = []
    source_outbox_status = []
    if schemas["problem"] and table_exists(db, args.problem_schema, "problem_event_outbox"):
        source_outbox_types = [row.split("\t")[0] for row in db.query(
            f"SELECT DISTINCT event_type FROM {sql_name(args.problem_schema)}.problem_event_outbox ORDER BY event_type")]
        source_outbox_status = db.query(f"SELECT event_type,status,COUNT(*) FROM {sql_name(args.problem_schema)}.problem_event_outbox "
                                        "GROUP BY event_type,status ORDER BY event_type,status")
    schema_layout = {}
    table_collations = {}
    foreign_keys = {}
    expected_tables = {
        args.problem_schema: ("solution_explanation", "solution_explanation_content", "solution_like", "solution_comment",
                              "comment_like", "solution_moderation_action", "problem_event_outbox"),
        args.content_schema: ("solution_explanation", "solution_explanation_content", "solution_like", "solution_comment",
                              "comment_like", "solution_moderation_action", "content_event_outbox",
                              "solution_problem_reference", "content_solution_migration_state"),
    }
    for schema, tables in expected_tables.items():
        if not schemas["problem" if schema == args.problem_schema else "content"]:
            continue
        for table in tables:
            key = schema + "." + table
            if not table_exists(db, schema, table):
                schema_layout[key] = {"exists": False}
                continue
            schema_layout[key] = {
                "exists": True,
                "columns": db.query("SELECT column_name FROM information_schema.columns WHERE table_schema="
                                    + sql_text(schema) + " AND table_name=" + sql_text(table) + " ORDER BY ordinal_position"),
                "rows": db.query(f"SELECT COUNT(*) FROM {sql_name(schema)}.{sql_name(table)}")[0],
            }
            table_collations[key] = db.query("SELECT table_collation FROM information_schema.tables WHERE table_schema="
                                             + sql_text(schema) + " AND table_name=" + sql_text(table))[0]
            foreign_keys[key] = db.query("SELECT CONCAT(k.constraint_name,':',COALESCE(k.referenced_table_schema,''),'.',"
                "COALESCE(k.referenced_table_name,''),':',r.delete_rule) "
                "FROM information_schema.key_column_usage k LEFT JOIN information_schema.referential_constraints r "
                "ON r.constraint_schema=k.constraint_schema AND r.constraint_name=k.constraint_name "
                "WHERE k.table_schema=" + sql_text(schema) + " AND k.table_name=" + sql_text(table)
                + " AND k.referenced_table_name IS NOT NULL ORDER BY k.constraint_name")
    first_publish_marker = []
    if source_exists:
        first_publish_marker = db.query("SELECT column_comment FROM information_schema.columns WHERE table_schema="
                                        + sql_text(args.problem_schema) + " AND table_name='solution_explanation' "
                                        "AND column_name='first_published_at'")
    integrity = {}
    for schema, target in ((args.problem_schema, False), (args.content_schema, True)):
        if not has_schema(db, schema):
            continue
        try:
            integrity[schema] = check_integrity(db, schema, args, target=target)
        except RuntimeError as exc:
            integrity[schema] = [str(exc)]
    permissions = {"available": False}
    if has_schema(db, args.user_schema) and all(table_exists(db, args.user_schema, table)
                                                for table in ("sys_menu", "sys_role_menu", "sys_role")):
        permissions = {"available": True, "grants": db.query(
            "SELECT m.perms,COUNT(DISTINCT r.role_name) FROM " + sql_name(args.user_schema)
            + ".sys_menu m LEFT JOIN " + sql_name(args.user_schema)
            + ".sys_role_menu rm ON rm.menu_id=m.menu_id LEFT JOIN " + sql_name(args.user_schema)
            + ".sys_role r ON r.role_id=rm.role_id AND r.role_name IN ('admin','super_admin') "
            "WHERE m.perms IN ('problem:comment:list','problem:comment:remove','problem:judge:dispatch:retry') "
            "GROUP BY m.perms ORDER BY m.perms")}
    result = {
        "phase": "inspect", "mysqlVersion": db.query("SELECT VERSION()")[0],
        "schemas": schemas, "detectedSourceMode": "LEGACY" if source_exists else "FRESH",
        "legacySolutionTable": source_exists,
        "contentMigrationState": target_state,
        "problemOutboxTypes": source_outbox_types,
        "problemOutboxStatus": source_outbox_status,
        "firstPublishedColumnComment": first_publish_marker,
        "schemaLayout": schema_layout,
        "tableCollations": table_collations,
        "foreignKeys": foreign_keys,
        "integrityFindings": integrity,
        "interactionPermissionGrants": permissions,
        "maintenanceProcesses": "not inspectable from MySQL; write phases require explicit operator confirmations",
        "manifestSha256": manifest_sha,
        "historicalMigrationSha256": {
            name: next(row["sha256"] for row in read_manifest()[0] if row["file"] == name)
            for name in ("V20261004_1__user_social.sql", "V20261004_2__solution_interactions.sql",
                         "V20261004_3__interaction_permissions.sql")
        },
    }
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="受控的题解域维护迁移工具；默认 inspect 只读")
    parser.add_argument("--phase", choices=("inspect", "prepare", "copy", "verify", "cutover"), default="inspect")
    parser.add_argument("--source-mode", choices=("LEGACY", "FRESH"))
    parser.add_argument("--host", default=os.environ.get("OJ_MIGRATION_MYSQL_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("OJ_MIGRATION_MYSQL_PORT", "3306")))
    parser.add_argument("--user", default=os.environ.get("OJ_MIGRATION_MYSQL_USER", "root"))
    parser.add_argument("--problem-schema", default=os.environ.get("OJ_MIGRATION_PROBLEM_SCHEMA", "db_problem"))
    parser.add_argument("--content-schema", default=os.environ.get("OJ_MIGRATION_CONTENT_SCHEMA", "db_content"))
    parser.add_argument("--user-schema", default=os.environ.get("OJ_MIGRATION_USER_SCHEMA", "db_user"))
    parser.add_argument("--confirm-source-backup", action="store_true")
    parser.add_argument("--confirm-maintenance", action="store_true")
    parser.add_argument("--confirm-source-writes-stopped", action="store_true")
    parser.add_argument("--confirm-relays-stopped", action="store_true")
    parser.add_argument("--test-fail-after-batches", type=int, default=0, help=argparse.SUPPRESS)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    validate_schema_names(args)
    _, manifest_sha = read_manifest()
    db = Mysql(args)
    if args.phase == "inspect":
        inspect(db, args, manifest_sha)
        return 0
    require_mysql8(db)
    if not args.source_mode:
        fail("prepare/copy/verify/cutover 必须显式指定 --source-mode LEGACY|FRESH")
    if args.phase == "prepare":
        prepare(db, args, manifest_sha)
    elif args.phase == "copy":
        copy(db, args, manifest_sha)
    elif args.phase == "verify":
        current = state_row(db, args)
        if not current or current[1] != "CUTOVER":
            require_maintenance(args)
        verify(db, args, manifest_sha)
    elif args.phase == "cutover":
        require_maintenance(args)
        verify(db, args, manifest_sha, for_cutover=True)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"solution-domain migration stopped: {exc}", file=sys.stderr)
        sys.exit(1)
