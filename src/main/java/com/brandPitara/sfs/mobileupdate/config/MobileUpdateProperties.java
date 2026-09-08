package com.brandPitara.sfs.mobileupdate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sfs.mobile-update")
public class MobileUpdateProperties {

    private boolean emergencyKillSwitchEnabled = false;

    public boolean isEmergencyKillSwitchEnabled() {
        return emergencyKillSwitchEnabled;
    }

    public void setEmergencyKillSwitchEnabled(boolean emergencyKillSwitchEnabled) {
        this.emergencyKillSwitchEnabled = emergencyKillSwitchEnabled;
    }
}

