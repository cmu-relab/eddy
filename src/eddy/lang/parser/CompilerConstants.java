package eddy.lang.parser;

/**
 * Defines the compiler constants, including default namespaces.
 * 
 * @author Travis Breaux
 *
 */

public interface CompilerConstants {
	public static final String NS = "http://gaius.isri.cmu.edu/2011/8/policy-base.owl";
	public static final String nsActor = NS + "#Actor";
	public static final String nsDatum = NS + "#Datum";
	public static final String nsConflict = NS + "#Conflict";
	public static final String nsPurpose = NS + "#Purpose";
	public static final String nsModality = NS + "#Modality";
	public static final String nsRule = NS + "#Rule";
	public static final String nsRight = NS + "#Right";
	public static final String nsAction = NS + "#Action";
	public static final String nsObligation = NS + "#Obligation";
	public static final String nsProhibition = NS + "#Prohibition";
	public static final String nsExclusion = NS + "#Exclusion";
	public static final String nsExclusionOfRight = NS + "#ExclusionOfRight";
	public static final String nsExclusionOfObligation = NS + "#ExclusionOfObligation";
	public static final String nsExclusionOfProhibition = NS + "#ExclusionOfProhibition";
	public static final String nsHasObject = NS + "#hasObject";
	public static final String nsHasSource = NS + "#hasSource";
	public static final String nsHasPurpose = NS + "#hasPurpose";
	public static final String nsHasTarget = NS + "#hasTarget";
	public static final String nsHasInstrument = NS + "#hasInstrument";
	public static final String nsHasSubject = NS + "#hasSubject";
}
