package gaia.admin.collection;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.client.solrj.util.ClientUtils;

public class SchedParamUtil {
	public static final String AUTOCOMPLETE_ACT = "autocomplete";
	public static final String CLICK_ACT = "click";
	public static final String SPELLING_ACT = "spelling";
	public static final String OPTIMIZE_ACT = "optimize";
	public static final String CRAWL_ACT = "crawl";
	public static final Set<String> ACT_TYPES = new HashSet<String>();

	public static void fillOptimizeCommand(ScheduledSolrCommand cmd, String collection) {
		GaiaSolrParams params = new GaiaSolrParams();
		params.set("jobHandler", new String[] { "/update" });
		params.set("coreName", new String[] { collection });
		params.set("optimize", new String[] { "true" });

		addScheduleParams(cmd.getSchedule(), params);

		cmd.setParams(params);
	}

	public static ScheduledSolrCommand createCrawlCommand(Schedule schedule, String collection, String dsId) {
		ScheduledSolrCommand cmd = new ScheduledSolrCommand(dsId);
		cmd.setSchedule(schedule);
		if (schedule != null) {
			schedule.setActivity("crawl");
		}
		cmd.setName("crawl " + dsId);
		fillCrawlCommand(cmd, collection, dsId);
		return cmd;
	}

	public static void fillCrawlCommand(ScheduledSolrCommand cmd, String collection, String dsId) {
		GaiaSolrParams params = new GaiaSolrParams();
		params.set("jobHandler", new String[] { "--crawl--" });
		params.set("coreName", new String[] { collection });
		params.set("trackHistory", new String[] { "false" });
		params.set("coreName", new String[] { collection });
		params.set("id", new String[] { dsId });
		params.set("commit", new String[] { "true" });
		addScheduleParams(cmd.getSchedule(), params);
		cmd.setParams(params);
	}

	public static void fillClickCommand(ScheduledSolrCommand cmd, String collection) {
		GaiaSolrParams params = new GaiaSolrParams();
		params.set("jobHandler", new String[] { "/click" });
		params.set("coreName", new String[] { collection });
		params.set("request", new String[] { "PROCESS" });

		params.set("sync", new String[] { "true" });
		params.set("commit", new String[] { "true" });

		addScheduleParams(cmd.getSchedule(), params);

		cmd.setParams(params);
	}

	public static void fillAutocompleteCommand(ScheduledSolrCommand cmd, String collection) {
		GaiaSolrParams params = new GaiaSolrParams();
		params.set("jobHandler", new String[] { "/autocomplete" });
		params.set("coreName", new String[] { collection });
		params.set("cmd_id", new String[] { cmd.getId() });
		params.set("q", new String[] { "foo" });
		params.set("rows", new String[] { String.valueOf(0) });
		params.set("spellcheck", new String[] { "true" });
		params.set("spellcheck.build", new String[] { "true" });

		addScheduleParams(cmd.getSchedule(), params);

		cmd.setParams(params);
	}

	public static void fillSpellingCommand(ScheduledSolrCommand cmd, String collection, String jobHandler) {
		GaiaSolrParams params = new GaiaSolrParams();
		params.set("jobHandler", new String[] { jobHandler });
		params.set("q", new String[] { "foo" });
		params.set("rows", new String[] { String.valueOf(0) });
		params.set("spellcheck", new String[] { "true" });
		params.set("spellcheck.build", new String[] { "true" });
		params.set("coreName", new String[] { collection });
		addScheduleParams(cmd.getSchedule(), params);

		cmd.setParams(params);
	}

	public static GaiaSolrParams addParams(ScheduledSolrCommand cmd, String collection, CollectionManager cm) {
		String act_type = cmd.getName();
		if (!ACT_TYPES.contains(act_type)) {
			if (cmd.getSchedule() != null) {
				act_type = cmd.getSchedule().getActivity();
			}
		}
		if ("optimize".equalsIgnoreCase(act_type))
			fillOptimizeCommand(cmd, collection);
		else if ("spelling".equalsIgnoreCase(act_type))
			fillSpellingCommand(cmd, collection, cm.getGaiaSearchHandler());
		else if ("click".equalsIgnoreCase(act_type))
			fillClickCommand(cmd, collection);
		else if ("autocomplete".equalsIgnoreCase(act_type))
			fillAutocompleteCommand(cmd, collection);
		else if ("crawl".equalsIgnoreCase(act_type))
			fillCrawlCommand(cmd, collection, cmd.getId());
		else {
			throw new IllegalStateException("Unknown or unsupported activity " + act_type + ": " + cmd.toString());
		}
		return cmd.getParams();
	}

	public static void addScheduleParams(Schedule schedule, GaiaSolrParams params) {
		if (schedule == null) {
			schedule = new Schedule();
		}
		Date scheduledTime = schedule.getStartTime();
		int interval = schedule.getInterval();
		if (scheduledTime != null) {
			params.set("jobStartDate", new String[] { ClientUtils.getThreadLocalDateFormat().format(scheduledTime) });
			Date endDate = schedule.getEndTime();
			if (endDate != null) {
				params.set("jobEndDate", new String[] { ClientUtils.getThreadLocalDateFormat().format(endDate) });
			}
			params.set("jobRepeat", new String[] { String.valueOf(interval > 0 ? true : false) });
			if (interval > 0) {
				params.set("jobRepeatInterval", new String[] { String.valueOf(interval) });
				params.set("jobRepeatUnits", new String[] { schedule.getRepeatUnit().toString() });
			}
		}
	}

	static {
		ACT_TYPES.add("autocomplete");
		ACT_TYPES.add("click");
		ACT_TYPES.add("spelling");
		ACT_TYPES.add("optimize");
		ACT_TYPES.add("crawl");
	}
}
