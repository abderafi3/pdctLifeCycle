package com.checkmk.pdctLifeCycle.model;

public class HostWithLiveInfo {
    private Host host;
    private HostLiveInfo liveInfo;
    private LdapUser ldapUser; // New field for LdapUser

    // Getters and Setters
    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public HostLiveInfo getLiveInfo() {
        return liveInfo;
    }

    public void setLiveInfo(HostLiveInfo liveInfo) {
        this.liveInfo = liveInfo;
    }

}
