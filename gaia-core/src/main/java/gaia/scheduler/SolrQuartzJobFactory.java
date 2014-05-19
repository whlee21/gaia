package gaia.scheduler;

import org.quartz.Job;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class SolrQuartzJobFactory implements JobFactory {

	@Inject
	private Injector injector;

	public Job newJob(TriggerFiredBundle triggerFiredBundle) throws SchedulerException {
		Job result = null;
		Class<?> clazz = triggerFiredBundle.getJobDetail().getJobClass();
		try {
			if (clazz.equals(SolrJob.class))
				result = (Job) injector.getInstance(SolrJob.class);
			else
				result = (Job) injector.getInstance(clazz);
		} catch (Exception e) {
			throw new SchedulerException(e);
		}
		return result;
	}
}
