/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.system.model;

/**
 *
 * @author bernard_07
 */

public class Student extends Participant 
{
    private String studentNumber;
    private String program;

    public Student(int participantId, String fullName, String contactInfo, String studentNumber, String program) 
    {
        super(participantId, fullName, contactInfo);
        this.studentNumber = studentNumber;
        this.program = program;
    }

    public String getStudentNumber() 
    { 
        return studentNumber; 
    }
    
    public String getProgram() 
    { 
        return program; 
    }

    @Override
    public String displayDetails() 
    {
        return "[STUDENT] " + getFullName() + " | SN: " + studentNumber + " | Program: " + program;
    }
}
