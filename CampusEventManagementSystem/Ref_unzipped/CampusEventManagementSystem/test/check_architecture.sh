#!/usr/bin/env bash
# Static layer checks. Run from the project root:  bash test/check_architecture.sh   (exit 1 on any violation)
fail=0
bad() { echo "VIOLATION: $1"; echo "$2" | sed 's/^/    /'; fail=1; }
ok()  { echo "ok: $1"; }
chk() { # description, pattern, files...
  local d="$1" p="$2"; shift 2
  local out; out=$(grep -nE "$p" "$@" 2>/dev/null | grep -v '^\S*:[0-9]*:\s*\(//\|\*\|/\*\)')
  if [ -n "$out" ]; then bad "$d" "$out"; else ok "$d"; fi
}
GUI=$(find src/com/system/gui -name '*.java'); SVC=$(find src/com/system/service -name '*.java'); MODEL=$(find src/classModel -name '*.java')
chk "services do not use Swing/AWT"            'import (javax\.swing|java\.awt)|javax\.swing\.|JOptionPane'  $SVC
chk "models do not use Swing/AWT"              'import (javax\.swing|java\.awt)|javax\.swing\.|JOptionPane'  $MODEL
chk "models do not call the service/GUI layers" 'com\.system\.(service|gui)'                                  $MODEL
chk "GUI does not read/write files"            'java\.io\.|java\.nio\.file|Files\.|FileReader|FileWriter|new File\('  $GUI
chk "GUI does not touch DataStore/LoginPreferences" 'DataStore|LoginPreferences'                               $GUI
chk "GUI does not build model/business objects" 'new (Workshop|Seminar|Venue|Student|Faculty|Others|Registration|Attendance|Participant|OrganizerKey|Organizer|Organization)\('  $GUI
# SignInFrame is excluded on purpose: it compares the chosen sign-up role only to show the invitation-key field and to
# give an immediate "key is empty" message. registerAccount enforces every real rule (no admin sign-up, key validity).
chk "GUI does not decide permissions from the role" '\.getRole\(\)\s*\.equals|ROLE_[A-Z]+\.equals\(|isAdministrator\(|isOrganizer\('  $(echo "$GUI" | grep -v SignInFrame)
chk "GUI does not sort events itself"          'Comparator\.|\.sort\('                                          $GUI
chk "GUI does not use the old unchecked entry points" 'manager\.(addEvent|addVenue|addParticipant|register)\('   $GUI
if [ $fail -ne 0 ]; then echo; echo "ARCHITECTURE CHECK FAILED"; exit 1; fi
echo; echo "ARCHITECTURE CHECK PASSED"
