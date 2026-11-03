package com.goalstack.entity;

public enum SubgoalStatus {
    PENDING("Pending"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    private final String label;

    SubgoalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Converts text from a form into a status, rejecting anything unsupported. */
    public static SubgoalStatus parse(String value) {
        if (value != null) {
            for (SubgoalStatus status : values()) {
                if (status.name().equals(value.trim())) {
                    return status;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported status. Use PENDING, IN_PROGRESS or COMPLETED.");
    }
}
