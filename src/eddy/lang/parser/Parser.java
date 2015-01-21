package eddy.lang.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.TreeMap;

import eddy.lang.Action;
import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.Type;
import eddy.lang.parser.Tokenizer.Token;

/**
 * Translates policy text into a {@link Policy} object. 
 * 
 * The architecture consists of {@link ActionParser} objects that are specialized 
 * for actions expressed in the language (e.g., COLLECT statements support the 
 * designation of a FROM role over the domain of {@link Actor} objects.) The parser 
 * can be extended to support parsing new actions using this architecture.
 * 
 * @author Travis Breaux
 *
 */

public class Parser {
	private int index;
	private ArrayList<Token> tokens = new ArrayList<Token>();
	private final Tokenizer tokenizer = new Tokenizer();
	private final Logger logger = new Logger(new PrintWriter(System.err));
	private final TreeMap<String, Double> units = new TreeMap<String, Double>();
	private final TreeMap<String, Rule.Modality> modality = new TreeMap<String, Rule.Modality>();
	private final TreeMap<String, ActionParser> actions = new TreeMap<String, ActionParser>();
	private final TreeMap<Modality,Indexer> indexer = new TreeMap<Modality,Indexer>();
	private Policy policy;

	public Parser() {
		// create the unit map for the retention action
		units.put("day", 1.0);
		units.put("month", units.get("day") * (365.0 / 12.0));
		units.put("year", units.get("day") * 365.0);
		
		// create the modality map for parsing rules
		modality.put("P", Rule.Modality.PERMISSION);
		modality.put("O", Rule.Modality.OBLIGATION);
		modality.put("R", Rule.Modality.REFRAINMENT);
		modality.put("E", Rule.Modality.EXCLUSION);
		modality.put("EP", Rule.Modality.EXCLUSION_PERMISSION);
		modality.put("EO", Rule.Modality.EXCLUSION_OBLIGATION);
		modality.put("ER", Rule.Modality.EXCLUSION_REFRAINMENT);
		
		// create and configure the default action parsers
		ActionParser parser;
		parser = new ActionParser("COLLECT");
		parser.add(RoleParser.OBJECT);
		parser.add(RoleParser.SOURCE);
		parser.add(RoleParser.PURPOSE);
		add(parser);
		
		parser = new ActionParser("USE");
		parser.add(RoleParser.OBJECT);
		parser.add(RoleParser.SOURCE);
		parser.add(RoleParser.PURPOSE);
		add(parser);
		
		parser = new ActionParser("TRANSFER");
		parser.add(RoleParser.OBJECT);
		parser.add(RoleParser.SOURCE);
		parser.add(RoleParser.TARGET);
		parser.add(RoleParser.PURPOSE);
		add(parser);
	}
	
	public void add(ActionParser parser) {
		actions.put(parser.name, parser);
	}
	
	private Rule createOnlyRestriction(Rule oldRule) {		
		// set the new modality to exclude the old modality 
		Modality newMod = modality.get("E" + oldRule.modality.toString());
		
		// create the new rule
		Rule newRule = new Rule(oldRule.id + "x", newMod, oldRule.action.clone(), false);

		// restate the purpose to: anything \ the old purpose
		Role role = newRule.action.getRole(Role.Type.PURPOSE);
		RoleValueSet newValues = new RoleValueSet.Complement(Purpose.ANYTHING, role.values);
		Role newRole = new Role(Role.Type.PURPOSE, role.prefix, newValues);
		newRule.action.add(newRole);
		
		return newRule;
	}
	
	public void remove(ActionParser parser) {
		actions.remove(parser.name);
	}

	public Logger getLogger() {
		return logger;
	}

