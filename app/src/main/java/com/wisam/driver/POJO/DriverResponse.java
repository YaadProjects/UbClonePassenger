package com.wisam.driver.POJO;

import com.google.gson.annotations.SerializedName;

/**
 * Created by islam on 11/17/16.
 */
public class DriverResponse {
    @SerializedName(value = "status")
    public int status;

    @SerializedName(value = "request_id")
    public String requestID;

    @SerializedName(value = "error_msg")
    public String errorMessage;

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
