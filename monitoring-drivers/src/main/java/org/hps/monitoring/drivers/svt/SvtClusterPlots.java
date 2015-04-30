package org.hps.monitoring.drivers.svt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

import org.hps.recon.tracking.FittedRawTrackerHit;

/**
 *  Monitoring driver that looks at the SVT cluster charge.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SvtClusterPlots extends Driver {
    
    // TODO: Add documentation

    static {
        hep.aida.jfree.AnalysisFactory.register();
    }
   
    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = IAnalysisFactory.create();
	private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();
    private IHistogramFactory histogramFactory = null; 
	private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
   
	// Histogram Maps
	private static Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
	private static Map<String, IHistogram1D> singleHitClusterChargePlots = new HashMap<String, IHistogram1D>();
   
    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, FittedRawTrackerHit> fittedRawTrackerHitMap 
        = new HashMap<RawTrackerHit, FittedRawTrackerHit>();
	
    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    // Collections
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    
    private int runNumber = -1; 
	
    private int computePlotterRegion(HpsSiSensor sensor) {

		if (sensor.getLayerNumber() < 7) {
			if (sensor.isTopLayer()) {
				return 6*(sensor.getLayerNumber() - 1); 
			} else { 
				return 6*(sensor.getLayerNumber() - 1) + 1;
			} 
		} else { 
		
			if (sensor.isTopLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 6*(sensor.getLayerNumber() - 7) + 2;
				} else { 
					return 6*(sensor.getLayerNumber() - 7) + 3;
				}
			} else if (sensor.isBottomLayer()) {
				if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
					return 6*(sensor.getLayerNumber() - 7) + 4;
				} else {
					return 6*(sensor.getLayerNumber() - 7) + 5;
				}
			}
		}
		return -1; 
    }
    
    IPlotterStyle createStyle(HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) { 
    
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);
        
        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(4);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);
       
        if (sensor == null) { 
            style.dataStyle().fillStyle().setColor("255, 38, 38, 1");
            style.dataStyle().outlineStyle().setColor("255, 38, 38, 1");
            style.dataStyle().fillStyle().setOpacity(.70);
        } else if (sensor.isTopLayer()) { 
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else if (sensor.isBottomLayer()){ 
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }
        style.dataStyle().errorBarStyle().setVisible(false);
       
        style.regionBoxStyle().backgroundStyle().setOpacity(.20);
        if (sensor != null && sensor.isAxial()) {  
            style.regionBoxStyle().backgroundStyle().setColor("246, 246, 34, 1");
        }
            
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        return style;
    }
    
    /**
     *  Clear all histograms of it's current data.
     */
    private void resetPlots() { 

        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
        
        // Since all plots are mapped to the name of a sensor, loop 
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) { 
           clusterChargePlots.get(sensor.getName()).reset();
           singleHitClusterChargePlots.get(sensor.getName()).reset();
        }
    }
    
    /**
     *  Method that creates a map between a fitted raw hit and it's corresponding raw fit
     *  
     * @param fittedHits : List of fitted hits to map
     */
    private void mapFittedRawHits(List<FittedRawTrackerHit> fittedHits) { 
        
        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
       
        // Loop through all fitted hits and map them to their corresponding raw hits
        for (FittedRawTrackerHit fittedHit : fittedHits) { 
            fittedRawTrackerHitMap.put(fittedHit.getRawTrackerHit(), fittedHit);
        }
    }
  
    /**
     * 
     * @param rawHit
     * @return
     */
    private FittedRawTrackerHit getFittedHit(RawTrackerHit rawHit) { 
        return fittedRawTrackerHitMap.get(rawHit);
    }
    
    
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
   
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        // If the tree already exist, clear all existing plots of any old data
        // they might contain.
        if (tree != null) { 
            this.resetPlots();
            return; 
        }
        
        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);
        
        plotters.put("Cluster Amplitude", plotterFactory.create("Cluster Amplitude"));
        plotters.get("Cluster Amplitude").createRegions(6, 6);
   
        for (HpsSiSensor sensor : sensors) { 
        
            clusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(clusterChargePlots.get(sensor.getName()), this.createStyle(sensor, "Cluster Amplitude [ADC Counts]", ""));
        
            singleHitClusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Single Hit Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(singleHitClusterChargePlots.get(sensor.getName()), this.createStyle(null, "Cluster Amplitude [ADC Counts]", ""));
        }
    
		for (IPlotter plotter : plotters.values()) { 
			plotter.show();
			for (int regionN = 0; regionN < plotter.numberOfRegions(); regionN++) { 
			    PlotterRegion region = ((PlotterRegion) ((Plotter) plotter).region(regionN));
			    if (region.getPlottedObjects().size() == 0) continue;
                region.getPanel().addMouseListener(new PopupPlotterListener(region));
			}
		}
    }
    
    public void process(EventHeader event) { 
     
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        // If the event doesn't contain fitted raw hits, skip it
        if (!event.hasCollection(FittedRawTrackerHit.class, fittedHitsCollectionName)) return;
        
        // Get the list of fitted hits from the event
        List<FittedRawTrackerHit> fittedHits = event.get(FittedRawTrackerHit.class, fittedHitsCollectionName);
        
        // Map the fitted hits to their corresponding raw hits
        this.mapFittedRawHits(fittedHits);
        
        // If the event doesn't contain any clusters, skip it
        if (!event.hasCollection(SiTrackerHitStrip1D.class, clusterCollectionName)) return;
        
        // Get the list of clusters in the event
        List<SiTrackerHitStrip1D> clusters = event.get(SiTrackerHitStrip1D.class, clusterCollectionName);
       
        for (SiTrackerHitStrip1D cluster : clusters) { 
            
            // Get the sensor associated with this cluster
            HpsSiSensor sensor = (HpsSiSensor) cluster.getSensor();
            
            // Get the raw hits composing this cluster and use them to calculate the amplitude of the hit
            double amplitude = 0;
            for (RawTrackerHit rawHit : cluster.getRawHits()) {
                
                // Add the amplitude of that channel to the total amplitude
                amplitude += this.getFittedHit(rawHit).getAmp();
            }
            
            // Fill all plots
            clusterChargePlots.get(sensor.getName()).fill(amplitude);
            
            if (cluster.getRawHits().size() == 1) { 
                singleHitClusterChargePlots.get(sensor.getName()).fill(amplitude);
            } 
        }
    }
    
    public void endOfData() { 
        
        String rootFile = "run" + runNumber + "_cluster_analysis.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}