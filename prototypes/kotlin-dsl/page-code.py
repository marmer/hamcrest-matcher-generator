#!/usr/bin/env python3
"""PROTOTYPE helper: merges the // @code snippets of every candidate into spec.json's "code" map
(keys "GROUP ID/VARIANT", read by scripts/agents/prototype-page.py). Usage: page-code.py spec.json > full.json"""
import glob, json, re, sys, textwrap

def snippets(path):
    src = open(path, encoding="utf-8").read()
    for fn, body in re.findall(r"fun (\w+)\(\)[^\n]*\n\s*// @code\n(.*?)\n\s*// @end", src, re.S):
        yield fn, textwrap.dedent(body)

code = {}
for path in glob.glob("kotlin/proto/demo/K*.kt"):
    variant = path.rsplit("/", 1)[1][:-3]
    for fn, body in snippets(path):
        code[f"HAMCREST S{fn[1:]}/{variant}"] = body
for fn, body in snippets("kotlin/proto/demo/AssertJ.kt"):
    code[f"ASSERTJ {fn.upper()}/AS_IS"] = body
for path in sorted(glob.glob("compile/*.kt")):
    sid, _, variant = path.rsplit("/", 1)[1][:-3].split("_")
    body = re.search(r"// @code\n(.*?)\n// @end", open(path, encoding="utf-8").read(), re.S).group(1)
    code[f"COMPILE {sid}/{variant}"] = body
spec = json.load(open(sys.argv[1], encoding="utf-8"))
spec["code"] = {**code, **spec.get("code", {})}
json.dump(spec, sys.stdout, indent=1)
