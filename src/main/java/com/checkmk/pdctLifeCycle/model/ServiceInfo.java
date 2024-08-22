package com.checkmk.pdctLifeCycle.model;


public class ServiceInfo {

    private String serviceState;
    private String serviceDescription;
    private String pluginOutput;

    // Getters and Setters
    public String getServiceState() {
        return serviceState;
    }

    public void setServiceState(String serviceState) {
        this.serviceState = serviceState;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getPluginOutput() {
        return pluginOutput;
    }

    public void setPluginOutput(String pluginOutput) {
        this.pluginOutput = pluginOutput;
    }
}
