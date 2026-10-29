#!/usr/bin/env bash
# Compiles the project + all tests and runs them. Run from the project root:  bash test/run_tests.sh
# Needs a JDK (javac). If only a JRE is installed, set:  JAVAC="java -m jdk.compiler/com.sun.tools.javac.Main"
# Also needs python3 for step 6 (the .form consistency check).
set -euo pipefail
JAVAC="${JAVAC:-javac}"
OUT="$(mktemp -d)"
LIB="dist/lib/AbsoluteLayout.jar"          # NetBeans' AbsoluteLayout library (same jar the project uses)
echo "== 1. compile project sources"
$JAVAC -encoding UTF-8 -cp "$LIB" -d "$OUT/classes" $(find src -name '*.java')
echo "== 2. compile tests"
$JAVAC -encoding UTF-8 -cp "$OUT/classes:$LIB" -d "$OUT/tests" $(find test -name '*.java')
echo "== 3. Part 6.1 service tests (temp folders; the real data/ folder is only read, never written)"
java -cp "$OUT/classes:$OUT/tests" com.system.service.Part61ServiceTests data
echo "== 4. Part 6.2 service tests"
java -cp "$OUT/classes:$OUT/tests" com.system.service.Part62ServiceTests
echo "== 5. headless GUI wiring tests (no windows are shown; see the headers of the classes for their limits)"
java -Djava.awt.headless=true -cp "$OUT/classes:$OUT/tests:$LIB" com.system.gui.GuiWiringSmokeTest
java -Djava.awt.headless=true -cp "$OUT/classes:$OUT/tests:$LIB" com.system.gui.Part62GuiWiringTest
echo "== 6. .java <-> .form consistency (NetBeans wiring)"
python3 test/check_forms.py
echo "== 7. layer / architecture checks"
bash test/check_architecture.sh
echo "ALL TEST RUNS FINISHED OK"
