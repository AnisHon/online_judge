#!/usr/bin/env python3
import json
import os
import secrets
import subprocess
import sys
from pathlib import Path


def container_env(name: str) -> dict[str, str]:
    raw = subprocess.check_output(["docker", "inspect", name], text=True)
    result: dict[str, str] = {}
    for entry in json.loads(raw)[0]["Config"].get("Env", []):
        key, separator, value = entry.partition("=")
        if separator:
            result[key] = value
    return result


def read_dotenv(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.lstrip().startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key] = value.strip().strip("'\"")
    return result


def dotenv_value(value: str) -> str:
    return "'" + value.replace("'", "'\"'\"'") + "'"


target = Path(sys.argv[1] if len(sys.argv) > 1 else ".env")
release_tag = sys.argv[2] if len(sys.argv) > 2 else os.environ["RELEASE_TAG"]
existing = read_dotenv(target)
mysql = container_env("oj-mysql")
rabbit = container_env("oj-mq")
minio = container_env("oj-minio")

values = {
    "RELEASE_TAG": release_tag,
    "MYSQL_ROOT_PASSWORD": mysql["MYSQL_ROOT_PASSWORD"],
    "RABBITMQ_DEFAULT_USER": rabbit["RABBITMQ_DEFAULT_USER"],
    "RABBITMQ_DEFAULT_PASS": rabbit["RABBITMQ_DEFAULT_PASS"],
    "MINIO_ROOT_USER": minio["MINIO_ROOT_USER"],
    "MINIO_ROOT_PASSWORD": minio["MINIO_ROOT_PASSWORD"],
    "OJ_JWT_SECRET": existing.get("OJ_JWT_SECRET") or secrets.token_urlsafe(48),
    "OJ_JWT_ACCESS_SECRET": existing.get("OJ_JWT_ACCESS_SECRET") or secrets.token_urlsafe(48),
    "OJ_JWT_REFRESH_SECRET": existing.get("OJ_JWT_REFRESH_SECRET") or secrets.token_urlsafe(48),
    "OJ_DEFAULT_PASSWORD": existing.get("OJ_DEFAULT_PASSWORD") or secrets.token_urlsafe(18),
}
target.write_text(
    "\n".join(f"{key}={dotenv_value(value)}" for key, value in values.items()) + "\n",
    encoding="utf-8",
)
target.chmod(0o600)
print(f"Wrote {target} with preserved infrastructure credentials and non-persistent release tag.")
