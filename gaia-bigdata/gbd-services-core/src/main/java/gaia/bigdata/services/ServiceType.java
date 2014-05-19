 package gaia.bigdata.services;
 
 import org.codehaus.jackson.map.annotate.JsonSerialize;
 
 @JsonSerialize
 public enum ServiceType
 {
   ADMIN, 
   ANALYTICS, 
   CLASSIFIER, 
   CLASSIFIER_STATE, 
   CLIENT, 
   CONNECTOR, 
   DATA_MANAGEMENT, 
   DOCUMENT, 
   HADOOP, 
   HBASE, 
   ID, 
   JOB, 
   GAIASEARCH, 
   GAIASEARCH_PROXY, 
   MONITOR, 
   PING, 
   USER, 
   WEBHDFS, 
   WEBHDFS_PROXY, 
   WORKFLOW;
 }

