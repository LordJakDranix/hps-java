package org.hps.recon.ecal;
/**
 * version of the time dependent ecal gains where a constant factor is used for the corrections
 * to the energy.  
 * @author spaul
 *
 */
public class TimeDependentGainParameters2016Global extends TimeDependentGainParameters{
    protected void setup(){ 
        this.beamEnergy = 2.306;
        setGlobal(new double[]{
            1457140000, 1457230000, 2.2585, .399416,      6.574,
            1457250000, 1457350000, 2.29357, .0388658,    30863,
            1460200000, 1460400000, 2.25799, .117147,     39662.9,
            1460750000, 1461000000, 2.26538, .163699,     40000,
            1461350000, 1461450000, 2.27994, -.00861751,  15844.2,
            1461460000, 1461580000, 2.26131, .0794,       25784.6
        });
    }
}