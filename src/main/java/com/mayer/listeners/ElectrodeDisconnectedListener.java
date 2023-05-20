package com.mayer.listeners;

import com.mayer.event.NarcotrackEventHandler;
import com.mayer.event.frame.CurrentAssessmentEvent;
import com.mayer.event.frame.EEGEvent;
import com.mayer.event.frame.ElectrodeCheckEvent;
import com.mayer.event.frame.PowerSpectrumEvent;
import com.mayer.event.remains.RemainsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
        if (isElectrodeDisconnected(event.getData().getImp1a())) {
            LOGGER.warn("Received ElectrodeCheck with loose Imp1a Electrode ({})", event.getData().getImp1a());
        }
        if (isElectrodeDisconnected(event.getData().getImp1b())) {
            LOGGER.warn("Received ElectrodeCheck with loose Imp1b Electrode ({})", event.getData().getImp1b());
        }
        if (isElectrodeDisconnected(event.getData().getImpRef())) {
            LOGGER.warn("Received ElectrodeCheck with loose ImpRef Electrode ({})", event.getData().getImpRef());
        }
        if (isElectrodeDisconnected(event.getData().getImp2a())) {
            LOGGER.warn("Received ElectrodeCheck with loose Imp2a Electrode ({})", event.getData().getImp2a());
        }
        if (isElectrodeDisconnected(event.getData().getImp2b())) {
            LOGGER.warn("Received ElectrodeCheck with loose Imp2b Electrode ({})", event.getData().getImp2b());
        }
    }

    private boolean isElectrodeDisconnected(float impedance) {
        return impedance >= 45 || impedance <= 1;
    }

}
