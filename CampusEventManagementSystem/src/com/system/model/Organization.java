/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */
public class Organization 
{
    private int orgId;
    private String orgName;
    private String description;

    public Organization(int orgId, String orgName, String description) 
    {
        this.orgId = orgId;
        this.orgName = orgName;
        this.description = description;
    }

    public int getOrgId() 
    { 
        return orgId; 
    }
    
    public String getOrgName() 
    { 
        return orgName; 
    }
    
    public String getDescription() 
    { 
        return description; 
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }
}
