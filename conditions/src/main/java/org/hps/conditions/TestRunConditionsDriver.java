package org.hps.conditions;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

import org.hps.conditions.svt.TestRunSvtConditions;
import org.hps.conditions.svt.TestRunSvtDetectorSetup;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

/**
 * This {@link org.lcsim.util.Driver} is a subclass of
 * {@link AbstractConditionsDriver} and specifies the database connection
 * parameters and configuration for the test run database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunConditionsDriver extends AbstractConditionsDriver {

    // Default constructor used to setup the database connection
    public TestRunConditionsDriver() {
        if (ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager) {
            // System.out.println(this.getName()+": Found existing DatabaseConditionsManager");
            // manager = (DatabaseConditionsManager)
            // ConditionsManager.defaultInstance();
            throw new RuntimeException("ConditionsManager was already setup.");
        } else {
            manager = new DatabaseConditionsManager();
            manager.configure("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
            manager.register();
        }
    }

    /**
     * Load the {@link TestRunSvtConditions} set onto
     * <code>HpsTestRunSiSensor</code>.
     * 
     * @param detector The detector to update.
     */
    @Override
    protected void loadSvtConditions(Detector detector) {
        TestRunSvtConditions conditions = manager.getCachedConditions(TestRunSvtConditions.class, SVT_CONDITIONS).getCachedData();
        TestRunSvtDetectorSetup loader = new TestRunSvtDetectorSetup();
        loader.load(detector.getSubdetector(svtSubdetectorName), conditions);
    }
}
