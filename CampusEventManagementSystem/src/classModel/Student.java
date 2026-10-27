package classModel;

public class Student extends Participant {
    private final String studentNumber;
    private final String program;

    public Student(String fullName, String contactInfo, String studentNumber, String program) {
        super(fullName, contactInfo);
        this.studentNumber = requireText(studentNumber, "Student number");
        this.program = requireText(program, "Program");
    }

    /** Rebuilds a saved student. */
    public Student(int participantId, String fullName, String contactInfo, String studentNumber, String program) {
        super(participantId, fullName, contactInfo);
        this.studentNumber = requireText(studentNumber, "Student number");
        this.program = requireText(program, "Program");
    }

    public String getStudentNumber() { return studentNumber; }
    public String getProgram() { return program; }

    @Override
    public String displayDetails() {
        return "[STUDENT] " + getFullName() + " | SN: " + studentNumber + " | Program: " + program;
    }
}
