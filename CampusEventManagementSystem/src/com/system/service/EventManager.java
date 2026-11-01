package com.system.service;

import java.util.List;

/**
 * The application's service entry point and composition root. It creates the one shared {@link DataRepository}
 * (which loads every saved file), builds each service with exactly the collaborators it needs, and hands the
 * services to the screens. It contains NO business rules of its own.
 *
 * Screens receive one EventManager (so one set of services and one copy of the data is shared by the whole
 * application) and take from it the services they need:
 * <pre>
 *   manager.getAuthenticationService()   login, sign-up, remember me
 *   manager.getOrganizerKeyService()     organizer invitation keys (administrator only)
 *   manager.getAccessControl()           who may do what (for showing/hiding buttons)
 *   manager.getEventService()            create / edit / delete events
 *   manager.getEventQueryService()       search, lists, ordering, event dates
 *   manager.getVenueService()            venues
 *   manager.getRegistrationService()     participants, registering, cancelling, registration lists
 *   manager.getAttendanceService()       marking and counting attendance
 *   manager.getReportService()           report rows, totals and text
 *   manager.getOrganizerService()        organizations and organizer profiles
 *   manager.getChangeNotifier()          "something was saved" notifications for screens
 * </pre>
 * The dependency order below is the whole wiring: nothing depends on anything created after it.
 */
public class EventManager {
    private final DataRepository data;
    private final ChangeNotifier changeNotifier;
    private final AccessControl accessControl;
    private final OrganizerKeyService organizerKeyService;
    private final AuthenticationService authenticationService;
    private final VenueService venueService;
    private final RegistrationService registrationService;
    private final AttendanceService attendanceService;
    private final EventService eventService;
    private final EventQueryService eventQueryService;
    private final OrganizerService organizerService;
    private final ReportService reportService;

    /** Uses the default "data" folder. */
    public EventManager() {
        this(new DataStore());
    }

    public EventManager(DataStore store) {
        if (store == null) throw new IllegalArgumentException("A data store is required.");
        data = new DataRepository(store);
        changeNotifier = new ChangeNotifier();
        accessControl = new AccessControl(data);
        organizerKeyService = new OrganizerKeyService(data, accessControl);
        authenticationService = new AuthenticationService(data, organizerKeyService);
        venueService = new VenueService(data);
        registrationService = new RegistrationService(data, accessControl, changeNotifier);
        attendanceService = new AttendanceService(data, accessControl, changeNotifier);
        eventService = new EventService(data, accessControl, changeNotifier, venueService, registrationService);
        eventQueryService = new EventQueryService(data);
        organizerService = new OrganizerService(data);
        reportService = new ReportService(data, accessControl, registrationService, attendanceService);
        authenticationService.createDefaultAdministratorIfFirstRun();
    }

    public AuthenticationService getAuthenticationService() { return authenticationService; }
    public OrganizerKeyService getOrganizerKeyService() { return organizerKeyService; }
    public AccessControl getAccessControl() { return accessControl; }
    public EventService getEventService() { return eventService; }
    public EventQueryService getEventQueryService() { return eventQueryService; }
    public VenueService getVenueService() { return venueService; }
    public RegistrationService getRegistrationService() { return registrationService; }
    public AttendanceService getAttendanceService() { return attendanceService; }
    public ReportService getReportService() { return reportService; }
    public OrganizerService getOrganizerService() { return organizerService; }
    public ChangeNotifier getChangeNotifier() { return changeNotifier; }

    /** Absolute path of the folder the data files are in (shown in the start-up warning). */
    public String getDataDirectory() { return data.directoryText(); }

    /** Problems found while loading (skipped lines). Empty when everything loaded cleanly. */
    public List<String> getLoadWarnings() { return data.loadWarnings(); }
}
