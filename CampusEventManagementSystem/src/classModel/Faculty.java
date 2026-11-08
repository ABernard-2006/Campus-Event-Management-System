package classModel;

public class Faculty extends Participant {
    private final String facultyNumber;
    private final String department;
    private final String position;

    public Faculty(String fullName, String contactInfo, String facultyNumber, String department, String position) {
        super(fullName, contactInfo);
        this.facultyNumber = requireText(facultyNumber, "Faculty ID");
        this.department = requireText(department, "Department");
        this.position = requireText(position, "Position");
    }

    /** Rebuilds a saved faculty member. */
    public Faculty(int participantId, String fullName, String contactInfo,
                   String facultyNumber, String department, String position) {
        super(participantId, fullName, contactInfo);
        this.facultyNumber = requireText(facultyNumber, "Faculty ID");
        this.department = requireText(department, "Department");
        this.position = requireText(position, "Position");
    }

    public String getFacultyNumber() { return facultyNumber; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }

    @Override
    public String displayDetails() {
        return "[FACULTY] " + getFullName() + " | Dept: " + department + " | Pos: " + position;
    }
}
