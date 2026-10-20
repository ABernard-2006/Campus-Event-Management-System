/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package classModel;

/**
 *
 * @author bernard_07
 */

public class Others extends Participant 
{
    private String orgAffiliation;

    public Others(String fullName, String contactInfo, String orgAffiliation) 
    {
        super(fullName, contactInfo);
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
