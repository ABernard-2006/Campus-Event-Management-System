/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

import CEMS.Main;
import java.time.LocalDate;
/**
 *
 * @author ASUS2
 */
public class Registration {
    public static final String REGISTERED = "Registered";
    public static final String CANCELLED = "Cancelled";

    private final int registrationId;
    private final Participant participant;
    private final Event event;
    private final LocalDate registrationDate;
    private String status;

    public Registration(Participant participant, Event event, LocalDate registrationDate, String status) {
        if (participant == null || event == null)
            throw new IllegalArgumentException("Participant and event are required.");
        this.registrationId = Main.generateId();
        this.participant = participant;
        this.event = event;
        this.registrationDate = registrationDate == null ? LocalDate.now() : registrationDate;
        this.status = status == null ? REGISTERED : status;
    }

    public int getRegistrationId() { return registrationId; }
    public Participant getParticipant() { return participant; }
    public Event getEvent() { return event; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public String getStatus() { return status; }
    public boolean isActive() { return REGISTERED.equals(status); }
    public void cancel() { this.status = CANCELLED; }

    @Override public String toString() { return participant.getFullName() + " -> " + event.getEventName(); }
}
