package eddy.lang.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import eddy.lang.Policy;
import eddy.lang.Role;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.Type;

/**
 * Extracts all {@link eddy.lang.RoleValue} names from a given policy. When applied to two 
 * policies, the names from both policies can be separately aligned in a thesaurus.
 * 
 * @author Travis Breaux
 *
 */

public class ThesaurusExtractor {

	public static void extract(Policy policy, File file) throws IOException {
		TreeMap<String,TreeSet<String>> map = new TreeMap<String,TreeSet<String>>();
		Rule[] rule = policy.rules();
		Type[] type = policy.types();
		
		// extract the values from the rules
		for (int i = 0; i < rule.length; i++) {
			Role[] role = rule[i].action.roles();
			
			for (int j = 0; j < role.length; j++) {
				String name = null;
				if (role[j].type == Role.Type.OBJECT) {
					name = "Datum";
				}
				else if (role[j].type == Role.Type.SOURCE || role[j].type == Role.Type.TARGET) {
					name = "Actor";
				}
				else if (role[j].type == Role.Type.PURPOSE) {
					name = "Purpose";
				}
				
				TreeSet<String> terms = map.get(name);
				if (terms == null) {
					terms = new TreeSet<String>();
					map.put(name, terms);
				}
				extract(role[j].values, terms);
			}
		}
		
		// extract the values from the types
		for (int i = 0; i < type.length; i++) {
			TreeSet<String> terms = null;
			
			switch (type[i].type) {
				case Type.CLASS_ACTOR:
					terms = map.get("Actor");
					break;
				case Type.CLASS_DATUM:
					terms = map.get("Datum");
					break;
				case Type.CLASS_PURPOSE:
					terms = map.get("Purpose");
					break;
			}
			
			terms.add(type[i].lhs);
			for (int j = 0; j < type[i].rhs.length; j++) {
				terms.add(type[i].rhs[j]);
			}
		}
		
		FileWriter out = new FileWriter(file);
		for (String name : map.keySet()) {
			out.write("# Terminology for " + name + "\n\n");
			TreeSet<String> terms = map.get(name);
			for (String term : terms) {
				out.write(term + "\n");
			}
		}
		out.close();
	}
	
	private static void extract(RoleValueSet set, TreeSet<String> terms) {
		if (set.isSingle()) {
			String term = set.getValue().toString();
			terms.add(term);
		}
		else {
			RoleValueSet lhs = set.getLHS();
			RoleValueSet rhs = set.getRHS();
			extract(lhs, terms);
			extract(rhs, terms);
		}
	}
}
