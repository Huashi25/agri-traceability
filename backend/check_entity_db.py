# -*- coding: utf-8 -*-
"""
实体字段 vs 数据库列 全量核对脚本

作用：
    把 backend 下所有 @TableName 实体里的字段，逐个和数据库实际列做比对，
    找出「实体有、库里没有」的字段 —— 这类缺口会让 MyBatis-Plus 拼出的 SELECT
    直接报 Unknown column，相关接口全线 500，而且只有调用到该接口时才暴露。

做法：
    1. 正则解析 entity/*.java：@TableName("表名") + private 字段名 + @TableField(exist = false)
    2. 驼峰转下划线，和 `DESC 表名` 的列名集合比对
    3. 输出缺口表；有缺口时退出码 1

运行：
    python check_entity_db.py
"""

import os
import re
import subprocess
import sys

ENTITY_DIR = r"E:\agri-traceability\backend\src\main\java\com\agri\traceability\entity"
MYSQL = r"E:\MySQL\bin\mysql.exe"
DB = "agri_traceability"
USER = "root"
PWD = "root"


def camel_to_snake(name):
    """驼峰转下划线：merchantId -> merchant_id；URLPath -> url_path"""
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def parse_entities():
    """
    解析实体文件，返回 {表名: [(字段名, 文件, 行号), ...]}
    跳过 @TableField(exist = false) 标注的非数据库字段。
    """
    result = {}
    for fn in os.listdir(ENTITY_DIR):
        if not fn.endswith(".java"):
            continue
        path = os.path.join(ENTITY_DIR, fn)
        with open(path, encoding="utf-8") as f:
            text = f.read()
        m = re.search(r'@TableName\(\s*"([^"]+)"', text)
        if not m:
            continue
        table = m.group(1)
        fields = []
        # 逐行扫，便于处理 @TableField(exist=false) 与字段的相邻关系
        lines = text.splitlines()
        skip_next = False
        for i, line in enumerate(lines, 1):
            if "@TableField" in line and re.search(r"exist\s*=\s*false", line):
                skip_next = True
                continue
            fm = re.search(r"^\s*private\s+[\w<>,\[\]\s\.]+?\s+(\w+)\s*;", line)
            if fm:
                if skip_next:
                    skip_next = False
                    continue
                fields.append((fm.group(1), fn, i))
        result[table] = fields
    return result


def describe(table):
    """取某张表的实际列名集合；表不存在返回 None"""
    cmd = [MYSQL, "-u" + USER, "-p" + PWD, "--default-character-set=utf8mb4",
           DB, "-N", "-B", "-e", "SHOW COLUMNS FROM `%s`;" % table]
    p = subprocess.run(cmd, capture_output=True, timeout=30)
    if p.returncode != 0:
        return None
    out = p.stdout.decode("utf-8", errors="replace").strip()
    if not out:
        return None
    return {line.split("\t")[0] for line in out.splitlines() if line.strip()}


def main():
    entities = parse_entities()
    print("扫描到 %d 个实体类\n" % len(entities))

    missing_tables = []
    gaps = []
    for table, fields in sorted(entities.items()):
        cols = describe(table)
        if cols is None:
            missing_tables.append(table)
            continue
        for name, fn, ln in fields:
            col = camel_to_snake(name)
            if col not in cols:
                gaps.append((table, name, col, fn, ln))

    if missing_tables:
        print("=== 库里不存在的表 ===")
        for t in missing_tables:
            print("  [缺表] %s" % t)
        print()

    if gaps:
        print("=== 实体有字段、库里没有列（会导致 Unknown column）===")
        print("  %-20s %-18s %-20s %s" % ("表", "实体字段", "期望列名", "来源"))
        for table, name, col, fn, ln in gaps:
            print("  %-20s %-18s %-20s %s:%d" % (table, name, col, fn, ln))
    else:
        print("=== 无缺口：所有实体字段在库里都有对应列 ===")

    print("\n汇总：缺表 %d 张，缺列 %d 个" % (len(missing_tables), len(gaps)))
    if missing_tables or gaps:
        sys.exit(1)


if __name__ == "__main__":
    main()
