#!/usr/bin/env python3
"""Checks that JS modules imported by Scala.js code exist in node_modules.

Facades import npm modules by the paths of the d.ts files they were generated from, which is not
always a path that exists at runtime (e.g. a package that ships only a bundle, or restricts deep
imports with `exports`). Compilation does not notice it, the bundler fails on the first request.

Usage:
  check-js-imports.py --node-modules <dir> --main-js <linked main.js>   imports a linked app really uses
  check-js-imports.py --node-modules <dir> --facades <dir>              all @JSImport of the generated modules
                                                                        (also lists imports that are never linked)
Exits with 1 if something does not resolve. Aliases of the bundler (e.g. vite resolve.alias) are not known.
"""
import argparse
import collections
import json
import os
import re
import sys

SUFFIXES = ("", ".js", ".mjs", ".cjs", ".json", "/index.js", "/index.mjs")


def split(spec):
    parts = spec.split("/")
    n = 2 if spec.startswith("@") else 1
    return "/".join(parts[:n]), "/".join(parts[n:])


def is_file(path):
    return any(os.path.isfile(path + suffix) for suffix in SUFFIXES)


def is_package_dir(path):
    package_json = os.path.join(path, "package.json")
    if os.path.isfile(package_json):
        with open(package_json) as f:
            meta = json.load(f)
        if any(k in meta and is_file(os.path.join(path, meta[k])) for k in ("module", "main")):
            return True
    return is_file(path)


def targets(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for v in value.values():
            yield from targets(v)
    elif isinstance(value, list):
        for v in value:
            yield from targets(v)


def pattern(key):
    return re.escape(key).replace(r"\*", "(.*)")


def resolve(node_modules, spec):
    """True if the import resolves, otherwise the reason."""
    package, subpath = split(spec)
    package_dir = os.path.join(node_modules, package)
    if not os.path.isdir(package_dir):
        return "package is not installed"
    with open(os.path.join(package_dir, "package.json")) as f:
        exports = json.load(f).get("exports")

    if exports is None:
        if not subpath:
            return True if is_package_dir(package_dir) else "entry point does not exist"
        return True if is_file(os.path.join(package_dir, subpath)) else "file does not exist"

    if isinstance(exports, (str, list)) or not any(k.startswith(".") for k in exports):
        exports = {".": exports}
    key = "./" + subpath if subpath else "."
    for export_key, value in exports.items():
        match = re.fullmatch(pattern(export_key), key) if "*" in export_key else None
        if export_key != key and not match:
            continue
        for target in targets(value):
            if match:
                target = target.replace("*", match.group(1))
            if os.path.isfile(os.path.join(package_dir, target)):
                return True
        return "exports target does not exist"
    return "not listed in exports"


def linked_imports(main_js):
    with open(main_js, encoding="utf8", errors="ignore") as f:
        text = f.read()
    return {s: [] for s in re.findall(r'^import [^;]* from "([^"./][^"]*)"', text, re.MULTILINE)}


def facade_imports(facades_dir):
    found = collections.defaultdict(list)
    pattern_ = re.compile(r'@JSImport\("([^"]+)"')
    for directory, _, files in os.walk(facades_dir):
        if "/target" in directory:
            continue
        for name in files:
            if name.endswith(".scala"):
                with open(os.path.join(directory, name), encoding="utf8", errors="ignore") as f:
                    for spec in pattern_.findall(f.read()):
                        found[spec].append(os.path.relpath(directory, facades_dir).split("/")[0])
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--node-modules", required=True)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--main-js")
    source.add_argument("--facades")
    args = parser.parse_args()

    imports = linked_imports(args.main_js) if args.main_js else facade_imports(args.facades)
    unresolved = {}
    for spec, modules in sorted(imports.items()):
        result = resolve(args.node_modules, spec)
        if result is not True:
            unresolved[spec] = (result, sorted(set(modules))[:3])

    print(f"checked {len(imports)} imports, unresolved: {len(unresolved)}")
    for spec, (reason, modules) in unresolved.items():
        print(f"  {spec}: {reason}" + (f" (modules: {', '.join(modules)})" if modules else ""))
    return 1 if unresolved else 0


if __name__ == "__main__":
    sys.exit(main())
