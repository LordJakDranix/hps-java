/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.luca;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/**
 *
 * @author Luca
 */
public class CalibTesi extends Driver {
    
    double energyThreshold=0.5;
    protected String clusterCollectionName = "GTPEcalClusters";
    protected String ICclusterCollectionName= "EcalClusters";
   
    //Declaration of histograms array
    AIDA aida = AIDA.defaultInstance();
    ArrayList<IHistogram2D> GTPHists = new ArrayList<IHistogram2D>(442);
    ArrayList<IHistogram1D> ICHists=new ArrayList<IHistogram1D>(442);
    
    
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
       }
  

  
  
 

   @Override   
public void startOfData(){
   //initialize histograms  
      for(int t=0; t<442; t++){
      String cristallo=String.valueOf(t+1);  
      String GTPhistName="GTPCluster-Seeds Energy " + cristallo;
      String IChistName="ICCluster Energy " + cristallo;
      IHistogram2D GTPclushisto=aida.histogram2D(GTPhistName, 150, 0.0,1.5,150,0.0,1.5);
      IHistogram1D ICcluhisto=aida.histogram1D(IChistName,150,0.0,1.5);
      ICHists.add(ICcluhisto);
      GTPHists.add(GTPclushisto);
      }
 
    
}



 
    @Override
    public void process (EventHeader event){
        
      
        //here it writes the GTP clusters info
        if(event.hasCollection(Cluster.class,"EcalClustersGTP"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersGTP");
         for(Cluster cluster : clusters){
           int idBack;
           
           
           idBack=getDBID(cluster);
           
           
           //riempio gli istogrammi
           if(cluster.getEnergy()>energyThreshold){
           GTPHists.get(idBack -1).fill(cluster.getEnergy(),cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
          
           //System.out.println("Clu = " + cluster.getEnergy() + " Seed = " + cluster.getCalorimeterHits().get(0).getCorrectedEnergy() + "\n");
           }         
         }
        } //if gtp
       
        
        ///riempio cluster fichi
        if(event.hasCollection(Cluster.class,ICclusterCollectionName))
        {
        List<Cluster> clusters=event.get(Cluster.class, ICclusterCollectionName);
         
        for(Cluster cluster : clusters){
        int idBack;
        idBack=getDBID(cluster);
        if(cluster.getEnergy()>0.6){
        ICHists.get(idBack-1).fill(cluster.getEnergy());
        }
        
        }   
        }
       
    }
    
    
  public int getDBID ( Cluster cluster ){
    int xx=  cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
    int yy=cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
    int XOFFSET=23;
    int YOFFSET=5;
    int ix = xx<0 ? xx+XOFFSET : xx+XOFFSET-1;
    int iy = yy<0 ? yy+YOFFSET : yy+YOFFSET-1;
    int dbid = ix + 2*XOFFSET*(YOFFSET*2-iy-1) + 1;
    if      (yy ==  1 && xx>-10){ dbid-=9;}
    else if (yy == -1 && xx<-10) {dbid-=9;}
    else if (yy < 0){dbid-=18;}
   return dbid;
}
  
public int getDBID ( CalorimeterHit hit ){
    int xx=  hit.getIdentifierFieldValue("ix");
    int yy=  hit.getIdentifierFieldValue("iy");
    int XOFFSET=23;
    int YOFFSET=5;
    int ix = xx<0 ? xx+XOFFSET : xx+XOFFSET-1;
    int iy = yy<0 ? yy+YOFFSET : yy+YOFFSET-1;
    int dbid = ix + 2*XOFFSET*(YOFFSET*2-iy-1) + 1;
    if      (yy ==  1 && xx>-10){ dbid-=9;}
    else if (yy == -1 && xx<-10) {dbid-=9;}
    else if (yy < 0){dbid-=18;}
   return dbid;
}  
    
}