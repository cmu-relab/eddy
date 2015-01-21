package eddy.lang.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class PolicyGenerator {
	private final ArrayList<String> actors = new ArrayList<String>();
	private final TreeMap<String,Integer> actorSub = new TreeMap<String,Integer>();
	private final ArrayList<String> data = new ArrayList<String>();
	private final TreeMap<String,Integer> dataSub = new TreeMap<String,Integer>();
	private final ArrayList<String> purposes = new ArrayList<String>();
	private final TreeMap<String,Integer> purposeSub = new TreeMap<String,Integer>();
	private final ArrayList<Flow> flows = new ArrayList<Flow>();
	private final Random random;
	private Config config = new Config();
	
	public PolicyGenerator(Random random) {
		this.random = random;
	}
	
	public String generatePolicy() {
		String text = "";
		text += createHeader();
		text += createPolicy();
		return text;
	}
	
	/**
	 * Create the header specification consisting of domain descriptions
	 * @return
	 */
	
	private String createHeader() {
		String text = "SPEC HEADER\n";
		
		text += "\tATTR NAMESPACE " + config.namespace + "\n";
		
		// create the actor hierarchy
		text += createHierarchy("A", actorSub, config.actorHeight, config.actorSpan);
		actors.addAll(actorSub.keySet());
		
		// create the data hierarchy
		text += createHierarchy("D", dataSub, config.dataHeight, config.dataSpan);
		data.addAll(dataSub.keySet());
		
		// create the purpose hierarchy
		text += createHierarchy("P", purposeSub, config.purposeHeight, config.purposeSpan);
		purposes.addAll(purposeSub.keySet());
		
		return text;
	}
	
	/**
	 * Create the policy specification consisting of data rules
	 * @return
	 */
	
	private String createPolicy() {
		String text = "SPEC POLICY\n";
		
		// create the flows form which to choose data flow rules
		createFlows(config.flows);
		
		// create the rights
		text += createRulesCollection("R", config.collectRights, Integer.MAX_VALUE);
		text += createRulesUse("R", config.useRights, Integer.MAX_VALUE);
		text += createRulesTransfer("R", config.transferRights, Integer.MAX_VALUE);
		
		// create the prohibitions with conflicts
		int maxPower = (int) (Math.pow(config.actorHeight, config.actorSpan) 
				* Math.pow(config.dataHeight, config.dataSpan) 
				* Math.pow(config.actorHeight, config.actorSpan));
		int limit = (int) (maxPower * config.conflictLimit);
		
		text += createRulesCollection("P", config.collectProhibitions, limit);
		
		// should we ensure that collections always map to transfers?
		return text;
	}
	
	private String createRulesCollection(String type, int count, int limit) {
		String text = "";
		int power = 0;
		
		for (int i = 0; i < count && power < limit; i++) {
			Flow flow = flows.get(random.nextInt(flows.size()));
			power += actorSub.get(flow.source) * dataSub.get(flow.datum) * purposeSub.get(flow.purpose);
			text += "\t" + type + " COLLECT " + flow.datum + " FROM " + flow.source + " FOR " + flow.purpose + "\n";
		}
		return text;
	}
	
	private String createRulesUse(String type, int count, int limit) {
		String text = "";
		int power = 0;
		
		for (int i = 0; i < count && power < limit; i++) {
			Flow flow = flows.get(random.nextInt(flows.size()));
			power += actorSub.get(flow.source) * dataSub.get(flow.datum) * purposeSub.get(flow.purpose);
			text += "\t" + type + " USE " + flow.datum + " FROM " + flow.source + " FOR " + flow.purpose + "\n";
		}
		return text;
	}
	
	
	private String createRulesTransfer(String type, int count, int limit) {
		String text = "";
		int power = 0;
		
		for (int i = 0; i < count && power < limit; i++) {
			Flow flow = flows.get(random.nextInt(flows.size()));
			power += actorSub.get(flow.source) * dataSub.get(flow.datum) * purposeSub.get(flow.purpose) * actorSub.get(flow.target); 
			text += "\t" + type + " TRANSFER " + flow.datum + " FROM " + flow.source + " TO " + flow.target + " FOR " + flow.purpose + "\n";
		}
		return text;
	}
	
	/**
	 * Create the pool of data flows from which we select rules
	 * @param flows
	 */
	
	private void createFlows(int flows) {
		this.flows.clear();
		int a6 = actors.size() / 7;
		int d6 = data.size() / 7;
		int p6 = purposes.size() / 7;
		int s, t, d, p;
		
		for (int i = 0; i < flows; i++) {

			// if the number of domain concepts is large, select flow concepts from a normal distribution
			if (a6 > 0) {
				s = (int) Math.round(random.nextGaussian() * a6) + (3 * a6);
				t = (int) Math.round(random.nextGaussian() * a6) + (3 * a6);
				s = Math.min(s, actors.size() - 1);
				s = Math.max(s, 0);
				t = Math.min(t, actors.size() - 1);
				t = Math.max(t, 0);
			}
			
			// else, select flow concepts randomly
			else {
				s = random.nextInt(actors.size());
				t = random.nextInt(actors.size());
			}
			
			// repeat this selection process for datum and purposes
			if (d6 > 0) {
				d = (int) Math.round(random.nextGaussian() * d6) + (3 * d6);
				d = Math.min(d, data.size() - 1);
				d = Math.max(d, 0);
			}
			else {
				d = random.nextInt(data.size());
			}
			if (p6 > 0) {
				p = (int) Math.round(random.nextGaussian() * p6) + (3 * p6);
				p = Math.min(p, purposes.size() - 1);
				p = Math.max(p, 0);
			}
			else {
				p = random.nextInt(purposes.size());
			}
			
			Flow flow = new Flow(actors.get(s), actors.get(t), data.get(d), purposes.get(p));
			this.flows.add(flow);
		}
	}
	
	private String createHierarchy(String type, TreeMap<String,Integer> map, int height, int span) {
		List<String> parents = new ArrayList<String>();
		String text = "";
		int counter = 1;
		
		// build the concept hierarchy
		parents.add(type + "0");
		map.put(type + "0", (int) Math.pow(height, span));
		
		// for each level in the height, define 'span' number of children
		for (int i = height - 1; i > 0; i--) {
			int last = parents.size();
			
			for (int j = 0; j < last; j++) {
				String parent = parents.get(j);
				
				for (int k = 0; k < span; k++) {
					String child = type + counter;
					counter++;
					text += "\t" + type + " " + parent + " > " + child + "\n";
					parents.add(child);
					
					// record the power for determining the number of concepts implicated
					map.put(child, (int) Math.pow(height, span));
				}
			}
			parents = parents.subList(last, parents.size());
		}
		return text;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	public static void main(String[] args) {
		PolicyGenerator g = new PolicyGenerator(new Random(123456789));
		String policyText = g.generatePolicy();
		System.err.println(policyText);
	}
	
	class Flow {
		String source;
		String target;
		String datum;
		String purpose;
		
		public Flow(String source, String target, String datum, String purpose) {
			this.source = source;
			this.target = target; // the target is used in transfers
			this.datum = datum;
			this.purpose = purpose;
		}
		
		public String toString() {
			return "<" + source + ", " + datum + ", " + purpose + ">";
		}
	}
	
	class Config {
		int actorSpan = 2;
		int actorHeight = 1;
		int dataSpan = 2;
		int dataHeight = 1;
		int purposeSpan = 2;
		int purposeHeight = 1;
		int flows = 4;
		int collectRights = 1;
		int useRights = 1;
		int transferRights = 1;
		int collectProhibitions = 1;
		double conflictLimit = 1.00;
		String namespace = "http://test";
		
		public String toString() {
			return actorHeight + ":" + actorSpan + ", " + dataHeight + ":" + dataSpan + ", " + purposeHeight + ":" + purposeSpan;
		}
	}
}
