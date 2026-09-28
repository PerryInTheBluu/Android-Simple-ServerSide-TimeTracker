#!/usr/bin/env fish

set -l REPO ~/Projekte/Android-Simple-ServerSide-TimeTracker
set -l PKG de.piusdischinger.timetracker.debug
set -l APK app/build/outputs/apk/base/debug/app-base-debug.apk
set -l BRANCH $argv[1]
set -g LOGDIR

function fail
    set -l message $argv[1]

    echo
    echo "========================================"
    echo "FEHLER: $message"

    if test -n "$LOGDIR"
        echo "Logs: $LOGDIR"
    end

    echo "========================================"
    exit 1
end

function collect_logs
    if test -z "$LOGDIR"
        return
    end

    mkdir -p $LOGDIR

    adb logcat -d -v threadtime -b main -b system -b crash > $LOGDIR/logcat-full.log 2>&1
    adb logcat -d -v long -b crash > $LOGDIR/crash-buffer.log 2>&1

    grep -n -E \
        'FATAL EXCEPTION|AndroidRuntime|Process:|Caused by:|Exception|Error:|IllegalArgumentException|Unresolved reference|error\.NonExistentClass|KSP|Hilt|Dagger' \
        $LOGDIR/logcat-full.log > $LOGDIR/logcat-filtered.log 2>/dev/null; or true

    if test -s $LOGDIR/logcat-filtered.log
        echo
        echo "----- Relevante Logcat-Zeilen -----"
        cat $LOGDIR/logcat-filtered.log
        echo "-----------------------------------"
    else
        echo
        echo "Keine offensichtlichen Crash-/Exception-Zeilen im Logcat gefunden."
    end
end

function run_gradle
    set -l task $argv[1]
    set -l safe_task (string replace -a ':' '_' $task)
    set -l logfile $LOGDIR/gradle$safe_task.log

    echo
    echo "==> Gradle: $task"

    ./gradlew $task 2>&1 | tee $logfile
    set -l gradle_status $pipestatus[1]

    if test $gradle_status -ne 0
        echo
        echo "----- Relevante Gradle-Zeilen -----"

        grep -n -E \
            'FAILED|FAILURE:|What went wrong|Execution failed|Compilation error|error:|e: |Unresolved reference|error\.NonExistentClass|KSP|Hilt|Dagger|Caused by:' \
            $logfile; or true

        echo "-----------------------------------"
        fail "Gradle-Task fehlgeschlagen: $task"
    end
end

if test (count $argv) -ne 1
    echo "Verwendung: fish scripts/test-pr-branch.fish <Remote-Branch>"
    echo "Beispiel:  fish scripts/test-pr-branch.fish vibe/fix-sync-offline-startup"
    exit 2
end

if not test -d $REPO/.git
    echo "Repository nicht gefunden: $REPO"
    exit 2
end

cd $REPO; or exit 2

set -l timestamp (date '+%Y%m%d-%H%M%S')
set -g LOGDIR $REPO/logs/pr-test-$timestamp-(string replace -a '/' '_' $BRANCH)

mkdir -p $LOGDIR; or fail "Log-Verzeichnis konnte nicht erstellt werden."

echo "Repository: $REPO"
echo "Remote-Branch: origin/$BRANCH"
echo "Logs: $LOGDIR"

echo
echo "==> Prüfe sauberen Arbeitsbaum"

if not git diff --quiet; or not git diff --cached --quiet
    fail "Lokale Änderungen vorhanden. Erst committen, stashen oder verwerfen."
end

echo
echo "==> Aktualisiere Remote-Referenzen"
git fetch origin --prune; or fail "git fetch --prune fehlgeschlagen."

if not git show-ref --verify --quiet refs/remotes/origin/$BRANCH
    fail "Remote-Branch existiert nicht: origin/$BRANCH"
end

echo
echo "==> Checke PR-Branch aus"
git switch --detach origin/$BRANCH; or fail "Checkout von origin/$BRANCH fehlgeschlagen."

echo
echo "==> Getesteter Commit"
git log -1 --oneline | tee $LOGDIR/commit.txt
git status --short | tee $LOGDIR/git-status.txt

run_gradle :data_sync:compileDebugKotlin
run_gradle :data_sync:testDebugUnitTest
run_gradle assembleBaseDebug

if not test -f $APK
    fail "APK wurde trotz erfolgreichen Builds nicht gefunden: $APK"
end

echo
echo "==> Verbundene ADB-Geräte"
adb devices | tee $LOGDIR/adb-devices.txt

set -l device_count (adb devices | string match -r '^[^\s]+\s+device$' | count)

if test $device_count -ne 1
    fail "Genau ein autorisiertes ADB-Gerät erwartet, gefunden: $device_count"
end

echo
echo "==> Installiere Debug-APK"
adb install -r $APK 2>&1 | tee $LOGDIR/adb-install.log

if test $pipestatus[1] -ne 0
    fail "APK-Installation fehlgeschlagen."
end

echo
echo "==> Bereinige Logcat und starte App kalt"
adb logcat -c; or fail "Logcat konnte nicht geleert werden."
adb shell am force-stop $PKG; or fail "App konnte nicht gestoppt werden."

adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 \
    2>&1 | tee $LOGDIR/adb-launch.log

if test $pipestatus[1] -ne 0
    collect_logs
    fail "App konnte nicht gestartet werden."
end

sleep 3

echo
echo "==> Prüfe App-Prozess"
adb shell pidof $PKG | tee $LOGDIR/pid.txt
set -l pid_status $pipestatus[1]

collect_logs

if test $pid_status -ne 0
    fail "App-Prozess läuft nach Kaltstart nicht."
end

if test -s $LOGDIR/crash-buffer.log
    echo
    echo "----- Crash-Buffer -----"
    cat $LOGDIR/crash-buffer.log
    echo "------------------------"
    fail "Crash-Buffer enthält Einträge nach dem Kaltstart."
end

echo
echo "========================================"
echo "ERFOLG: Build, Installation und Kaltstart bestanden."
echo "Getesteter Branch: $BRANCH"
echo "Getesteter Commit: "(git rev-parse --short HEAD)
echo "Logs: $LOGDIR"
echo "========================================"

echo
echo "Manueller Resttest:"
echo "1. App öffnen und Einstellungen aufrufen."
echo "2. Ohne Sync-URL prüfen: kein Crash, erwarteter Offline-Status."
echo "3. Sync manuell auslösen und prüfen: kontrollierter 'nicht konfiguriert'-Zustand."

