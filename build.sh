#!/usr/bin/env bash
# Cross-platform build script for XpoTriad (Paper plugin).
# Native on Linux/macOS; runs on Windows via Git Bash or WSL.
# Requires a JDK on PATH (provides javac + jar).
set -euo pipefail
cd "$(dirname "$0")"

echo '==> Cleaning build directory...'
rm -rf build
mkdir -p build/classes

echo '==> Building classpath...'
shopt -s nullglob
jars=(lib/*.jar)
shopt -u nullglob
if [ "${#jars[@]}" -eq 0 ]; then
    echo 'ERROR: No JARs found in lib/' >&2
    exit 1
fi
echo "    Found ${#jars[@]} JAR(s) on classpath"

echo '==> Collecting sources...'
mapfile -t sources < <(find src/main/java -type f -name '*.java' | sort)
total=${#sources[@]}
if [ "$total" -eq 0 ]; then
    echo 'ERROR: No .java sources found under src/main/java' >&2
    exit 1
fi
echo "    Found $total source file(s)"

# Compile in ONE javac call (reliable for interdependent sources), but translate
# javac's own verbose "[parsing started <file>]" lines into a pip-style in-place
# progress bar. Everything else (warnings/errors/notes) is buffered and only shown
# if the build fails. javac writes -verbose output to STDERR, so we merge with 2>&1.
# A trailing sentinel line carries javac's real exit code back out of the process
# substitution, so failures are detected reliably. "lib/*" is expanded by the JVM,
# not the shell, so the classpath is identical on Windows/Linux/macOS.
echo '==> Compiling...'
done=0
messages=()
javac_status=0
while IFS= read -r line; do
    if [[ "$line" == '__JAVAC_STATUS__:'* ]]; then
        javac_status="${line#__JAVAC_STATUS__:}"
    elif [[ "$line" == '[parsing started'* ]]; then
        done=$((done + 1))
        pct=$(( total > 0 ? done * 100 / total : 100 ))
        (( pct > 100 )) && pct=100
        printf '\r  [%3d/%d] Compiling %3d%%' "$done" "$total" "$pct"
    elif [[ "$line" != '['* && -n "${line//[[:space:]]/}" ]]; then
        messages+=("$line")
    fi
done < <(javac -verbose -cp 'lib/*' -d build/classes "${sources[@]}" 2>&1; printf '__JAVAC_STATUS__:%s\n' "$?")
printf '\n'
if [ "$javac_status" -ne 0 ]; then
    echo 'BUILD FAILED during compilation' >&2
    if [ "${#messages[@]}" -gt 0 ]; then
        printf '    %s\n' "${messages[@]}" >&2
    fi
    exit "$javac_status"
fi
echo "    Compiled $total source file(s)"

echo '==> Copying resources...'
cp src/main/resources/plugin.yml build/classes/
cp src/main/resources/config.yml build/classes/
echo '    Copied plugin.yml, config.yml'

echo '==> Packaging JAR...'
jar -cf build/XpoTriad.jar -C build/classes .

kb=$(du -k build/XpoTriad.jar | cut -f1)
echo "    Created build/XpoTriad.jar (${kb} KB)"

echo 'BUILD SUCCESSFUL'

