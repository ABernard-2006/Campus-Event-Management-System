# CEMS Part 6.2 — Test Results

Everything below was actually executed in this session. Nothing is reported that was not run.

## How to reproduce
From the project root (needs JDK; if only a JRE exists use the `JAVAC` line; `python3` is needed for step 6):
```
JAVAC="java -m jdk.compiler/com.sun.tools.javac.Main" bash test/run_tests.sh      # JRE-only machine
bash test/run_tests.sh                                                             # machine with javac
```
Environment used: OpenJDK 21.0.10 (compiler module only, no `javac` binary), headless JRE, no JUnit, no network.
`nbproject/project.properties` says Java 25. **Nothing was compiled with Java 25.**

## 1. Part 6.1 baseline (run first, on the unmodified 6.1 files)
137 service passed / 0 failed; 38 GUI-wiring passed / 0 failed. Same as the 6.1 report.

## 2. Final run (all steps of `test/run_tests.sh`)
| Step | Result |
|---|---|
| 1. Compile all `src/**/*.java` | OK, no errors |
| 2. Compile all tests | OK |
| 3. Part 6.1 service tests (`Part61ServiceTests`) | **137 passed, 0 failed** (regression: authentication, sign-up/keys, event create/update, duration, venue conflicts, registration, cancellation, attendance, authorization, persistence, shipped `data/` loads) |
| 4. Part 6.2 service tests (`Part62ServiceTests`, new) | **25 passed, 0 failed** |
| 5a. Headless wiring, 6.1 panels (`GuiWiringSmokeTest`) | **38 passed, 0 failed** (EventPanel, RegistrationPanel, AttendancePanel, EventListPanel, unchanged and still green after 6.2 edits) |
| 5b. Headless wiring, 6.2 panels (`Part62GuiWiringTest`, new) | **29 passed, 0 failed** |
| 6. `.java` ↔ `.form` consistency (`test/check_forms.py`) | 7 forms checked, **0 problems** |
| 7. Layer rules (`test/check_architecture.sh`) | 9 rules, **all passed** |

Total automated checks that passed: 137 + 25 + 38 + 29 = 229, plus 7 forms and 9 architecture rules.

### What the new tests cover
- **Part62ServiceTests:** role queries for admin / organizer / participant / null / forged-unsaved admin, and agreement with what the service actually refuses; `orderForDisplay` (upcoming soonest first, past most-recent first, input not modified, empty input); `getEventDates` / `getEventDateRange` (empty and populated, copy semantics); `summarizeReport` (cancelled registrations excluded, attended count, 66.67 % rate, Workshop filter, agreement with row sums, empty = 0/0/0.0); remember-me (nothing at first, save, password never written, replace, forget, null account).
- **Part62GuiWiringTest** (real panels, real buttons via `doClick`/`setSelectedItem`/`setValue`/`postActionEvent`):
  - *ReportPanel:* exactly one listener per Generate button, both combos and both spinners (the spinner's own editor listener is excluded from the count); rows are soonest-first; tiles equal the service totals; default date range equals the service range; Type filter, Event filter and date filter each change the table through the service; a user-picked range survives a data refresh; start-after-end reaches the warning dialog; Generate re-runs.
  - *EventListPanel:* exactly one listener per button/field; Search and Enter filter via the service; Show All resets; Edit passes the selected event (and nothing when no row); Delete reaches its confirm dialog and deletes nothing until confirmed.
  - *AttendancePanel:* one listener on the event combo; changing the event reloads that event's registrations.
- **Mutation check (scratch copy):** three of the new rules were deliberately broken (organizer allowed to manage keys; totals ignoring a count; past events sorted before upcoming). The suite failed on each, and the real source was restored.
- **Negative test of the form checker:** run against the untouched 6.1 files it reports the known `Dashboard.btnRegister` problem (3 findings), then reports 0 on the final files.
- **Data compatibility:** `data/` is byte-identical to the 6.1 project (`diff -r` clean). Tests only write to temp folders.

## 3. Wiring check of every refactored handler
Verified by the form checker (text-level, all 7 forms) and the headless tests (behaviour, JPanels only):
- Dashboard (8 form handlers) and ReportPanel (5 form handlers): each form `EventHandler` has exactly one listener line in `initComponents()` and exactly one `//GEN-FIRST` method; no handwritten duplicate listener. Behaviour of ReportPanel's handlers is additionally exercised headless. Dashboard's is **not** (it is a JFrame).
- EventPanel (8), RegistrationPanel (4), LoginFrame (1), SignInFrame (1): same text-level result, unchanged by 6.2.

## 4. What the tests do NOT prove
- They do not prove any appearance, dialog text, focus behaviour, layout or NetBeans Design-view loading.
- JOptionPane dialogs cannot be shown here: they throw `HeadlessException`, which the tests treat as "the handler reached a dialog".

## 5. Manual testing still required (not run)
No AWT display library exists in the sandbox (`libawt_xawt` absent, so Xvfb could not help). These could not be constructed or shown and were **never run**:
- `LoginFrame`: login (all three roles, wrong password ×5 lockout, empty fields), Remember me on/off, link to Register.
- `SignInFrame`: Participant sign-up, Organizer sign-up with a key, duplicate username/email, password mismatch.
- `Dashboard`: every sidebar button, search box + button, calendar day click, "View all", Logout (confirm → login screen), role-based hiding (Participant sees no Manage/Reports; Organizer no Organizer Keys; Administrator sees all), event cards refresh after create/delete/register.
- `OrganizerKeysDialog` / `GenerateKeyDialog`: open as admin, generate (key shown once, Copy), list refresh, Revoke enabled only for an active key, Revoke confirm.
- Reports: visual check of tiles, table and double-click details.
- Open `Dashboard.form` and `ReportPanel.form` in NetBeans Design view and confirm they load, and that "Events" in the component properties shows the new handlers.
A checklist is in the Installation Guide §6.
