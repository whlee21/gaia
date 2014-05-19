 package com.lucid.crawl.behemoth;
 
 import com.lucid.Constants;
 import java.io.File;
 import java.io.FileFilter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URI;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.jar.JarEntry;
 import java.util.jar.JarFile;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.filecache.DistributedCache;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.Writable;
 import org.apache.hadoop.mapreduce.InputFormat;
 import org.apache.hadoop.mapreduce.Job;
 import org.apache.hadoop.mapreduce.JobContext;
 import org.apache.hadoop.mapreduce.Mapper;
 import org.apache.hadoop.mapreduce.OutputFormat;
 import org.apache.hadoop.mapreduce.Reducer;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public final class HadoopUtils
 {
   private static final Logger log = LoggerFactory.getLogger(HadoopUtils.class);
 
   public static File getJobFile(String toMatch)
     throws IOException
   {
     File result = null;
     File dir = new File(Constants.LWE_HADOOP_JOB_HOME);
     if (dir.exists()) {
       File[] jobs = dir.listFiles(new FileFilter()
       {
         public boolean accept(File file) {
           return (file.getName().endsWith("job.jar")) || (file.getName().endsWith(".job"));
         }
       });
       for (int i = 0; i < jobs.length; i++) {
         File job = jobs[i];
         JarFile jf = new JarFile(job);
         Enumeration entries = jf.entries();
         while (entries.hasMoreElements()) {
           JarEntry entry = (JarEntry)entries.nextElement();
           String name = entry.getName();
           if (name.contains(toMatch))
             return job;
         }
       }
     }
     else {
       throw new IOException(new StringBuilder().append("No Hadoop Job Home directory: ").append(Constants.LWE_HADOOP_JOB_HOME).toString());
     }
     return result;
   }
 
   public static File getJobFileByName(String toMatch) throws IOException {
     File result = null;
     File dir = new File(Constants.LWE_HADOOP_JOB_HOME);
     if (dir.exists()) {
       log.info(new StringBuilder().append("Loading Hadoop job jar matching '").append(toMatch).append("' from ").append(dir.getAbsolutePath()).toString());
       File[] jobs = dir.listFiles(new FileFilter()
       {
         public boolean accept(File file) {
           String name = file.getName();
           return ((name.endsWith("job.jar")) || (name.endsWith(".job"))) && (name.contains(this.val$toMatch));
         }
       });
       if (jobs.length > 0) {
         return jobs[0];
       }
       log.info(new StringBuilder().append("No matching job jar '").append(toMatch).append("' in ").append(dir.getAbsolutePath()).toString());
     }
     else {
       throw new IOException(new StringBuilder().append("No Hadoop Job Home directory: ").append(Constants.LWE_HADOOP_JOB_HOME).toString());
     }
     return result;
   }
 
   public static Job prepareJob(Path inputPath, Path outputPath, Class<? extends InputFormat> inputFormat, Class<? extends Mapper> mapper, Class<? extends Writable> mapperKey, Class<? extends Writable> mapperValue, Class<? extends OutputFormat> outputFormat, Configuration conf)
     throws IOException
   {
     Job job = new Job(new Configuration(conf));
     Configuration jobConf = job.getConfiguration();
 
     if (mapper.equals(Mapper.class)) {
       throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
     }
     job.setJarByClass(mapper);
 
     job.setInputFormatClass(inputFormat);
     jobConf.set("mapred.input.dir", inputPath.toString());
 
     job.setMapperClass(mapper);
     job.setMapOutputKeyClass(mapperKey);
     job.setMapOutputValueClass(mapperValue);
     job.setOutputKeyClass(mapperKey);
     job.setOutputValueClass(mapperValue);
     jobConf.setBoolean("mapred.compress.map.output", true);
     job.setNumReduceTasks(0);
 
     job.setOutputFormatClass(outputFormat);
     jobConf.set("mapred.output.dir", outputPath.toString());
 
     return job;
   }
 
   public static Job prepareJob(Path inputPath, Path outputPath, Class<? extends InputFormat> inputFormat, Class<? extends Mapper> mapper, Class<? extends Writable> mapperKey, Class<? extends Writable> mapperValue, Class<? extends Reducer> reducer, Class<? extends Writable> reducerKey, Class<? extends Writable> reducerValue, Class<? extends OutputFormat> outputFormat, Configuration conf)
     throws IOException
   {
     Job job = new Job(new Configuration(conf));
     Configuration jobConf = job.getConfiguration();
 
     if (reducer.equals(Reducer.class)) {
       if (mapper.equals(Mapper.class)) {
         throw new IllegalStateException("Can't figure out the user class jar file from mapper/reducer");
       }
       job.setJarByClass(mapper);
     } else {
       job.setJarByClass(reducer);
     }
 
     job.setInputFormatClass(inputFormat);
     jobConf.set("mapred.input.dir", inputPath.toString());
 
     job.setMapperClass(mapper);
     if (mapperKey != null) {
       job.setMapOutputKeyClass(mapperKey);
     }
     if (mapperValue != null) {
       job.setMapOutputValueClass(mapperValue);
     }
 
     jobConf.setBoolean("mapred.compress.map.output", true);
 
     job.setReducerClass(reducer);
     job.setOutputKeyClass(reducerKey);
     job.setOutputValueClass(reducerValue);
 
     job.setOutputFormatClass(outputFormat);
     jobConf.set("mapred.output.dir", outputPath.toString());
 
     return job;
   }
 
   public static String getCustomJobName(String className, JobContext job, Class<? extends Mapper> mapper, Class<? extends Reducer> reducer)
   {
     StringBuilder name = new StringBuilder(100);
     String customJobName = job.getJobName();
     if ((customJobName == null) || (customJobName.trim().isEmpty()))
       name.append(className);
     else {
       name.append(customJobName);
     }
     name.append('-').append(mapper.getSimpleName());
     name.append('-').append(reducer.getSimpleName());
     return name.toString();
   }
 
   public static void delete(Configuration conf, Iterable<Path> paths) throws IOException
   {
     if (conf == null) {
       conf = new Configuration();
     }
     for (Path path : paths) {
       FileSystem fs = path.getFileSystem(conf);
       if (fs.exists(path)) {
         log.info("Deleting {}", path);
         fs.delete(path, true);
       }
     }
   }
 
   public static void delete(Configuration conf, Path[] paths) throws IOException {
     delete(conf, Arrays.asList(paths));
   }
 
   public static InputStream openStream(Path path, Configuration conf) throws IOException
   {
     FileSystem fs = FileSystem.get(path.toUri(), conf);
     return fs.open(path.makeQualified(fs));
   }
 
   public static void cacheFiles(Path fileToCache, Configuration conf)
   {
     DistributedCache.setCacheFiles(new URI[] { fileToCache.toUri() }, conf);
   }
 
   public static Path cachedFile(Configuration conf) throws IOException {
     return new Path(DistributedCache.getCacheFiles(conf)[0].getPath());
   }
 
   public static void setSerializations(Configuration conf) {
     conf.set("io.serializations", "org.apache.hadoop.io.serializer.JavaSerialization,org.apache.hadoop.io.serializer.WritableSerialization");
   }
 }

