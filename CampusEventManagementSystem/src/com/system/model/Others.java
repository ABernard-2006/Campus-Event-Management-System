/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */

public class Others extends Participant 
{
    private String orgAffiliation;

    public Others(int participantId, String fullName, String contactInfo, String orgAffiliation) 
    {
        super(participantId, fullName, contactInfo);
        this.orgAffiliation = orgAffiliation;
    }

    public String getOrgAffiliation() 
    { 
        return orgAffiliation; 
    }

    @Override
    public String displayDetails() 
    {
        return "[GUEST] " + getFullName() + " | Affiliation: " + orgAffiliation;
    }
}
