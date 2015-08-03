package eddy.lang.parser;

/**
 * Describes property names for compilation statistics. These properties are reported via
 * the {@link Compilation#getProperties()} method.
 * 
 * @author Travis Breaux
 */

public interface CompilationProperties {
	public final static String RULE_CONFLICTS = "rule-conflicts";
	public final static String RULE_RIGHTS = "rule-rights";
	public final static String RULE_OBLIGATIONS = "rule-obligations";
	public final static String RULE_PROHIBITIONS = "rule-prohibitions";
	public final static String RULE_EX_OBLIGATION = "rule-exclusions-of-obligation";
	public final static String RULE_EX_PROHIBITION = "rule-exclusions-of-prohibition";
	public final static String RULE_EX_RIGHT = "rule-exclusions-of-right";
	public final static String EXT_COMPUTED = "extension-computed";
	public final static String EXT_SIZE = "extension-size";
	public final static String LIMIT_COMPUTED = "limitation-computed";
	public final static String LIMIT_SOURCE = "limitation-source";
	public final static String LIMIT_TARGET = "limitation-target";
	public final static String LIMIT_RIGHTS = "limitation-rights";
	public final static String LIMIT_VIOLATIONS = "limitation-violations";
}
