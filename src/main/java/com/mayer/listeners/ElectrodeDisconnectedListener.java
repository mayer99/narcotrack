package com.mayer.listeners;

import com.mayer.events.NarcotrackEventHandler;
import com.mayer.events.CurrentAssessmentEvent;
import com.mayer.events.EEGEvent;
import com.mayer.events.ElectrodeCheckEvent;
import com.mayer.events.PowerSpectrumEvent;
import com.mayer.events.RemainsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ElectrodeDisconnectedListener implements NarcotrackEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticHandler.class);

    public ElectrodeDisconnectedListener() {
        EEGEvent.getEventHandlers().add(this);
        CurrentAssessmentEvent.getEventHandlers().add(this);
        PowerSpectrumEvent.getEventHandlers().add(this);
        ElectrodeCheckEvent.getEventHandlers().add(this);
        RemainsEvent.getEventHandlers().add(this);
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
        impedances.put("imp2a", event.getData().getImp2a());
        impedances.put("imp2b", event.getData().getImp2b());
        impedances.forEach((name, impedance) -> {
            if (impedance >= 45 || impedance <= 0.05) {
                LOGGER.warn("Received ElectrodeCheck with loose {} Electrode (impedance: {})", name, impedance);
            }
        });
    }
}
