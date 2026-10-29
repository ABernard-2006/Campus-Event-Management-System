# CEMS Part 6.1 — Handoff to Part 6.2

Project: NetBeans Java Swing app, packages `CEMS`, `classModel`, `com.system.service`, `com.system.gui`.
Everything below was actually implemented and compiled; "tested" is stated explicitly per item (see Test_Results.md).

## 1. Original structure (as uploaded)
- `classModel/`: Event (abstract), Workshop, Seminar, Venue, Participant, Student, Faculty, Others, Registration,
  Attendance, UserAccount, OrganizerKey, Organization, Organizer.
- `com.system.service/`: **EventManager** (1338 lines; all business rules, actor-based authorization, rollback on
  storage failure, change listeners), DataStore (text-file IO), LoginPreferences, EventReportRow, StorageException.
- `com.system.gui/`: LoginFrame, SignInFrame, Dashboard, EventPanel, RegistrationPanel, ReportPanel (all with `.form`),
  plus handwritten ManageEventsPanel, EventListPanel, AttendancePanel, GenerateKeyDialog, OrganizerKeysDialog,
  CalendarPanel, UpcomingEventsPanel, EventCardPanel(.form), ProfileHeaderPanel, SearchBarPanel, Refreshable.
- `CEMS/Main.java` (entry point, ID counters). `data/` (accounts, events, venues). `test/` was empty.

## 2. Architectural inventory (Task A)
| GUI class / handler | Where logic was | Service used | Status |
|---|---|---|---|
| LoginFrame.btnLogin | already delegated to `authenticate` | `authenticate` | **6.1 done**: added immediate validation+focus. Lockout counter stays in GUI |
| SignInFrame.btnSignIn | already delegated | `registerAccount` | **6.1 done**: added immediate validation+focus |
| EventPanel create/save (was lambda->`saveEvent`) | ALL rules + venue resolution + object construction in GUI | `createEvent` / `updateEvent` (new) | **6.1 done** |
| EventPanel add/remove speaker, clear | GUI form state only | none needed | **6.1 done** (now generated handlers) |
| RegistrationPanel register | field rules, participant lookup/creation in GUI | `registerForEvent` (new) | **6.1 done** |
| RegistrationPanel search/table, open-event list | filter in GUI | `searchVisibleRegistrations` (new), `getUpcomingEvents(0)` | **6.1 done** |
| RegistrationPanel cancel | delegated already | `cancelRegistration` | **6.1 done** (named handler; hand-added button, not in .form) |
| AttendancePanel Present/Absent | delegated already | `markAttendance` | **6.1 done** (named handlers; sorting via `getEventsChronological`) |
| EventListPanel search/sort, delete | sort in GUI; delete delegated | `searchEventsChronological`, `deleteEvent` | sort **done**; buttons still lambdas -> **6.2** |
| Dashboard | role checks duplicated (`isAdministrator/isOrganizer`), `sortForDisplay`, nav lambdas | `isStaff` exists | **6.2** |
| ReportPanel | event sort, date-range min/max, totals | `buildReport`, `attendanceRate` exist | **6.2** |
| GenerateKeyDialog / OrganizerKeysDialog | already delegated | `generateOrganizerKey`, `getOrganizerKeys`, `revokeOrganizerKey` | lambdas -> named handlers in **6.2** |
| Calendar/UpcomingEvents/EventCard/ProfileHeader/ManageEvents | display only | read methods | audit in **6.2** |

## 3. Changes implemented
### EventManager (new public API; all in `com.system.service.EventManager`)
```java
public static final int MAX_EVENT_NAME_LENGTH = 100, MAX_VENUE_NAME_LENGTH = 60;
public static final String TYPE_WORKSHOP = "Workshop", TYPE_SEMINAR = "Seminar";
public static final String PARTICIPANT_STUDENT = "Student", PARTICIPANT_FACULTY = "Faculty", PARTICIPANT_OTHERS = "Others";

Event createEvent(UserAccount actor, String type, String name, LocalDate date, LocalTime time, int durationMinutes,
                  String venueName, int capacity, String speakers, String topicOrSubject, String materials)
void  updateEvent(UserAccount actor, Event event, String name, LocalDate date, LocalTime time, int durationMinutes,
                  String venueName, int capacity, String speakers, String topicOrSubject, String materials)   // overload
Registration registerForEvent(UserAccount actor, Event event, String type, String name, String contact,
                  String number, String extra, String extra2)
List<Registration> searchVisibleRegistrations(UserAccount actor, String keyword)
List<Event> getEventsChronological()                       // copy, date then start time
List<Event> searchEventsChronological(String keyword)
```
Private helpers: `validateEventFields(...)`, `venueFor(name, capacity)`.
Rules: staff only for create/update; past date rejected on create and when the date changes on edit; capacity>0;
name<=100, venue<=60; speakers/topic/materials required (materials only for Workshop); same venue name must keep its
capacity; schedule conflict is checked **before** the Event is built so failures do not burn event IDs;
`registerForEvent`: a Participant always registers its own profile (contact forced to account email; existing profile
is locked and other fields ignored); staff reuse a saved participant by Student/Faculty number (Others by name).
Old methods `addEvent(actor, Event)`, `updateEvent(..., Venue, ...)`, `register(actor, Participant, Event)` are **kept**
and unchanged (note: `addEvent` itself does not enforce the new field rules; only the new `createEvent` does).

