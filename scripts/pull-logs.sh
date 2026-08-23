#!/bin/bash
# Pull debug logs and telemetry from the app on a connected Android device
# Usage: ./scripts/pull-logs.sh [output_dir]
# Default output: ./logs/

set -e

OUTPUT_DIR="${1:-./logs}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
PKG="com.app.traveldocs"

echo "=== Document Manager — Pull Logs ==="
echo "Output: $OUTPUT_DIR"
echo ""

# Check device
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No Android device connected."
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# 1. Pull debug log file
echo "[1/4] Pulling debug log..."
adb shell run-as $PKG cat files/debug_logs/traveldocs_debug.log > "$OUTPUT_DIR/debug_${TIMESTAMP}.log" 2>/dev/null && \
    echo "  → $OUTPUT_DIR/debug_${TIMESTAMP}.log ($(wc -c < "$OUTPUT_DIR/debug_${TIMESTAMP}.log") bytes)" || \
    echo "  → No debug log found (app may not have run yet)"

# 2. Pull crash report (if any)
echo "[2/4] Pulling crash report..."
adb shell run-as $PKG cat files/last_crash_report.txt > "$OUTPUT_DIR/crash_${TIMESTAMP}.txt" 2>/dev/null && \
    echo "  → $OUTPUT_DIR/crash_${TIMESTAMP}.txt" || \
    echo "  → No crash report (good!)"

# 3. Pull logcat (filtered to our app)
echo "[3/4] Pulling logcat (app-filtered)..."
adb logcat -d -s TravelDocs > "$OUTPUT_DIR/logcat_${TIMESTAMP}.log"
LINES=$(wc -l < "$OUTPUT_DIR/logcat_${TIMESTAMP}.log")
echo "  → $OUTPUT_DIR/logcat_${TIMESTAMP}.log ($LINES lines)"

# 4. Pull ANR traces (if any)
# ANR = "Application Not Responding" — Android shows this dialog when the main thread
# is blocked for 5+ seconds (e.g., disk I/O, heavy computation on UI thread).
echo "[4/4] Checking for ANR traces..."
adb logcat -d | grep "ANR in $PKG" > "$OUTPUT_DIR/anr_${TIMESTAMP}.log" 2>/dev/null
ANR_COUNT=$(wc -l < "$OUTPUT_DIR/anr_${TIMESTAMP}.log")
if [ "$ANR_COUNT" -gt 0 ]; then
    echo "  → $OUTPUT_DIR/anr_${TIMESTAMP}.log ($ANR_COUNT ANR events found)"
else
    rm -f "$OUTPUT_DIR/anr_${TIMESTAMP}.log"
    echo "  → No ANRs found (good!)"
fi

echo ""
echo "=== Done ==="
echo "Files in $OUTPUT_DIR:"
ls -lh "$OUTPUT_DIR"/*_${TIMESTAMP}* 2>/dev/null || echo "  (none)"
