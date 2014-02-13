package org.lcsim.hps.readout.ecal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.PriorityQueue;
import java.util.Set;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.hps.evio.EventConstants;
import org.lcsim.hps.recon.ecal.ECalUtils;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.hps.recon.ecal.HPSRawCalorimeterHit;
import org.lcsim.hps.util.*;
import org.lcsim.lcio.LCIOConstants;

/**
 * Performs readout of ECal hits. Simulates time evolution of preamp output
 * pulse.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: FADCEcalReadoutDriver.java,v 1.4 2013/10/31 00:11:02 meeg Exp $
 */
public class FADCEcalReadoutDriver extends EcalReadoutDriver<RawCalorimeterHit> {

    String ecalName = "Ecal";
    Subdetector ecal;
    //buffer for preamp signals (units of volts, no pedestal)
    private Map<Long, RingBuffer> signalMap = null;
    //ADC pipeline for readout (units of ADC counts)
    private Map<Long, FADCPipeline> pipelineMap = null;
    //buffer for window sums
    private Map<Long, Double> sumMap = null;
    //buffer for timestamps
    private Map<Long, Integer> timeMap = null;
    //queue for hits to be output to clusterer
    private PriorityQueue<HPSRawCalorimeterHit> outputQueue = null;
    //length of ring buffer (in readout cycles)
    private int bufferLength = 100;
    //length of readout pipeline (in readout cycles)
    private int pipelineLength = 2000;
    //switch between two pulse shape functions
    private boolean useCRRCShape = false;
    //shaper time constant in ns; negative values generate square pulses of the given width (for test run sim)
    private double tp = 14.0;
    //pulse rise time in ns
    private double riseTime = 10.0;
    //pulse fall time in ns
    private double fallTime = 17.0;
    //delay (number of readout periods) between start of summing window and output of hit to clusterer
    private int delay0 = 32;
    //start of readout window relative to trigger time (in readout cycles)
    //in FADC documentation, "Programmable Latency" or PL
    private int readoutLatency = 100;
    //number of ADC samples to read out
    //in FADC documentation, "Programmable Trigger Window" or PTW
    private int readoutWindow = 100;
    //number of ADC samples to read out before each rising threshold crossing
    //in FADC documentation, "number of samples before" or NSB
    private int numSamplesBefore = 5;
    //number of ADC samples to read out after each rising threshold crossing
    //in FADC documentation, "number of samples before" or NSA
    private int numSamplesAfter = 30;
//    private HPSEcalConverter converter = null;
    //output buffer for hits
    private LinkedList<HPSRawCalorimeterHit> buffer = new LinkedList<HPSRawCalorimeterHit>();
    //number of readout periods for which a given hit stays in the buffer
    private int coincidenceWindow = 2;
    //output collection name for hits read out from trigger
    private String ecalReadoutCollectionName = "EcalReadoutHits";
    private int mode = EventConstants.ECAL_PULSE_INTEGRAL_MODE;
    private int readoutThreshold = 50;
    private int triggerThreshold = 50;
    //amplitude ADC counts/GeV
//    private double gain = 0.5*1000 * 80.0 / 60;
    private double scaleFactor = 128;
    private double fixedGain = -1;
    private boolean constantTriggerWindow = false;
    private boolean addNoise = false;
    private double pePerMeV = 2.0; //photoelectrons per MeV, used to calculate noise
    //parameters for 2014 APDs and preamp
    private double lightYield = 120. / ECalUtils.MeV; // number of photons per MeV
    private double quantumEff = 0.7;  // quantum efficiency of the APD
    private double surfRatio = (10. * 10.) / (16 * 16); // surface ratio between APD and crystals
    private double gainAPD = 150.; // Gain of the APD
    private double elemCharge = 1.60217657e-19;
    private double gainPreAmpl = 0.550e12; // Gain of the preamplifier in V/C, true value is higher but does not take into account losses

