package com.mayer99.narcotrack.handlers;

import com.mayer99.Narcotrack;
import com.mayer99.narcotrack.base.events.CurrentAssessmentEvent;
import com.mayer99.narcotrack.base.events.ElectrodeCheckEvent;
import com.mayer99.narcotrack.base.models.NarcotrackEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BadElectrodeListener implements NarcotrackEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BadElectrodeListener.class);


    public BadElectrodeListener() {
    }

    @Override
    public void onCurrentAssessment(CurrentAssessmentEvent event) {

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
            logger.warn("Received CurrentAssessment with all relative band activities below 1%");
        }
    }

    @Override
    public void onElectrodeCheck(ElectrodeCheckEvent event) {
        float imp1a = event.getData().getImp1a();
        float impRef = event.getData().getImpRef();
        float imp1b = event.getData().getImp1b();
        HashMap<String, Float> impedances = new HashMap<>();
        impedances.put("imp1a", imp1a);
        impedances.put("imp1b", imp1b);
        impedances.put("impRef", impRef);
        if (impedances.values().stream().anyMatch(impedance -> impedance >= 45.0f)) {
            String detachedElectrodes = impedances.entrySet().stream().filter(entry -> entry.getValue() >= 45.0f).map(Map.Entry::getKey).collect(Collectors.joining(", "));
            logger.warn("Received ElectrodeCheck with detached electrodes: {}", detachedElectrodes);
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onDetachedElectrode);
            return;
        }
        if (Math.abs(imp1a - impRef) > 3.0f || Math.abs(imp1b - impRef) > 3.0f || Math.abs(imp1a - imp1b) > 3.0f || impedances.values().stream().anyMatch(impedance -> impedance >= 5.0f)) {
            logger.warn("Received ElectrodeCheck with loose electrodes (impedance differs by more than 3 or is greater than 5)");
            Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onLooseElectrode);
            return;
        }
        logger.debug("Received EletrodeCheck without impedance issues in any electrode");
        Narcotrack.getHandlers().forEach(NarcotrackEventHandler::onGoodElectrodes);
    }
}
