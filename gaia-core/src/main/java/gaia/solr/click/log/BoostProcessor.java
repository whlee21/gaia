package gaia.solr.click.log;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.Text;

public interface BoostProcessor extends Configurable {
	public BoostWritable processCurrent(Text paramText, BoostWritable paramBoostWritable);

	public BoostWritable processHistory(Text paramText, BoostWritable paramBoostWritable1,
			BoostWritable paramBoostWritable2, long paramLong);
}
