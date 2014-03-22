package org.hps.conditions.ecal;

import static org.hps.conditions.TableConstants.ECAL_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_CALIBRATIONS;
import static org.hps.conditions.TableConstants.ECAL_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_GAINS;

import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class loads all ecal conditions into an {@link EcalConditions} object
 * from the database, based on the current run number known by the conditions manager.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {
      
    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {
        
        // Create new, empty conditions object to fill with data.
        EcalConditions conditions = new EcalConditions();
                               
        // Get the channel information from the database.                
        EcalChannelCollection channelMap = manager.getCachedConditions(EcalChannelCollection.class, ECAL_CHANNELS).getCachedData();
        
        // Set the channel map.
        conditions.setChannelMap(channelMap);
                                       
        // Add gains.
        EcalGainCollection gains = manager.getCachedConditions(EcalGainCollection.class, ECAL_GAINS).getCachedData();        
        for (EcalGain gain : gains.getObjects()) {
            ChannelId channelId = new ChannelId();
            channelId.id = gain.getChannelId();
            EcalChannel channel = channelMap.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }
        
        // Add bad channels.
        EcalBadChannelCollection badChannels = manager.getCachedConditions(
                EcalBadChannelCollection.class, ECAL_BAD_CHANNELS).getCachedData();
        for (EcalBadChannel badChannel : badChannels.getObjects()) {
            ChannelId channelId = new ChannelId();
            channelId.id = badChannel.getChannelId();
            EcalChannel channel = channelMap.findChannel(channelId);
            conditions.getChannelConstants(channel).setBadChannel(true);
        }
        
        // Add calibrations including pedestal and noise values.
        EcalCalibrationCollection calibrations = 
                manager.getCachedConditions(EcalCalibrationCollection.class, ECAL_CALIBRATIONS).getCachedData();
        for (EcalCalibration calibration : calibrations.getObjects()) {
            ChannelId channelId = new ChannelId();
            channelId.id = calibration.getChannelId();
            EcalChannel channel = channelMap.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }       
        
        // Return the conditions object to caller.
        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalConditions> getType() {
        return EcalConditions.class;
    }
    
    
}
