/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */

public class Faculty extends Participant 
{
    private String facultyNumber;
    private String department;
    private String position;

    public Faculty(int participantId, String fullName, String contactInfo, String facultyNumber, String department, String position) 
    {
        super(participantId, fullName, contactInfo);
        this.facultyNumber = facultyNumber;
        this.department = department;
        this.position = position;
    }

    public String getFacultyNumber() 
    { 
        return facultyNumber; 
    }
    
    public String getDepartment() 
    { 
        return department; 
    }
    
    public String getPosition() 
    { 
        return position; 
    }

    @Override
    public String displayDetails() 
    {
        return "[FACULTY] " + getFullName() + " | Dept: " + department + " | Pos: " + position;
    }
}
