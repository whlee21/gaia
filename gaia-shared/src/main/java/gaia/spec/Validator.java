package gaia.spec;

import gaia.api.Error;

import java.util.List;

public abstract class Validator {
	public static final Validator NOT_NULL_VALIDATOR = new NotNullValidator();
	public static final Validator NUMBER_VALIDATOR = new NumberValidator();
	public static final Validator LONG_STRING_VALIDATOR = new LongStringValidator();
	public static final Validator INT_STRING_VALIDATOR = new IntStringValidator();
	public static final Validator NON_NEG_INT_STRING_VALIDATOR = new NonNegIntStringValidator();
	public static final Validator NON_NEG_LONG_STRING_VALIDATOR = new NonNegLongStringValidator();
	public static final Validator NOT_BLANK_VALIDATOR = new NotBlankValidator();
	public static final Validator TREAT_BLANK_AS_NULL_VALIDATOR = new TreatBlankAsNullValidator();
	public static final Validator URL_VALIDATOR = new URLValidator();
	public static final Validator URI_VALIDATOR = new URIValidator();
	public static final Validator BOOLEAN_VALIDATOR = new BooleanValidator();
	public static final Validator NULL_VALIDATOR = new NullValidator();
	public static final Validator VALID_REGEX_VALIDATOR = new ValidRegexValidator();
	public static final Validator NOOP_VALIDATOR = new NOOPValidator();
	public static final Validator AUTHENTICATION_VALIDATOR = new AuthenticationValidator();

	public abstract List<Error> validate(SpecProperty paramSpecProperty, Object paramObject);

	public abstract Object cast(SpecProperty paramSpecProperty, Object paramObject);
}