	protected Policy getPolicy() {
		return policy;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	protected Token nextToken() {
		if (tokens.get(index).type == Token.EOF) {
			return tokens.get(index);
		}
		Token token = tokens.get(index);
		index++;
		return token;
	}

	public Policy parse(File file) throws ParseException {
		try {
			return parse(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new ParseException("Cannot find file: " + file);
		}
	}

	public Policy parse(Reader reader) throws ParseException {
		resetIndexer();
		this.policy = new Policy();

		// tokenize the input stream
		this.tokens = getTokenizer().tokenize(reader);
		this.index = 0;
		
		// parse any leading new lines
		while (peekToken().type == Token.NEWLINE) {
			nextToken();
		}

		// parse the header
		parseHeader();

		// parse the body
		Token token1 = nextToken();
		Token token2 = nextToken();
		String header = token1.text + " " + token2.text;

		if (!header.equals("SPEC POLICY")) {
			throw new ParseException("Expecting policy body, but found '" + token1.text + "'", token1.line);
		}
		
		parseNewLines();
		while (peekToken().type != Token.EOF) {
			Token token = nextToken();
			
			if (token.type == Token.TAB) {
				Rule rule = parseRule();
				policy.add(rule);
				
				// generate counter-exceptions, if any
				if (rule.only) {
					rule = createOnlyRestriction(rule);
					policy.add(rule);
				}
				
			} else {
				throw new ParseException("Expected tab, found " + token, token.line);
			}
			parseNewLines();
		}

		return policy;
	}
	
	protected Datum parseDatum() throws ParseException {
		Token token = nextToken();
		if (token.type != Token.WORD) {
			throw new ParseException("Expecing data expression, but found " + token, token.line);
		}
		
		// parse the datum as a data component list and create corresponding type rules
		String[] split = token.text.split("\\.");
		for (int i = 1; i < split.length; i++) {
			Type type = new Type(Type.CLASS_DATUM, split[i - 1], Type.SUPERCLASS, new String[] { split[i] });
			policy.add(type);
		}
		
		return new Datum(split[split.length - 1]);
	}
	
	protected Actor parseActor() throws ParseException {
		Token token = nextToken();
		
		if (token.type != Token.WORD) {
			throw new ParseException("Expecting actor name, but found " + token, token.line);
		}
		
		return new Actor(token.text);
	}
	
	protected Purpose parsePurpose() throws ParseException {
		Token token = nextToken();
		
		if (token.type != Token.WORD) {
			throw new ParseException("Expecting purpose name, but found " + token, token.line);
		}
		
		return new Purpose(token.text);
	}
	
	private void parseHeader() throws ParseException {
		Token token1 = nextToken();
		Token token2 = nextToken();
		String header = token1.text + " " + token2.text;

		// parse the header section title
		if (!header.equals("SPEC HEADER")) {
			throw new ParseException("Expecting header, but found '" + token1.text + "'", token1.line);
		}
		parseNewLines();

		// parse policy attributes; expecting at least namespace
		Token token;
		while (index < tokens.size() + 2 && peekToken().type == Token.TAB
				&& peekToken(1).text.equals("ATTR")) {
			nextToken(); // the tab
			nextToken(); // the keyword ATTR
			token = nextToken();
			if (token.type != Token.WORD) {
				throw new ParseException("Expecting attribute name, but found " + token, token.line);
			}
			String name = token.text;
			String value = "";
			while (peekToken().type != Token.NEWLINE) {
				token = nextToken();
				if (token.type != Token.WORD) {
					throw new ParseException("Expecting attribute value, but found " + token, token.line);
				}
				value += " " + token.text;
			}
			if (value.length() > 0) {
				value = value.substring(1);
			}
			if (value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}
			parseNewLines();
			policy.setAttribute(name, value);
		}

		// parse any concept axioms
		while (peekToken().type == Token.TAB) {
			nextToken();
			policy.add(parseType());
			parseNewLines();
		}
	}

	private void parseNewLines() throws ParseException {
		Token token = nextToken();
		if (token.type != Token.NEWLINE) {
			throw new ParseException("Expecting newline, but found '" + token.text + "'", token.line);
		}

		while (peekToken().type == Token.NEWLINE) {
			nextToken();
		}
	}

	/**
	 * Parses the next statement the rule
	 * 
	 * @param postfix
	 * @return a list of rules. Will contain two rules if there is an ONLY
	 *         clause. The first rule is always the primary (non-exception) rule
	 * @throws ParseException
	 */
	private Rule parseRule() throws ParseException {
		boolean only = false;
		
		// parse the statement modality
		Token token = nextToken();
		Modality modality = this.modality.get(token.text);
		if (token.type != Token.WORD || modality == null) {
			throw new ParseException("Expecting modality {R,O,P,E,ER,EO,EP}, but found " + token, token.line);
		}

		// parse the only keyword
		if (peekToken().text.equals("ONLY")) {
			if (modality.equals(Rule.Modality.EXCLUSION)) {
				throw new ParseException("Cannot use keyword ONLY with exclusion", token.line);
			}
			nextToken();
			only = true;
		}

		token = nextToken();
		if (token.type != Token.ACTION) {
			throw new ParseException("Expecting action keyword, but found " + token, token.line);
		}
		
		// build the rule and parse the roles for this action
		ActionParser parser = actions.get(token.text);
		if (parser == null) {
			throw new ParseException("Unexcepted action '" + token.text + "'", token.line);
		}
		Action action = parser.parseAction(this);
		
		// create the next rule id and create the new rule
		String id = indexer.get(modality).next();
		Rule rule = new Rule(id, modality, action, only);
		return rule;
	}
	
	private Type parseType() throws ParseException {
		// parse the concept types
		Token token = nextToken();
		if (token.type != Token.WORD || "DAP".indexOf(token.text) < 0) {
			throw new ParseException("Expecting concept type {D, A, P}, but found " + token, token.line);
		}
		int clazz = -1;
		switch (token.text.charAt(0)) {
			case 'A':
				clazz = Type.CLASS_ACTOR;
				break;
			case 'D':
				clazz = Type.CLASS_DATUM;
				break;
			case 'P':
				clazz = Type.CLASS_PURPOSE;
				break;
		}
		
		// parse the left-hand side
		token = nextToken();
		if (token.type != Token.WORD) {
			throw new ParseException("Expecting concept name, but found " + token, token.line);
		}
		String lhs = token.text;
		
		// parse the concept relational operator
		token = nextToken();
		int op;
		switch (token.type) {
			case Token.LESS:
				op = Type.SUBCLASS;
				break;
			case Token.GREATER:
				op = Type.SUPERCLASS;
				break;
			case Token.BACKSLASH:
				op = Type.DISJOINT;
				break;
			case Token.EQUALS:
				op = Type.EQUIVALENT;
				break;
			default:
				throw new ParseException("Expecting relational operator {<, >, !=}, but found " + token, token.line);
		}
		
		// parse the right-hand side
		token = nextToken();
		if (token.type != Token.WORD) {
			throw new ParseException("Expecting concept name, but found " + token, token.line);
		}
		ArrayList<String> rhs = new ArrayList<String>();
		rhs.add(token.text);
		while (peekToken().type == Token.COMMA) {
			nextToken();
			token = nextToken();
			if (token.type != Token.WORD) {
				throw new ParseException("Expecting concept name, but found " + token, token.line);
			}
			rhs.add(token.text);
		}
		return new Type(clazz, lhs, op, rhs.toArray(new String[rhs.size()]));
	}

	protected Token peekToken() {
		return tokens.get(index);
	}

	protected Token peekToken(int lookahead) {
		return tokens.get(index + lookahead);
	}

	private void resetIndexer() {		
		indexer.put(Modality.EXCLUSION, new Indexer("e"));
		indexer.put(Modality.EXCLUSION_OBLIGATION, new Indexer("eo"));
		indexer.put(Modality.EXCLUSION_REFRAINMENT, new Indexer("er"));
		indexer.put(Modality.EXCLUSION_PERMISSION, new Indexer("ep"));
		indexer.put(Modality.OBLIGATION, new Indexer("o"));
		indexer.put(Modality.REFRAINMENT, new Indexer("r"));
		indexer.put(Modality.PERMISSION, new Indexer("p"));
	}
	
	class Indexer {
		public final String prefix;
		public int counter = -1;
		public Indexer(String prefix) {
			this.prefix = prefix;
		}
		public String next() {
			counter++;
			return prefix + counter;
		}
	}
}
