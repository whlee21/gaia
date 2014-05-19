package gaia.bigdata.hadoop.commoncrawl;


//public class ARCInputSource extends ARCSplitCalculator implements ARCSource, JobConfigurable {
//	private JobConf jc;
//
//	protected void configureImpl(JobConf job) {
//		this.jc = job;
//	}
//
//	public InputStream getStream(String resource, long streamPosition, Throwable lastError, int previousFailures)
//			throws Throwable {
//		if ((lastError != null) || (previousFailures > 0)) {
//			return null;
//		}
//
//		if (streamPosition != 0L) {
//			throw new RuntimeException("Non-zero position requested");
//		}
//
//		if (this.jc == null) {
//			throw new NullPointerException("Jc is null");
//		}
//		Path path = new Path(resource);
//		FileSystem fs = path.getFileSystem(this.jc);
//		System.err.println("getStream:: Opening: " + resource);
//		FSDataInputStream is = fs.open(path);
//		is.seek(streamPosition);
//		return is;
//	}
//
//	protected Collection<ARCResource> getARCResources(JobConf job) throws IOException {
//		Path[] inputPaths = FileInputFormat.getInputPaths(job);
//		List<ARCResource> arc_resources = new LinkedList<ARCResource>();
//		for (Path inputPath : inputPaths) {
//			FileSystem fs = inputPath.getFileSystem(job);
//			FileStatus[] fstats = fs.globStatus(inputPath);
//			for (FileStatus fstat : fstats) {
//				arc_resources.add(new ARCResource(fstat.getPath().toUri().toASCIIString(), fstat.getLen()));
//			}
//		}
//		return arc_resources;
//	}
//}
public class ARCInputSource {
	
}