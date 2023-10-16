package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.lights.StatusLightController;
import com.mayer99.lights.enums.StatusLight;
import com.mayer99.lights.enums.StatusLightColor;
import com.mayer99.lights.models.StatusLightAnimation;
import com.mayer99.narcotrack.base.events.CurrentAssessmentEvent;
import com.mayer99.narcotrack.base.events.ElectrodeCheckEvent;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ElectrodeDisconnectedListener implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectrodeDisconnectedListener.class);

    private final StatusLightController statusLights;

    public ElectrodeDisconnectedListener(Narcotrack narcotrack) {
        statusLights = narcotrack.getStatusLights();
    }

    @Override
    public void onCurrentAssessmentEvent(CurrentAssessmentEvent event) {

        List<Float> relativeBandActivities = Arrays.asList(
                event.getData().getAlphaRel1(),
                event.getData().getAlphaRel2(),
                event.getData().getBetaRel1(),
                event.getData().getBetaRel2(),
                event.getData().getDeltaRel1(),
                event.getData().getDeltaRel2(),
                event.getData().getThetaRel1(),
                event.getData().getThetaRel2()
        );

        if (relativeBandActivities.stream().noneMatch(value -> value > 1.0f)) {
            LOGGER.warn("Received CurrentAssessment with all relative band activities below 1%");
        }
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
        if (impedances.values().stream().anyMatch(impedance -> impedance >= 45)) {
            statusLights.animate(new StatusLightAnimation(StatusLight.ELECTRODES, StatusLightColor.ERROR));
            impedances.forEach((name, impedance) -> {
                if (impedance >= 45) {
                    LOGGER.warn("Received ElectrodeCheck with loose {} Electrode (impedance: {})", name, impedance);
                }
            });
            return;
        }
        if (Math.abs(imp1a - impRef) > 3.0f || Math.abs(imp1b - impRef) > 3.0f || Math.abs(imp1a - imp1b) > 3.0f) {
            statusLights.animate(new StatusLightAnimation(StatusLight.ELECTRODES, StatusLightColor.WARNING));
            LOGGER.warn("Received ElectrodeCheck with impendance difference between two electrodes");
            return;
        }
        if (impedances.values().stream().anyMatch(impedance -> impedance >= 5.0f)) {
            statusLights.animate(new StatusLightAnimation(StatusLight.ELECTRODES, StatusLightColor.WARNING));
            LOGGER.warn("Received ElectrodeCheck with high impedance of at least one electrode");
            return;
        }
        statusLights.animate(new StatusLightAnimation(StatusLight.ELECTRODES, StatusLightColor.INFO));
    }
}
