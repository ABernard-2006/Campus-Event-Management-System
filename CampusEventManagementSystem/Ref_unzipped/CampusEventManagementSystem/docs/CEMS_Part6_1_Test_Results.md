# CEMS Part 6.1 — Test Results

## How they were run
`JAVAC="java -m jdk.compiler/com.sun.tools.javac.Main" bash test/run_tests.sh` from the project root
(JDK 21 compiler module; the sandbox has only a headless JRE, no `javac` binary, no JUnit, no network).
`project.properties` says Java 25; **nothing was compiled with Java 25**.

## 1. Compilation — PASSED
All `src/**/*.java` compiled with no errors (baseline upload also compiled before changes). Tests compiled too.

## 2. Automated service tests — 137 passed, 0 failed
`test/com/system/service/Part61ServiceTests.java` (plain `main()` harness, temp data folders; real `data/` only read).
Covers: `createEvent` (happy path, Admin/Organizer allowed, Participant/null/forged-admin refused, persistence,
change-listener firing); validation (name empty/null/>100/=100, venue empty/>60, bad/blank type, past/today date, null
date/time, capacity 0/negative, no speakers, workshop topic/materials, seminar subject, duration 0/1/1440/1441);
venue reuse by name and capacity-mismatch rejection; conflicts (overlap, back-to-back allowed, other venue allowed,
custom duration, across midnight) and that rejected attempts do not consume event IDs; `updateEvent` (fields, persistence,
authorization, past-date only when changed, conflict, rollback, duration, venue capacity, too-small venue, old past event
still editable); `registerForEvent` (staff walk-ins, participant reuse, duplicates, full venue, every field rule,
duplicate email, Participant forced to own email and locked profile, closed event); `searchVisibleRegistrations`;
cancel/attendance authorization (regression); sign-up/authenticate rules incl. Part 5 duplicate-email and invitation-key
rules (regression); chronological helpers; shipped `data/` loads with 0 warnings, duration 90 kept, and `createEvent`
works on top of it.
**Mutation check:** three service rules were deliberately broken in a scratch copy; the suite failed on each.

## 3. Headless GUI wiring smoke test — 38 passed, 0 failed
`test/com/system/gui/GuiWiringSmokeTest.java`. Builds the JPanels headless and clicks real buttons with `doClick()`.
Verifies: exactly one ActionListener per refactored button/field (no duplicates); Create/Save/Add Speaker/Register/
Search/Mark Present/Mark Absent change service state exactly once; bad input is stopped (GUI-level or service-level);
Participant is refused by the service; edit mode saves instead of creating.

## 4. Static NetBeans consistency check
All `.form` files parse as XML. For EventPanel, RegistrationPanel, LoginFrame, SignInFrame: every form handler has one
generated listener line, one `GEN-FIRST/LAST` method, no handwritten duplicate. **Dashboard.form: pre-existing mismatch**
(`btnRegisterActionPerformed` in the form, absent from Dashboard.java; files identical to upload).

## NOT tested (manual GUI testing still required)
- No window was ever shown (JRE has no windowing toolkit). **LoginFrame and SignInFrame are JFrames, so they could not
  even be constructed; their edits are compile-checked only.**
- Dialogs cannot display: `HeadlessException` was treated as "handler reached a dialog". Dialog text, focus-on-field,
  layout, look and feel, NetBeans Design view loading: untested.
- Cancel-registration click path stops at the confirm dialog headless; only the service side is tested.
- In the smoke test `refresh()` after a success dialog is called explicitly (the dialog throws first headless).
- Dashboard, Reports, Calendar, key dialogs: not modified, not retested beyond compiling.

## Raw output of the final run
    == 1. compile project sources
    == 2. compile tests
    == 3. service tests (uses temp folders; the real data/ folder is only read, never written)
    === 137 passed, 0 failed ===
    == 4. headless GUI wiring smoke test (no windows are shown; see the header of the class for its limits)
    === 38 passed, 0 failed ===