    public FADCEcalReadoutDriver() {
        flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store cell ID
        hitClass = HPSRawCalorimeterHit.class;
        setReadoutPeriod(4.0);
//        converter = new HPSEcalConverter(null);
    }

    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }

    public void setConstantTriggerWindow(boolean constantTriggerWindow) {
        this.constantTriggerWindow = constantTriggerWindow;
    }

    public void setFixedGain(double fixedGain) {
        this.fixedGain = fixedGain;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void setReadoutThreshold(int readoutThreshold) {
        this.readoutThreshold = readoutThreshold;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void setTriggerThreshold(int triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    public void setEcalReadoutCollectionName(String ecalReadoutCollectionName) {
        this.ecalReadoutCollectionName = ecalReadoutCollectionName;
    }

    public void setNumSamplesAfter(int numSamplesAfter) {
        this.numSamplesAfter = numSamplesAfter;
    }

    public void setNumSamplesBefore(int numSamplesBefore) {
        this.numSamplesBefore = numSamplesBefore;
    }

    public void setReadoutLatency(int readoutLatency) {
        this.readoutLatency = readoutLatency;
    }

    public void setReadoutWindow(int readoutWindow) {
        this.readoutWindow = readoutWindow;
    }

    public void setCoincidenceWindow(int coincidenceWindow) {
        this.coincidenceWindow = coincidenceWindow;
    }

    public void setUseCRRCShape(boolean useCRRCShape) {
        this.useCRRCShape = useCRRCShape;
    }

    public void setTp(double tp) {
        this.tp = tp;
    }

    public void setFallTime(double fallTime) {
        this.fallTime = fallTime;
    }

    public void setPePerMeV(double pePerMeV) {
        this.pePerMeV = pePerMeV;
    }

    public void setRiseTime(double riseTime) {
        this.riseTime = riseTime;
    }

    public void setDelay0(int delay0) {
        this.delay0 = delay0;
    }

    public void setBufferLength(int bufferLength) {
        this.bufferLength = bufferLength;
        resetFADCBuffers();
    }

    public void setPipelineLength(int pipelineLength) {
        this.pipelineLength = pipelineLength;
        resetFADCBuffers();
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode != EventConstants.ECAL_WINDOW_MODE && mode != EventConstants.ECAL_PULSE_MODE && mode != EventConstants.ECAL_PULSE_INTEGRAL_MODE) {
            throw new IllegalArgumentException("invalid mode " + mode);
        }
    }

    @Override
    protected void readHits(List<RawCalorimeterHit> hits) {

        for (Long cellID : signalMap.keySet()) {
            RingBuffer signalBuffer = signalMap.get(cellID);

            FADCPipeline pipeline = pipelineMap.get(cellID);
            pipeline.step();

            double currentValue = signalBuffer.currentValue() * 4095.0 / 2.0; //12-bit ADC with 2 V range
            double pedestal = EcalConditions.physicalToPedestal(cellID);
            pipeline.writeValue(Math.min((int) Math.round(pedestal + currentValue), 4096)); //ADC can't return a value larger than 4095; 4096 (overflow) is returned for any input >2V

            Double sum = sumMap.get(cellID);
            if (sum == null && currentValue > triggerThreshold) {
                timeMap.put(cellID, readoutCounter);
                if (constantTriggerWindow) {
                    double sumBefore = 0;
                    for (int i = 0; i < numSamplesBefore; i++) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, i, pipeline.getValue(numSamplesBefore - i - 1));
                        }
                        sumBefore += pipeline.getValue(numSamplesBefore - i - 1);
                    }
                    sumMap.put(cellID, sumBefore);
                } else {
                    sumMap.put(cellID, currentValue);
                }
            }
            if (sum != null) {
                if (constantTriggerWindow) {
                    if (timeMap.get(cellID) + numSamplesAfter >= readoutCounter) {
                        if (debug) {
                            System.out.format("trigger %d, %d: %d\n", cellID, readoutCounter - timeMap.get(cellID) + numSamplesBefore - 1, pipeline.getValue(0));
                        }
                        sumMap.put(cellID, sum + pipeline.getValue(0));
                    } else if (timeMap.get(cellID) + delay0 <= readoutCounter) {
//                        System.out.printf("sum = %f\n", sum);
                        outputQueue.add(new HPSRawCalorimeterHit(cellID,
                                (int) Math.round(sum / scaleFactor),
                                64 * timeMap.get(cellID),
                                readoutCounter - timeMap.get(cellID) + 1));
                        sumMap.remove(cellID);
                    }
                } else {
                    if (currentValue < triggerThreshold || timeMap.get(cellID) + delay0 == readoutCounter) {
//					System.out.printf("sum = %f\n",sum);
                        outputQueue.add(new HPSRawCalorimeterHit(cellID,
                                (int) Math.round((sum + currentValue) / scaleFactor),
                                64 * timeMap.get(cellID),
                                readoutCounter - timeMap.get(cellID) + 1));
                        sumMap.remove(cellID);
                    } else {
                        sumMap.put(cellID, sum + currentValue);
                    }
                }
            }
            signalBuffer.step();
        }
        while (outputQueue.peek() != null && outputQueue.peek().getTimeStamp() / 64 <= readoutCounter - delay0) {
            if (outputQueue.peek().getTimeStamp() / 64 < readoutCounter - delay0) {
                System.out.println("Stale hit in output queue");
                outputQueue.poll();
            } else {
                buffer.add(outputQueue.poll());
            }
        }
        while (!buffer.isEmpty() && buffer.peek().getTimeStamp() / 64 <= readoutCounter - delay0 - coincidenceWindow) {
            buffer.remove();
        }
        if (debug) {
            for (RawCalorimeterHit hit : buffer) {
                System.out.format("new hit: energy %d\n", hit.getAmplitude());
            }
        }

        hits.addAll(buffer);
    }

    @Override
    public void startOfData() {
        super.startOfData();
        if (ecalReadoutCollectionName == null) {
            throw new RuntimeException("The parameter ecalReadoutCollectionName was not set!");
        }
    }

    @Override
    protected void processTrigger(EventHeader event) {
        switch (mode) {
            case EventConstants.ECAL_WINDOW_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in window mode");
                }
                event.put(ecalReadoutCollectionName, readWindow(), RawTrackerHit.class, 0, ecalReadoutName);
                break;
            case EventConstants.ECAL_PULSE_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in pulse mode");
                }
                event.put(ecalReadoutCollectionName, readPulses(), RawTrackerHit.class, 0, ecalReadoutName);
                break;
            case EventConstants.ECAL_PULSE_INTEGRAL_MODE:
                if (debug) {
                    System.out.println("Reading out ECal in integral mode");
                }
                event.put(ecalReadoutCollectionName, readIntegrals(), RawCalorimeterHit.class, flags, ecalReadoutName);
                break;
        }
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ClockSingleton.getTime() + triggerDelay;
        int cycle = (int) Math.floor((triggerTime - readoutOffset + ClockSingleton.getDt()) / readoutPeriod);
        double readoutTime = (cycle - readoutLatency) * readoutPeriod + readoutOffset - ClockSingleton.getDt();
        return readoutTime;
    }

    protected short[] getWindow(long cellID) {
        FADCPipeline pipeline = pipelineMap.get(cellID);
        short[] adcValues = new short[readoutWindow];
        for (int i = 0; i < readoutWindow; i++) {
            adcValues[i] = (short) pipeline.getValue(readoutLatency - i - 1);
//			if (adcValues[i] != 0) {
//				System.out.println("getWindow: " + adcValues[i] + " at i = " + i);
//			}
        }
        return adcValues;
    }

    protected List<RawTrackerHit> readWindow() {
//		System.out.println("Reading FADC data");
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        for (Long cellID : pipelineMap.keySet()) {
            short[] adcValues = getWindow(cellID);
            hits.add(new BaseRawTrackerHit(cellID, 0, adcValues));
        }
        return hits;
    }

    protected List<RawTrackerHit> readPulses() {
//		System.out.println("Reading FADC data");
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        for (Long cellID : pipelineMap.keySet()) {
            short[] window = getWindow(cellID);
            short[] adcValues = null;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;
            for (int i = 0; i < readoutWindow; i++) {
                if (numSamplesToRead != 0) {
                    adcValues[adcValues.length - numSamplesToRead] = window[i - pointerOffset];
                    numSamplesToRead--;
                    if (numSamplesToRead == 0) {
                        hits.add(new BaseRawTrackerHit(cellID, thresholdCrossing, adcValues));
                    }
                } else if ((i == 0 || window[i - 1] <= EcalConditions.physicalToPedestal(cellID) + readoutThreshold) && window[i] > EcalConditions.physicalToPedestal(cellID) + readoutThreshold) {
                    thresholdCrossing = i;
                    pointerOffset = Math.min(numSamplesBefore, i);
                    numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, readoutWindow - i - pointerOffset - 1);
                    adcValues = new short[numSamplesToRead];
                }
            }
        }
        return hits;
    }

    protected List<RawCalorimeterHit> readIntegrals() {
//		System.out.println("Reading FADC data");
        List<RawCalorimeterHit> hits = new ArrayList<RawCalorimeterHit>();
        for (Long cellID : pipelineMap.keySet()) {
            short[] window = getWindow(cellID);
            int adcSum = 0;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;
            if (window != null) {
                for (int i = 0; i < readoutWindow; i++) {
                    if (numSamplesToRead != 0) {
                        if (debug) {
                            System.out.format("readout %d, %d: %d\n", cellID, numSamplesBefore + numSamplesAfter - numSamplesToRead, window[i - pointerOffset]);
                        }
                        adcSum += window[i - pointerOffset];
                        numSamplesToRead--;
                        if (numSamplesToRead == 0) {
                            hits.add(new BaseRawCalorimeterHit(cellID, adcSum, 64 * thresholdCrossing));
                        }
                    } else if ((i == 0 || window[i - 1] <= EcalConditions.physicalToPedestal(cellID) + readoutThreshold) && window[i] > EcalConditions.physicalToPedestal(cellID) + readoutThreshold) {
                        thresholdCrossing = i;
                        pointerOffset = Math.min(numSamplesBefore, i);
                        numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, readoutWindow - i - pointerOffset - 1);
                        adcSum = 0;
                    }
                }
            }
        }
        return hits;
    }

    @Override
    protected void putHits(List<CalorimeterHit> hits) {
        //fill the readout buffers
        for (CalorimeterHit hit : hits) {
            RingBuffer eDepBuffer = signalMap.get(hit.getCellID());
            double energyAmplitude = hit.getRawEnergy();
            if (addNoise) {
                //add preamp noise and photoelectron Poisson noise in quadrature
                double noise = Math.sqrt(Math.pow(EcalConditions.physicalToNoise(hit.getCellID()) * EcalConditions.physicalToGain(hit.getCellID()) * ECalUtils.MeV, 2) + hit.getRawEnergy() * ECalUtils.MeV / pePerMeV);
                energyAmplitude += RandomGaussian.getGaussian(0, noise);
            }
            for (int i = 0; i < bufferLength; i++) {
                eDepBuffer.addToCell(i, energyAmplitude * pulseAmplitude((i + 1) * readoutPeriod + readoutTime() - (ClockSingleton.getTime() + hit.getTime()), hit.getCellID()));
            }
        }
    }

    @Override
    protected void initReadout() {
        //initialize buffers
        sumMap = new HashMap<Long, Double>();
        timeMap = new HashMap<Long, Integer>();
        outputQueue = new PriorityQueue(20, new HPSRawCalorimeterHit.TimeComparator());
        resetFADCBuffers();
    }

    @Override
    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = detector.getSubdetector(ecalName);
        resetFADCBuffers();
    }

    private boolean resetFADCBuffers() {
        if (ecal == null) {
            return false;
        }
        signalMap = new HashMap<Long, RingBuffer>();
        pipelineMap = new HashMap<Long, FADCPipeline>();
        Set<Long> cells = ((HPSEcal3) ecal).getNeighborMap().keySet();
        for (Long cellID : cells) {
            signalMap.put(cellID, new RingBuffer(bufferLength));
            pipelineMap.put(cellID, new FADCPipeline(pipelineLength, (int) Math.round(EcalConditions.physicalToPedestal(cellID))));
        }
        return true;
    }

    private double pulseAmplitude(double time, long cellID) {
        if (useCRRCShape) {
            if (time <= 0.0) {
                return 0.0;
            }

            //normalization constant from cal gain (MeV/integral bit) to amplitude gain (amplitude bit/GeV)
            double gain;
            if (fixedGain > 0) {
                gain = readoutPeriod / (fixedGain * ECalUtils.MeV * (4095.0 / 2.0));
            } else {
                gain = readoutPeriod / (EcalConditions.physicalToGain(cellID) * ECalUtils.MeV * (4095.0 / 2.0));
            }

            if (tp > 0.0) {
                return gain * ((time / tp) * Math.exp(1.0 - time / tp)) / (tp * Math.E);
            } else {
                if (time < -tp) {
                    return 1.0;
                } else {
                    return 0.0;
                }
            }
        } else {   // According to measurements the output signal can be fitted by two gaussians, one for the rise of the signal, one for the fall
            // Time corresponds to t-(t_eve+pulseDelay) such that the maximum of the amplitude is reached pulseDelay ns after the event t_eve
            // Without the coefficient, the integral is equal to 1 - 1 per 1000
            if (time <= 0.0) {
                return 0.0;
            }

            //if fixedGain is set, multiply the default gain by this factor
            double gain = 1.0;
            if (fixedGain > 0) {
                gain = fixedGain;
            }

            double norm = ((riseTime + fallTime) / 2) * 1e-9 * Math.sqrt(2 * Math.PI); //to ensure the total integral is equal to 1; gives 3.3839e-8 for default rise and fall times

            if (time < 3 * riseTime) {
                return gain * lightYield * quantumEff * surfRatio * gainAPD * gainPreAmpl * elemCharge * funcGaus(time - 3 * riseTime, riseTime) / norm;
            } else {
                return gain * lightYield * quantumEff * surfRatio * gainAPD * gainPreAmpl * elemCharge * funcGaus(time - 3 * riseTime, fallTime) / norm;
            }
        }
    }

    // Gaussian function needed for the calculation of the pulse shape amplitude  
    public static double funcGaus(double t, double sig) {
        return Math.exp(-t * t / (2 * sig * sig));
    }

    private class FADCPipeline {

        private int[] array;
        private int size;
        private int ptr;

        public FADCPipeline(int size) {
            this.size = size;
            array = new int[size]; //initialized to 0
            ptr = 0;
        }

        //construct pipeline with a nonzero initial value
        public FADCPipeline(int size, int init) {
            this.size = size;
            array = new int[size];
            for (int i = 0; i < size; i++) {
                array[i] = init;
            }
            ptr = 0;
        }

        /**
         * Write value to current cell
         */
        public void writeValue(int val) {
            array[ptr] = val;
        }

        /**
         * Write value to current cell
         */
        public void step() {
            ptr++;
            if (ptr == size) {
                ptr = 0;
            }
        }

        //return content of specified cell (pos=0 for current cell)
        public int getValue(int pos) {
            if (pos >= size || pos < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return array[((ptr - pos) % size + size) % size];
        }
    }
}
