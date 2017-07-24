package org.hps.recon.tracking;

import hep.aida.IHistogram1D;

import org.lcsim.util.aida.AIDA;

/**
 * Test class for raw->reco LCIO + producing histograms.
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 2.0 06/04/17$
 */
public class TrackReconWithPlotsTest extends ReconTestSkeleton {

    static final String inputFileName = "hps_005772.0_recon_Rv4657-0-10000_raw.slcio";
    private AIDA aida;

    @Override
    public void testRecon() throws Exception {
        if (inputFileName == null)
            return;
        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();
        String aidaOutputName = "TestPlots_" + inputFileName.replaceAll("slcio", "root");
        nEvents = -1;
        testTrackingDriver = new TrackingReconstructionPlots();
        ((TrackingReconstructionPlots) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TrackingReconstructionPlots) testTrackingDriver).aida = aida;
        super.testRecon();

        IHistogram1D ntracks = aida.histogram1D("Tracks per Event");
        assertTrue("No events in plots", ntracks.entries() > 0);
        assertTrue("No tracks in plots", ntracks.mean() > 0);
    }

}
