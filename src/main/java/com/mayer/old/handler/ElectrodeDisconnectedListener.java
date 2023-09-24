package com.mayer.old.handler;

import com.mayer.rework.Narcotrack;
import com.mayer.narcotrack.core.events.ElectrodeCheckEvent;
import com.mayer.rework.lights.enums.StatusLight;
import com.mayer.rework.lights.enums.StatusLightColor;
import com.mayer.rework.lights.StatusLights;
import com.mayer.rework.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ElectrodeDisconnectedListener implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectrodeDisconnectedListener.class);
    
    private final StatusLights statusLights;

    public ElectrodeDisconnectedListener(Narcotrack narcotrack) {
        statusLights = narcotrack.getStatusLights();
    }

    @Override
    public void onElectrodeCheckEvent(ElectrodeCheckEvent event) {
        float imp1a = event.getData().getImp1a();
        float impRef = event.getData().getImpRef();
        float imp1b = event.getData().getImp1b();
        HashMap<String, Float> impedances = new HashMap<>();
        impedances.put("imp1a", imp1a);
        impedances.put("imp1b", imp1b);
        impedances.put("impRef", impRef);
        StatusLightColor color = StatusLightColor.OFF;
        if (Math.abs(imp1a - impRef) > 3 || Math.abs(imp1b - impRef) > 3 || Math.abs(imp1a - imp1b) > 3) {
            LOGGER.warn("Received ElectrodeCheck with impendance difference between two electrodes");
            color = StatusLightColor.WARNING;
        }
        if (impedances.values().stream().anyMatch(impedance -> impedance >= 45)) {
            color = StatusLightColor.ERROR;
            impedances.forEach((name, impedance) -> {
                if (impedance >= 45) {
                    LOGGER.warn("Received ElectrodeCheck with loose {} Electrode (impedance: {})", name, impedance);
                }
            });
        }
        statusLights.setColorChangeAnimation(StatusLight.ELECTRODES, color);
    }
}
