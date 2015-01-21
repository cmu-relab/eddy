package eddy.lang.parser;

import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValue;
import eddy.lang.RoleValueSet;
import eddy.lang.parser.Tokenizer.Token;

/**
 * Translates statement fragments into {@link Role} objects.
 * 
 * @author Travis Breaux
 *
 */

public abstract class RoleParser {
	public final Role.Type type;
	public final String prefix;
	public final RoleValue generic;
	
	public RoleParser(Role.Type type, String prefix, RoleValue generic) {
		this.type = type;
		this.prefix = prefix;
		this.generic = generic;
	}
	
	public Role parseRole(Parser parser) throws ParseException {
		// return the generic role, if the value is unspecified
		if (prefix.length() > 0) {
			Token token = parser.peekToken();
			
			if (token.type != Token.ROLE || !token.text.equals(prefix)) {
				return new Role(type, prefix, new RoleValueSet.Singleton(generic));
			}
			else {
				parser.nextToken();
			}
		}
		
		return new Role(type, prefix, parseValues(parser));
	}
	
	private RoleValueSet parseValues(Parser parser) throws ParseException {
		// parse the first role value
		RoleValue value = parseValue(parser);
		RoleValueSet lhs = new RoleValueSet.Singleton(value), rhs;
		
		// parse any subsequent role values in a disjunction
		while (true) {
			Token token = parser.peekToken();
			if (token.type == Token.COMMA) {
				parser.nextToken();
				rhs = parseValues(parser);
				lhs = new RoleValueSet.Union(lhs, rhs);
			}
			else if (token.type == Token.BACKSLASH) {
				parser.nextToken();
				rhs = parseValues(parser);
				lhs = new RoleValueSet.Complement(lhs, rhs);
			}
			else if (token.type == Token.PLUS) {
				parser.nextToken();
				rhs = parseValues(parser);
				lhs = new RoleValueSet.Intersect(lhs, rhs);
			}
			else {
				break;
			}
		}
		return lhs;
	}
	
	public abstract RoleValue parseValue(Parser parser) throws ParseException;
	
	public static final RoleParser OBJECT = new RoleParser(Role.Type.OBJECT, "", Datum.ANYTHING) {
		public RoleValue parseValue(Parser parser) throws ParseException {
			return parser.parseDatum();
		}
	};
	
	public static final RoleParser TARGET = new RoleParser(Role.Type.TARGET, "TO", Actor.ANYONE) {
		public RoleValue parseValue(Parser parser) throws ParseException {
			return parser.parseActor();
		}
	};
	
	public static final RoleParser SOURCE = new RoleParser(Role.Type.SOURCE, "FROM", Actor.ANYONE) {
		public RoleValue parseValue(Parser parser) throws ParseException {
			return parser.parseActor();
		}
	};
	
	public static final RoleParser PURPOSE = new RoleParser(Role.Type.PURPOSE, "FOR", Purpose.ANYTHING) {
		public RoleValue parseValue(Parser parser) throws ParseException {
			return parser.parsePurpose();
		}
	};
}