### GUI
- **EventPanel**: handlers `btnCerateEventActionPerformed` (create AND save-changes), `btnEventClearActionPerformed`,
  `btnAddSpeakerActionPerformed`, `btnRemoveSpeakerActionPerformed`; `txtEventSpeakerActionPerformed` now calls the
  add-speaker handler. `.form` updated with 4 new `EventHandler` entries; matching `addActionListener(this::...)` lines
  added inside `initComponents()` (only change made there). `parseDuration` (static, package-private) kept.
- **RegistrationPanel**: handlers `btnRegisterActionPerformed`, `btnRegClearActionPerformed`, `btnSearchRegActionPerformed`,
  `txtSearchRegActionPerformed` (+ `.form` entries and listener lines); `btnCancelRegActionPerformed` for the hand-added
  Cancel button; helper `warn(...)`.
- **LoginFrame / SignInFrame**: early validation in the existing generated handlers; SignInFrame got private `warn(msg, component)`.
- **AttendancePanel**: `btnPresentActionPerformed`, `btnAbsentActionPerformed`; buttons became fields.
- **EventListPanel**: `refresh()` uses `searchEventsChronological`.

## 4. Removed / replaced
EventPanel: `saveEvent`, `addSpeaker`, `removeSpeaker`, `clearOrCancel`. RegistrationPanel: `registerParticipant`,
`cancelSelected`; loadEvents/loadTable now use the service. Unused imports removed. No GUI class now constructs
Workshop/Seminar/Venue/Student/Faculty/Others/Registration or touches files (grep-verified).

## 5. Handlers still awaiting refactor (6.2)
Dashboard (sidebar lambdas, role booleans, `sortForDisplay`, search/day-click flow), ReportPanel (combo/spinner/generate
lambdas, event sort, min/max range, totals), EventListPanel (search/show-all/edit/delete lambdas), GenerateKeyDialog and
OrganizerKeysDialog lambdas, ManageEventsPanel wiring, plus display-panel audit. Also LoginFrame lockout (decision: keep or move).

## 6. Compilation and tests
Compiles (JDK 21 compiler module; project declares Java 25 — not tried). 137 service checks + 38 headless wiring checks
pass. No real window was ever shown; LoginFrame/SignInFrame changes are compile-checked only. Details: `CEMS_Part6_1_Test_Results.md`.

## 7. Known issues
- **Pre-existing**: `Dashboard.form` declares `btnRegisterActionPerformed`; `Dashboard.java` has no such method (button wired
  by lambda in `buildSidebar`). Harmless now; NetBeans would add an empty stub when the form is saved. Fix in 6.2.
- Behaviour changes: error order in the event form differs slightly; Participant with locked profile skips GUI field checks.
- `build/` and `dist/CampusEventManagementSystem.jar` were NOT included (stale); use Clean and Build in NetBeans.
- Failed-attempt Venue objects are no longer created before conflicts, but a new `Venue` can still consume an ID if saving fails.

## 8. How to continue (Part 6.2)
1. Unzip, open in NetBeans, Clean and Build. Run `bash test/run_tests.sh` (set `JAVAC` if needed); expect 137 + 38 passes.
2. Manually run the GUI first: login, sign-up (Participant and Organizer with key), create/edit event, venue conflict,
   register/cancel as Participant and staff, attendance, reports. Report any regression from 6.1 before changing more.
3. Work through section 5. For each handler: read fields -> immediate checks -> service -> result -> refresh. Add the
   `.form` `EventHandler` and the `this::handler` line inside `initComponents()` together; remove any lambda (no duplicates).
   Extend `Part61ServiceTests`/`GuiWiringSmokeTest` for new service methods.
4. Do not weaken `requireStaff`/ownership checks; keep `data/` file formats unchanged.
