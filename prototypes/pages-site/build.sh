#!/usr/bin/env bash
# PROTOTYPE build: renders variants a, b, c with plain Asciidoctor into out/.
# The real site builds with asciidoctor-maven-plugin in the docs module; this only shows the look.
set -euo pipefail
cd "$(dirname "$0")"
here=$(pwd)
AD=${ASCIIDOCTOR:-asciidoctor}
rm -rf out && mkdir -p out
for v in a b c; do
  cat "$v/docinfo-footer.html" shared/switcher.html > "/tmp/docinfo-footer-$v.html"
  mkdir -p "out/$v/_di" && cp "$v"/docinfo*.html "out/$v/_di/" && cp "/tmp/docinfo-footer-$v.html" "out/$v/_di/docinfo-footer.html"
  extra=()
  [ -f "$v/attributes" ] && read -r -a extra < "$v/attributes"
  [ -f "$v/theme.css" ] && extra+=(-a stylesheet="$here/$v/theme.css")
  find "$v" -name '*.adoc' | while read -r f; do
    rel=${f#"$v"/}
    "$AD" -S unsafe -a shared="$here/shared" -a project-version=6.0.0 \
      -a docinfo=shared -a docinfodir="$here/out/$v/_di" \
      -a source-highlighter=rouge -a rouge-css=style -a nofooter "${extra[@]}" \
      -o "$here/out/$v/${rel%.adoc}.html" "$f"
  done
  rm -rf "out/$v/_di"
done
echo "rendered: $(find out -name '*.html' | sort | tr '\n' ' ')"
