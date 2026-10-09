#!/usr/bin/env python3
"""Poll authenticated Actuator metrics without logging the JWT."""

import csv
import json
import os
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8084").rstrip("/")
TOKEN_FILE = Path(os.environ.get("TOKEN_FILE", ".k6-local-tokens"))
INTERVAL = float(os.environ.get("INTERVAL_SECONDS", "0.5"))
DURATION = float(os.environ.get("DURATION_SECONDS", "390"))
timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
prefix = os.environ.get("OUTPUT_PREFIX", f"stress-metrics-{timestamp}")

METRICS = {
    "hikari_active": "hikaricp.connections.active",
    "hikari_idle": "hikaricp.connections.idle",
    "hikari_pending": "hikaricp.connections.pending",
    "hikari_max": "hikaricp.connections.max",
    "hikari_timeout": "hikaricp.connections.timeout",
    "hikari_acquire": "hikaricp.connections.acquire",
    "hikari_usage": "hikaricp.connections.usage",
    "http_active": "http.server.requests.active",
    "process_cpu": "process.cpu.usage",
    "system_cpu": "system.cpu.usage",
    "jvm_memory_used": "jvm.memory.used",
    "jvm_gc_pause": "jvm.gc.pause",
    "jvm_threads_live": "jvm.threads.live",
    "tomcat_busy": "tomcat.threads.busy",
    "tomcat_current": "tomcat.threads.current",
    "tomcat_config_max": "tomcat.threads.config.max",
}


def read_token():
    tokens = [token.strip() for token in TOKEN_FILE.read_text().split(",") if token.strip()]
    if not tokens:
        raise SystemExit(f"No JWT found in {TOKEN_FILE}")
    return tokens[0]


TOKEN = read_token()


def fetch(item):
    key, metric = item
    request = urllib.request.Request(
        f"{BASE_URL}/actuator/metrics/{metric}",
        headers={"Authorization": f"Bearer {TOKEN}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=3) as response:
            payload = json.load(response)
        measurements = {entry["statistic"]: entry["value"] for entry in payload.get("measurements", [])}
        return key, measurements, None
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, json.JSONDecodeError) as exc:
        return key, {}, f"{type(exc).__name__}:{getattr(exc, 'code', '')}"


def value(measurements, statistic="VALUE"):
    result = measurements.get(statistic)
    return "" if result is None else result


fieldnames = [
    "timestamp_utc", "elapsed_seconds", "errors",
    "hikari_active", "hikari_idle", "hikari_pending", "hikari_max", "hikari_timeout_count",
    "hikari_acquire_count", "hikari_acquire_total_seconds", "hikari_acquire_max_seconds",
    "hikari_usage_count", "hikari_usage_total_seconds", "hikari_usage_max_seconds",
    "http_active", "process_cpu", "system_cpu", "jvm_memory_used_bytes",
    "jvm_gc_pause_count", "jvm_gc_pause_total_seconds", "jvm_gc_pause_max_seconds",
    "jvm_threads_live", "tomcat_busy", "tomcat_current", "tomcat_config_max",
]

rows = []
started = time.monotonic()
while True:
    elapsed = time.monotonic() - started
    if elapsed > DURATION and rows:
        break
    results = dict()
    errors = []
    for item in METRICS.items():
        key, measurements, error = fetch(item)
        results[key] = measurements
        if error:
            errors.append(f"{key}={error}")
    rows.append({
            "timestamp_utc": datetime.now(timezone.utc).isoformat(),
            "elapsed_seconds": round(elapsed, 3),
            "errors": ";".join(errors),
            "hikari_active": value(results["hikari_active"]),
            "hikari_idle": value(results["hikari_idle"]),
            "hikari_pending": value(results["hikari_pending"]),
            "hikari_max": value(results["hikari_max"]),
            "hikari_timeout_count": value(results["hikari_timeout"], "COUNT"),
            "hikari_acquire_count": value(results["hikari_acquire"], "COUNT"),
            "hikari_acquire_total_seconds": value(results["hikari_acquire"], "TOTAL_TIME"),
            "hikari_acquire_max_seconds": value(results["hikari_acquire"], "MAX"),
            "hikari_usage_count": value(results["hikari_usage"], "COUNT"),
            "hikari_usage_total_seconds": value(results["hikari_usage"], "TOTAL_TIME"),
            "hikari_usage_max_seconds": value(results["hikari_usage"], "MAX"),
            "http_active": value(results["http_active"], "ACTIVE_TASKS"),
            "process_cpu": value(results["process_cpu"]),
            "system_cpu": value(results["system_cpu"]),
            "jvm_memory_used_bytes": value(results["jvm_memory_used"]),
            "jvm_gc_pause_count": value(results["jvm_gc_pause"], "COUNT"),
            "jvm_gc_pause_total_seconds": value(results["jvm_gc_pause"], "TOTAL_TIME"),
            "jvm_gc_pause_max_seconds": value(results["jvm_gc_pause"], "MAX"),
            "jvm_threads_live": value(results["jvm_threads_live"]),
            "tomcat_busy": value(results["tomcat_busy"]),
            "tomcat_current": value(results["tomcat_current"]),
            "tomcat_config_max": value(results["tomcat_config_max"]),
    })
    remaining = INTERVAL - ((time.monotonic() - started) - elapsed)
    if remaining > 0:
        time.sleep(remaining)

csv_path = Path(f"{prefix}.csv")
with csv_path.open("w", newline="") as output:
    writer = csv.DictWriter(output, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)


def numeric(column):
    return [float(row[column]) for row in rows if row[column] != ""]


def maximum(column):
    values = numeric(column)
    return max(values) if values else None


def minimum(column):
    values = numeric(column)
    return min(values) if values else None


def delta(column):
    values = numeric(column)
    return values[-1] - values[0] if len(values) >= 2 else None


summary = {
    "samples": len(rows),
    "durationSeconds": round(time.monotonic() - started, 3),
    "maximumHikariActive": maximum("hikari_active"),
    "minimumHikariIdle": minimum("hikari_idle"),
    "maximumHikariPending": maximum("hikari_pending"),
    "hikariTimeoutCountDelta": delta("hikari_timeout_count"),
    "maximumAcquireSeconds": maximum("hikari_acquire_max_seconds"),
    "maximumUsageSeconds": maximum("hikari_usage_max_seconds"),
    "maximumActiveHttpRequests": maximum("http_active"),
    "maximumTomcatBusyThreads": maximum("tomcat_busy"),
    "maximumTomcatCurrentThreads": maximum("tomcat_current"),
    "configuredTomcatMaxThreads": maximum("tomcat_config_max"),
    "maximumJvmLiveThreads": maximum("jvm_threads_live"),
    "maximumProcessCpu": maximum("process_cpu"),
    "maximumSystemCpu": maximum("system_cpu"),
    "gcPauseCountDelta": delta("jvm_gc_pause_count"),
    "maximumGcPauseSeconds": maximum("jvm_gc_pause_max_seconds"),
    "memoryHighWaterBytes": maximum("jvm_memory_used_bytes"),
    "samplesWithErrors": sum(bool(row["errors"]) for row in rows),
}
summary_path = Path(f"{prefix}-summary.json")
summary_path.write_text(json.dumps(summary, indent=2) + "\n")
print(f"Metrics CSV: {csv_path}")
print(f"Metrics summary: {summary_path}")
