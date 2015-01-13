/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;


import org.lcsim.util.Driver;
import hep.aida.IHistogram1D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.ecal.FADCEcalReadoutDriver;
import org.hps.readout.ecal.RingBuffer;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;


/**
 *
 * @author Luca
 */
public class CalibClusterAnalyzerEngRun extends Driver  {
    double energyThreshold=0;
    protected String clusterCollectionName = "EcalClustersGTP";
    protected String clusterCollectionName2 = "EcalClustersIC";
    private FileWriter writer;
    private FileWriter writer2;
    private FileWriter writer3;
    String outputFileName = "CalibClusterAnalyzerEngRunGTPtest.txt";
    String outputFileName2 = "CalibClusterAnalyzerEngRunICtest.txt";
    String outputFileName3 = "CalibClusterAnalyzerEngRunHIT.txt";
    private EcalConditions ecalConditions = null;
    private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalChannelCollection channels= null;
    
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
       }
  
  public void setOutputFileName(String outputFileName){
  this.outputFileName = outputFileName;
  }
  public void setOutputFileName2(String outputFileName2){
  this.outputFileName2 = outputFileName2;
  }
  public void setOutputFileName3(String outputFileName3){
  this.outputFileName3 = outputFileName3;
  }
  
  
  
@Override
    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = detector.getSubdetector(ecalName);
        
      /*  // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();*/
        
        
        
         ecalConditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
         channels = ecalConditions.getChannelCollection();
    }  
   @Override   
public void startOfData(){
    
    
    
    try{
    //initialize the writers
    writer=new FileWriter(outputFileName);
    writer2=new FileWriter(outputFileName2);
    writer3=new FileWriter(outputFileName3);
    //Clear the files
    writer.write("");
    writer2.write("");
    writer3.write("");
    }
    catch(IOException e ){
    System.err.println("Error initializing output file for event display.");
    }
}



public void endOfData(){
  
    try{
//close the file writer.
    writer.close();
    writer2.close();
    writer3.close();
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
} 
    @Override
    public void process (EventHeader event){
        
       // EcalConditions ecalConditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
       // EcalChannelCollection channels = ecalConditions.getChannelCollection();
        //here it writes the GTP clusters info
        if(event.hasCollection(Cluster.class,"EcalClustersGTP"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersGTP");
         for(Cluster cluster : clusters){
           int idBack;
           int idFront;
           idFront=getCrystalFront(cluster);
           idBack=getCrystal(cluster);
           //EcalChannelCollection channels = ecalConditions.getChannelCollection();
           EcalChannel channel = channels.findGeometric(cluster.getCalorimeterHits().get(0).getCellID());
           EcalChannelConstants channelConstants = ecalConditions.getChannelConstants(channel);
          //. System.out.println(channelConstants.getGain().getGain() + " ot asil cristallo " + idBack+  " \n ");
           try{
            writer.append(idBack + " " + idFront + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getCalorimeterHits().get(0).getCorrectedEnergy() + " " + cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix")+" " +cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") +  " " + channelConstants.getGain().getGain() + "\n");
            }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
         }
        }
        //here it writes the ICCluster info
        if(event.hasCollection(Cluster.class,"EcalClustersIC"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersIC");
        ClusterUtilities.sortReconClusterHits(clusters); 
         for(Cluster cluster : clusters){
         EcalChannel channel = channels.findGeometric(cluster.getCalorimeterHits().get(0).getCellID());
           EcalChannelConstants channelConstants = ecalConditions.getChannelConstants(channel);    
           int idBack;
           int idFront;
           idFront=getCrystalFront(cluster);
           idBack=getCrystal(cluster);
           try{
            writer2.append(idBack + " "+ idFront + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getCalorimeterHits().get(0).getCorrectedEnergy() + " " + cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix")+" " +cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") + " " + channelConstants.getGain().getGain() + "\n");
            }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
         }
        }
        
        //here it writes the hit info
     /*   if(event.hasCollection(CalorimeterHit.class,"EcalCalHits"))
        {List<CalorimeterHit> hits= event.get(CalorimeterHit.class,"EcalCalHits");
         for(CalorimeterHit hit : hits){
           int idBack;
           int idFront;
           idFront=getCrystalFront(hit);
           idBack=getCrystal(hit);
           try{
            writer3.append(idBack + " "+ idFront + " " + hit.getCorrectedEnergy() + " " + hit.getIdentifierFieldValue("ix")+ " " + hit.getIdentifierFieldValue("iy") + "\n");
            }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
         }
        }
        
       */ 
    
    }
    
 
 public int getCrystal (Cluster cluster){
 int x,y,id=0;
 x= cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
 y= cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 }   
    public int getCrystal (CalorimeterHit hit){
 int x,y,id=0;
 x= hit.getIdentifierFieldValue("ix");
 y= hit.getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 } 
    
  public int getCrystalFront (Cluster cluster){
 int x,y,id=0;
 x= (-1)*cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
 y= cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 }   
    public int getCrystalFront (CalorimeterHit hit){
 int x,y,id=0;
 x= (-1)*hit.getIdentifierFieldValue("ix");
 y= hit.getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 }   
}
