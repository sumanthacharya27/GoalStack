package com.goalstack.dto;

/** JSON body for changing a subgoal's status, for example {"status": "COMPLETED"}. */
public class StatusRequest {

    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
