package com.mayer.listeners;

import com.mayer.Narcotrack;
import com.mayer.NarcotrackEventHandler;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ElectrodeDisconnectedListener implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectrodeDisconnectedListener.class);

    public ElectrodeDisconnectedListener() {
        Narcotrack.registerNarcotrackEventListener(this);
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
        HashMap<String, Float> impedances = new HashMap<>();
        impedances.put("imp1a", event.getData().getImp1a());
        impedances.put("imp1b", event.getData().getImp1b());
        impedances.put("impRef", event.getData().getImpRef());
        impedances.forEach((name, impedance) -> {
            if (impedance >= 45) {
                LOGGER.warn("Received ElectrodeCheck with loose {} Electrode (impedance: {})", name, impedance);
            }
        });
    }
}
