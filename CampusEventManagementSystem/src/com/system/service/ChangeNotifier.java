package com.system.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets screens redraw themselves after any saved change to events, registrations or attendance.
 * One instance is shared by the services that save such changes (EventService, RegistrationService,
 * AttendanceService); a screen registers once and hears about all of them.
 */
public class ChangeNotifier {
    private final List<Runnable> listeners = new ArrayList<>();

    ChangeNotifier() { }

    /** Registers something to run after every successful change. Adding the same listener twice has no effect. */
    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** Called by a service AFTER its change was saved. */
    void fireChanged() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();       // one broken screen must not stop the others or hide the saved change
            }
        }
    }
}
