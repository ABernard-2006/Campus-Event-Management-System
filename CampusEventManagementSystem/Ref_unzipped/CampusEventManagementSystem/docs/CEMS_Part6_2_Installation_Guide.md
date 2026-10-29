# CEMS Part 6.2 — Installation Guide (macOS + NetBeans)

This guide replaces your current project with the final Part 6.2 project without losing your saved data.
Take your time; every step can be undone because you make a backup first.

You will receive one file: **`CEMS_GUI_Separation_Refactor_Final.zip`**. It contains the complete project.

---

## 1. Back up your current project (do not skip)
1. In NetBeans, close the project: right-click **CampusEventManagementSystem** in the *Projects* panel → **Close**.
2. Open **Finder** and go to the folder that holds your project (often `~/NetBeansProjects`).
3. Right-click the folder **CampusEventManagementSystem** → **Compress "CampusEventManagementSystem"**. This creates a zip next to it.
4. Rename that zip to `CampusEventManagementSystem_BACKUP_before_6_2.zip` and keep it somewhere safe (for example your Desktop).

## 2. Keep your saved data safe
Your events, accounts, venues, registrations and attendance live in the project's **`data`** folder (for example `accounts.txt`, `events.txt`, `venues.txt`, `participants.txt`, `registrations.txt`, `attendance.txt`, `organizer_keys.txt`, `login-preferences.properties`). Which files exist depends on what you have used so far; copy the whole folder.
1. In Finder, open your *current* project folder and copy the whole **`data`** folder to your Desktop as `data_COPY`.
2. The Part 6.2 files **do not change the data format**, so your copied data works as is.

## 3. Unzip the final project
1. Double-click **`CEMS_GUI_Separation_Refactor_Final.zip`**. A folder **CampusEventManagementSystem** appears.
2. Move it to a **new** location, for example `~/NetBeansProjects/CEMS_Part6_2` (create that folder first). Do **not** unzip on top of your old project.

## 4. Bring your data across
1. In the **new** project folder, open `data`.
2. Drag your files from `Desktop/data_COPY` into it and choose **Replace** when asked.
   (The zip ships only `accounts.txt`, `events.txt` and `venues.txt` as sample data. Skip this step only if you are happy to start from those.)

## 5. Open, Clean and Build
1. NetBeans → **File → Open Project…** → pick the new `CampusEventManagementSystem` folder → **Open Project**.
2. If NetBeans shows "Resolve Reference Problems", the project uses `dist/lib/AbsoluteLayout.jar`, which is included in the zip. Choose **Resolve…** and select that jar if asked.
3. Right-click the project → **Clean and Build**. Wait for **BUILD SUCCESSFUL** in the Output panel.
   - The project is set to **Java 25** (`nbproject/project.properties`). If your NetBeans uses an older JDK and complains, open **Project Properties → Sources → Source/Binary Format** and pick the JDK you have installed (the code compiles on JDK 21). Tell me if you have to do this.
4. Right-click the project → **Run** (or press the green ▶). The login window should appear.

## 6. Test the application (checklist)
The automated tests cannot open windows, so please click through this list once. Tick each line.

**Login and sign-up**
- [ ] Wrong password shows an error; five wrong attempts disable the Login button for about 15 seconds.
- [ ] Empty username / empty password shows a message and puts the cursor in that box.
- [ ] Log in as Administrator, as Organizer, and as Participant (each with its own account).
- [ ] "Remember me" ticked → close and reopen the app: username and role are filled in. Unticked → they are not.
- [ ] Register a new Participant. Registering with an existing email is refused.
- [ ] Register an Organizer with a valid invitation key (see Organizer Keys below).

**Dashboard (all roles)**
- [ ] Home, Events and Register buttons work; the search box (button **and** pressing Enter) filters events.
- [ ] Clicking a day on the calendar lists that day's events; "View all" lists everything.
- [ ] Participant: no *Manage Event*, *Reports* or *Organizer Keys*. Organizer: Manage + Reports but no Organizer Keys. Administrator: all of them.
- [ ] Logout asks for confirmation and returns to the login window.

**Events (Administrator / Organizer)**
- [ ] Create a Workshop and a Seminar (custom duration too). Overlapping events in the same venue are refused.
- [ ] Event List: Search, Show All, Edit (double-click works), Save Changes, Delete (confirmation, then gone).
- [ ] After any change, the Home cards, calendar dots and Upcoming list update by themselves.

**Registration and attendance**
- [ ] Participant registers once; a second try is refused; Cancel works only on their own registration.
- [ ] Staff register a walk-in and cancel any registration.
- [ ] Attendance tab: choose an event, select rows, Mark Present / Mark Absent.

**Reports (Administrator / Organizer)**
- [ ] Tiles (Total Registration, Attended, Attendance Rate) match the table.
- [ ] Event, Type and Date-range filters change the table; start date after end date shows a warning.
- [ ] Double-click a row to see the event details.

**Organizer Keys (Administrator)**
- [ ] Sidebar button or profile menu opens the list. *Generate New Key…* shows the key once (Copy works).
- [ ] *Revoke Selected* is enabled only for an Active key and asks for confirmation.

**NetBeans Design view**
- [ ] Open `Dashboard.form` and `ReportPanel.form` (Design tab). They should load without errors. Select a button → **Properties → Events → actionPerformed** to see its handler name.
- [ ] Do not "Save" the forms unless you changed them; if you do, NetBeans may reformat generated code (that is normal).

## 7. Run the automated tests (optional)
Open **Terminal**, then:
```
cd ~/NetBeansProjects/CEMS_Part6_2/CampusEventManagementSystem
bash test/run_tests.sh
```
You need a JDK (`javac`) and `python3` (macOS Terminal offers to install the developer tools if it is missing).
You should end with **ALL TEST RUNS FINISHED OK**. If you only have a Java runtime, see the first lines of `test/run_tests.sh`.

## 8. If something goes wrong
- **Build fails:** copy the first red error from the Output panel and send it to me.
- **Login not working after copying data:** make sure `accounts.txt` came from your old `data` folder and you did not mix two copies.
- **Go back:** close the new project, unzip `CampusEventManagementSystem_BACKUP_before_6_2.zip`, and open that instead. Your `data_COPY` is also a safe copy of your data.
- Note: `build/` and `dist/CampusEventManagementSystem.jar` are not shipped; *Clean and Build* creates them.
