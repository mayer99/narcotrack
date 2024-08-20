package com.mayer99.narcotrack.events.handlers;

import com.mayer99.narcotrack.application.NarcotrackApplication;
import com.mayer99.narcotrack.events.NarcotrackEventHandler;
import com.mayer99.narcotrack.events.NarcotrackEventManager;
import com.mayer99.narcotrack.events.ReceivedCurrentAssessmentEvent;
import com.mayer99.narcotrack.events.ReceivedElectrodeCheckEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BadElectrodeListener implements NarcotrackEventHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(BadElectrodeListener.class);
    private final NarcotrackEventManager eventManager;

    public BadElectrodeListener(NarcotrackApplication application) {
        eventManager = application.getEventManager();
    }

    @Override
    public void onReceivedCurrentAssessment(ReceivedCurrentAssessmentEvent event) {
        List<Float> relativeBandActivities = Arrays.asList(
                event.getAlphaRel1(),
                event.getAlphaRel2(),
                event.getBetaRel1(),
                event.getBetaRel2(),
                event.getDeltaRel1(),
                event.getDeltaRel2(),
                event.getThetaRel1(),
                event.getThetaRel2()
        );
        if (relativeBandActivities.stream().noneMatch(value -> value > 1.0f)) {
            LOGGER.warn("Received CurrentAssessment with all relative band activities below 1%");
        }
    }

    @Override
    public void onReceivedElectrodeCheck(ReceivedElectrodeCheckEvent event) {
        float imp1a = event.getImp1a();
        float impRef = event.getImpRef();
        float imp1b = event.getImp1b();
        HashMap<String, Float> impedances = new HashMap<>();
        impedances.put("imp1a", imp1a);
        impedances.put("imp1b", imp1b);
        impedances.put("impRef", impRef);
        if (impedances.values().stream().anyMatch(impedance -> impedance >= 45.0f)) {
            String detachedElectrodes = impedances.entrySet().stream().filter(entry -> entry.getValue() >= 45.0f).map(Map.Entry::getKey).collect(Collectors.joining(", "));
            LOGGER.warn("Received ElectrodeCheck with detached electrodes: {}", detachedElectrodes);
            eventManager.dispatchOnDetachedElectrode();
            return;
        }
        if (Math.abs(imp1a - impRef) > 3.0f || Math.abs(imp1b - impRef) > 3.0f || Math.abs(imp1a - imp1b) > 3.0f || impedances.values().stream().anyMatch(impedance -> impedance >= 5.0f)) {
            LOGGER.warn("Received ElectrodeCheck with loose electrodes (impedance differs by more than 3 or is greater than 5)");
            eventManager.dispatchOnLooseElectrode();
            return;
        }
        LOGGER.debug("Received EletrodeCheck without impedance issues in any electrode");
        eventManager.dispatchOnGoodElectrodes();
    }
}
