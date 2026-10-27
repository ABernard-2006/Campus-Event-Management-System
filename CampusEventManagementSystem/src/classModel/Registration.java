package classModel;

import CEMS.Main;
import java.time.LocalDate;

/** Connects one participant (Student, Faculty or Others) to one event. */
public class Registration {
    public static final String REGISTERED = "Registered";
    public static final String CANCELLED = "Cancelled";

    private final int registrationId;
    private final Participant participant;
    private final Event event;
    private final LocalDate registrationDate;
    private String status;

    /** Creates a NEW registration with an automatically generated ID. */
    public Registration(Participant participant, Event event, LocalDate registrationDate, String status) {
        this(Main.generateRegistrationId(), participant, event, registrationDate, status);
    }

    /** Rebuilds a saved registration (keeps its saved ID). */
    public Registration(int registrationId, Participant participant, Event event,
                        LocalDate registrationDate, String status) {
        if (registrationId <= 0) throw new IllegalArgumentException("Registration ID must be a positive number.");
        if (participant == null || event == null)
            throw new IllegalArgumentException("Participant and event are required.");
        String s = status == null ? REGISTERED : status.trim();
        if (!REGISTERED.equals(s) && !CANCELLED.equals(s))
            throw new IllegalArgumentException("Unknown registration status: " + status);
        this.registrationId = registrationId;
        Main.syncRegistrationId(registrationId);
        this.participant = participant;
        this.event = event;
        this.registrationDate = registrationDate == null ? LocalDate.now() : registrationDate;
        this.status = s;
    }

    public int getRegistrationId() { return registrationId; }
    public Participant getParticipant() { return participant; }
    public Event getEvent() { return event; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public String getStatus() { return status; }
    public boolean isActive() { return REGISTERED.equals(status); }
    public void cancel() { this.status = CANCELLED; }

    /** Used only to undo a cancel when saving the change to the file fails. */
    public void reactivate() { this.status = REGISTERED; }

    @Override public String toString() { return participant.getFullName() + " -> " + event.getEventName(); }
}
