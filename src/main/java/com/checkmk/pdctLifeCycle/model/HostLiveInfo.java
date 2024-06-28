package com.checkmk.pdctLifeCycle.model;

public class HostLiveInfo {

    private String hostName;
    private String hostState;
    private String ServiceOk;
    private String ServiceWarning;
    private String ServiceCritical;

    //Getters and Setters


    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostState() {
        return hostState;
    }

    public void setHostState(String hostState) {
        this.hostState = hostState;
    }

    public String getServiceOk() {
        return ServiceOk;
    }

    public void setServiceOk(String serviceOk) {
        ServiceOk = serviceOk;
    }

    public String getServiceWarning() {
        return ServiceWarning;
    }

    public void setServiceWarning(String serviceWarning) {
        ServiceWarning = serviceWarning;
    }

    public String getServiceCritical() {
        return ServiceCritical;
    }

    public void setServiceCritical(String serviceCritical) {
        ServiceCritical = serviceCritical;
    }
}
