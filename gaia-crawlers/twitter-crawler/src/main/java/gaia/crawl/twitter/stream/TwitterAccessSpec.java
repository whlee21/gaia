package gaia.crawl.twitter.stream;

import gaia.api.Error;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.spec.LooseListValidator;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class TwitterAccessSpec extends DataSourceSpec {
	public static final int DEFAULT_SLEEP = 10000;
	public static final long DEFAULT_MAX_DOCS = -1L;
	public static final String CONSUMER_KEY = "consumer_key";
	public static final String CONSUMER_SECRET = "consumer_secret";
	public static final String ACCESS_TOKEN = "access_token";
	public static final String TOKEN_SECRET = "token_secret";
	public static final String FILTER_FOLLOW = "filter_follow";
	public static final String FILTER_TRACK = "filter_track";
	public static final String FILTER_LOCATIONS = "filter_locations";
	public static final String SLEEP_AMOUNT = "sleep";
	public static final String MAX_DOCS = "max_docs";
	public static final String URL = "url";

	public TwitterAccessSpec() {
		super(DataSourceSpec.Category.Other.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty(URL, "The URL used by the Twitter Stream", String.class,
				"https://stream.twitter.com", Validator.NOOP_VALIDATOR, false, true, SpecProperty.HINTS_DEFAULT));

		addSpecProperty(new SpecProperty.Separator("Auth settings"));
		addSpecProperty(new SpecProperty(CONSUMER_KEY,
				"The OAuth Consumer Key is provided by Twitter when registering the application.", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty(CONSUMER_SECRET,
				"The OAuth Consumer Secret is provided by Twitter when registering the application.", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true, new String[] { SpecProperty.Hint.secret.toString(),
						SpecProperty.Hint.summary.toString() }));

		addSpecProperty(new SpecProperty(ACCESS_TOKEN,
				"The OAuth Access Token is provided by Twitter when registering the application.", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty(TOKEN_SECRET,
				"The OAuth Token Secret is provided by Twitter when registering the application.", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true, new String[] { SpecProperty.Hint.secret.toString(),
						SpecProperty.Hint.summary.toString() }));

		addSpecProperty(new SpecProperty.Separator("Filter stream settings"));

		addSpecProperty(new SpecProperty(FILTER_FOLLOW, "Set of users (user ids) to track", List.class, null,
				new LooseListValidator(Long.class), false));

		addSpecProperty(new SpecProperty(FILTER_TRACK, "Keywords or phrases to track", List.class, null,
				new LooseListValidator(String.class), false));

		addSpecProperty(new SpecProperty(FILTER_LOCATIONS,
				"Set of bounding boxes (e.g. 'left,bottom,right,top' lat/long coordinates)", List.class, null,
				new LooseListValidator(String.class), false));

		addSpecProperty(new SpecProperty.Separator("other limits"));
		addSpecProperty(new SpecProperty(SLEEP_AMOUNT,
				"The amount of time, in milliseconds, to sleep when listening so as to not get throttled", Integer.class,
				Integer.valueOf(10000), Validator.INT_STRING_VALIDATOR, true));

		addSpecProperty(new SpecProperty(MAX_DOCS,
				"The maximum number of documents to pull down, as a long. -1 for no limit", Long.class, Long.valueOf(-1L),
				Validator.LONG_STRING_VALIDATOR, false));

		addCommitProperties();

		addFieldMappingProperties();

		addBatchProcessingProperties();
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		if (errors.size() > 0) {
			return errors;
		}

		Object loc = map.get(FILTER_LOCATIONS);
		Validator propValidator = new LooseListValidator(String.class);
		loc = propValidator.cast(getSpecProperty(FILTER_LOCATIONS), loc);
		if ((loc != null) && (!loc.toString().isEmpty())) {
			List locations = (List) loc;
			if (locations != null) {
				try {
					parseLocations(locations);
				} catch (IllegalArgumentException e) {
					errors.add(new Error(FILTER_LOCATIONS, Error.E_INVALID_VALUE, e.getMessage()));
				}
			}
		}
		return errors;
	}

	public static double[][] parseLocations(List<String> locations) {
		if (locations == null)
			return (double[][]) null;
		if (locations.isEmpty())
			return (double[][]) null;
		double[][] res = new double[locations.size() * 2][2];
		for (int i = 0; i < locations.size(); i++) {
			String bbox = (String) locations.get(i);
			if (bbox == null) {
				throw new IllegalArgumentException("List item is null, but should be bounding box encoded as string");
			}
			String[] coords = StringUtils.split(bbox, ",");
			if (coords == null) {
				throw new IllegalArgumentException("List item is null, but should be bounding box encoded as string");
			}
			if (coords.length != 4) {
				throw new IllegalArgumentException("Bounding box " + Arrays.toString(coords)
						+ " should be encoded as 'left,bottom,right,top'");
			}
			for (int j = 0; j < coords.length; j++) {
				String coord = coords[j];
				try {
					double c = Double.parseDouble(coord);

					res[(i * 2 + j / 2)][(j % 2)] = c;
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid value " + Arrays.toString(coords) + " - " + coord
							+ " is not a number");
				}
			}
		}
		return res;
	}

	public FieldMapping getDefaultFieldMapping() {
		return new FieldMapping();
	}
}
