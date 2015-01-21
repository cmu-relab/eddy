package eddy.lang.parser;

import java.util.ArrayList;

import eddy.lang.Action;
import eddy.lang.Actor;

/**
 * Translates non-modal statement fragments into {@link Action} objects. Each action 
 * parser includes one or more {@link RoleParser} objects that define how to parse 
 * the parameters of the action statement (e.g., DISCLOSE statements support the 
 * designation of a TO role over the domain of {@link Actor} objects.)
 * 
 * @author Travis Breaux
 */

public class ActionParser {
	private final ArrayList<RoleParser> parsers = new ArrayList<RoleParser>();
	public final String name;
	
	public ActionParser(String name) {
		this.name = name;
	}
	
	public void add(RoleParser parser) {
		parsers.add(parser);
	}
	
	public Action parseAction(Parser parser) throws ParseException {
		Action action = new Action(name);
		for (int i = 0; i < parsers.size(); i++) {
			action.add(parsers.get(i).parseRole(parser));
		}
		return action;
	}
}

