package eddy.lang.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.TreeMap;

import eddy.lang.Role;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.analysis.Tracer.Flow;
import eddy.lang.analysis.CrossFlowTracer.CrossFlow;

/**
 * Exports the flows within and across policies in GraphML. This class accepts the list
 * of {@link CrossFlowTracer.CrossFlow} objects and generates a GraphML file.
 * 
 * @author Travis Breaux
 *
 */

public class CrossFlowGrapher {
	private Writer out;
	private final TreeMap<URI,String> agentLabel = new TreeMap<URI,String>();
	private final TreeMap<URI,ArrayList<Rule>> agentRules = new TreeMap<URI,ArrayList<Rule>>();
	private final TreeMap<String,String> nodeID = new TreeMap<String,String>();
	private boolean writeGroups = true;
	private int edgeCounter = 0;
	private int nodeCounter = 0;
	private boolean objectEdgeOnly = true;
	
	private String getAgentID(URI uri) {
		String id = agentLabel.get(uri);
		if (id == null) {
			//id = "agent" + agentLabel.size();
			id = uri.getPath().toString();
			agentLabel.put(uri, id);
		}
		return id;
	}
	
	private String getEdgeColor(Flow.Mode mode) {
		if (mode == Flow.Mode.EXACTFLOW) {
			return "#000000";
		}
		else if (mode == Flow.Mode.UNDERFLOW) {
			return "#CC0000";
		}
		else if (mode == Flow.Mode.OVERFLOW) {
			return "#00CC00";
		}
		else {
			return "#000000";
		}
	}
	
	private String getEdgeType(Flow.Mode mode) {
		if (mode == null) {
			return "dotted";
		}
		else {
			return "solid";
		}
	}
	
	private String getNodeColor(Modality modality) {
		if (modality.equals(Modality.PERMISSION)) {
			return "#CCFFCC";
		}
		else if (modality.equals(Modality.OBLIGATION)) {
			return "#FFFFCC";
		}
		else if (modality.equals(Modality.REFRAINMENT)) {
			return "#FF9999";
		}
		else if (modality.equals(Modality.EXCLUSION)) {
			return "#99CCFF";
		}
		else {
			return "#FFCC99";
		}
	}
	
	private ArrayList<Rule> getRuleList(URI uri) {
		ArrayList<Rule> list = agentRules.get(uri);
		if (list == null) {
			list = new ArrayList<Rule>();
			agentRules.put(uri, list);
		}
		return list;
	}
	
	public void graph(ArrayList<CrossFlow> flows, File file) throws IOException {
		this.out = new FileWriter(file);
		this.agentLabel.clear();
		
		writeHeader();

		// create all the rule nodes
		
		// start by sorting all the rules by agent, to create policy groups
		ArrayList<Rule> rules;
		for (CrossFlow flow : flows) {
			rules = getRuleList(flow.sourceURI);
			rules.add(flow.source);
			rules = getRuleList(flow.targetURI);
			rules.add(flow.target);
		}
		
		for (URI uri : agentRules.keySet()) {
			String id = getAgentID(uri);
			rules = agentRules.get(uri);
			writeGroup(id, rules);
		}
		
		// create all the rule nodes
		for (CrossFlow flow : flows) {
			String sourceLabel = getAgentID(flow.sourceURI) + flow.source.id;
			String targetLabel = getAgentID(flow.targetURI) + flow.target.id;
			String sourceID = nodeID.get(sourceLabel);
			String targetID = nodeID.get(targetLabel);
			
			Flow.Mode mode = flow.modes.get(Role.Type.OBJECT);
			writeEdge(sourceID, targetID, getEdgeColor(mode), getEdgeType(mode));
			
			/*
			if (flow.sourceMode == null && flow.targetMode == null) {
				Flow.Mode mode = flow.modes.get(Role.Type.OBJECT);
				writeEdge(sourceID, targetID, getEdgeColor(mode), getEdgeType(mode));
			}
			else {
				/*
				String label = "c" + interLink++;
				String interID = writeNode(label, "#FFCC99");
				writeEdge(sourceID, interID, getEdgeColor(flow.sourceMode), "dotted");
				writeEdge(interID, targetID, getEdgeColor(flow.targetMode), "dotted");
				*/
			/*
				Flow.Mode mode = flow.modes.get(Role.Type.OBJECT);
				writeEdge(sourceID, targetID, getEdgeColor(mode), "solid");
			}
			*/

			if (!objectEdgeOnly) {
				mode = flow.modes.get(Role.Type.SOURCE);
				writeEdge(sourceID, targetID, getEdgeColor(mode), "dotted");
				mode = flow.modes.get(Role.Type.PURPOSE);
				writeEdge(sourceID, targetID, getEdgeColor(mode), "dashed_dotted");
			}
		}
		
		writeFooter();
		out.flush();
	}
	
	private void writeEdge(String sourceID, String targetID, String color, String style) throws IOException {
		String id = "e" + (edgeCounter++);
		out.write(
			"<edge id=\"" + id
			+ "\" source=\"" + sourceID
			+ "\" target=\"" + targetID + "\">\n");
		
		String source = "none";
		String target = "standard";
		
		out.write(
			"<data key=\"d7\">" +
				"<y:PolyLineEdge>" +
					"<y:Path sx=\"0.0\" sy=\"0.0\" tx=\"0.0\" ty=\"0.0\"/>" +
					"<y:LineStyle color=\"" + color + "\" type=\"" + style + "\" width=\"1.0\"/>" +
					"<y:Arrows source=\"" + source + "\" target=\"" + target + "\"/>" +
					"<y:BendStyle smoothed=\"false\"/>" +
				"</y:PolyLineEdge>" +
			"</data>"
		);
		
		out.write("</edge>\n");
	}
	
	private void writeFooter() throws IOException {
		out.write(
				"</graph>\n"
				+ "</graphml>\n");
	}
	
	private void writeGroup(String id, ArrayList<Rule> rules) throws IOException {
		String label = id;
		
		if (rules.size() > 0) {
			// write the group node description
			if (writeGroups) {
				out.write("<node id=\"" + id + "\" yfiles.foldertype=\"group\">" +
						"<data key=\"d3\">" +
						"<y:ProxyAutoBoundsNode>" +
						"<y:Realizers active=\"0\">" +
						"<y:GroupNode>" +
						"<y:Geometry height=\"177.83203125\" width=\"343.5\" x=\"158.5\" y=\"243.833984375\"/>" +
						"<y:Fill color=\"#CAECFF84\" transparent=\"false\"/>" +
						"<y:BorderStyle color=\"#666699\" type=\"dotted\" width=\"1.0\"/>" +
						"<y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#99CCFF\" borderDistance=\"0.0\"" +
						" fontFamily=\"Dialog\"" +
						" fontSize=\"15\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"21.666015625\" modelName=\"internal\"" +
						" modelPosition=\"t\" textColor=\"#000000\" visible=\"true\" width=\"343.5\"" +
						" x=\"0.0\" y=\"0.0\">" + label + "</y:NodeLabel>" +
						"<y:Shape type=\"roundrectangle\"/>" +
						"<y:State closed=\"false\" innerGraphDisplayEnabled=\"false\"/>" +
						"<y:Insets bottom=\"15\" bottomF=\"15.0\" left=\"15\" leftF=\"15.0\" right=\"15\" rightF=\"15.0\" top=\"15\" topF=\"15.0\"/>" +
						"<y:BorderInsets bottom=\"15\" bottomF=\"15.0\" left=\"65\" leftF=\"65.0\" right=\"36\" rightF=\"36.0\" top=\"0\" topF=\"0.0\"/>" +
						"</y:GroupNode>" +
						"<y:GroupNode>" +
						"<y:Geometry height=\"83.75\" width=\"92.5\" x=\"331.5\" y=\"243.833984375\"/>" +
						"<y:Fill color=\"#CAECFF84\" transparent=\"false\"/>" +
						"<y:BorderStyle color=\"#666699\" type=\"dotted\" width=\"1.0\"/>" +
						"<y:NodeLabel alignment=\"right\" autoSizePolicy=\"node_width\" backgroundColor=\"#99CCFF\" borderDistance=\"0.0\"" +
						" fontFamily=\"Dialog\" fontSize=\"15\" fontStyle=\"plain\" hasLineColor=\"false\" height=\"21.666015625\"" +
						" modelName=\"internal\" modelPosition=\"t\" textColor=\"#000000\" visible=\"true\" width=\"92.5\"" +
						" x=\"0.0\" y=\"0.0\">" + id + "</y:NodeLabel>"  +
						"<y:Shape type=\"roundrectangle\"/>" +
						"<y:State closed=\"true\" innerGraphDisplayEnabled=\"false\"/>" +
						"<y:Insets bottom=\"15\" bottomF=\"15.0\" left=\"15\" leftF=\"15.0\" right=\"15\" rightF=\"15.0\" top=\"15\" topF=\"15.0\"/>" +
						"<y:BorderInsets bottom=\"0\" bottomF=\"0.0\" left=\"0\" leftF=\"0.0\" right=\"0\" rightF=\"0.0\" top=\"0\" topF=\"0.0\"/>" +
						"</y:GroupNode>" +
						"</y:Realizers>" +
						"</y:ProxyAutoBoundsNode>" +
						"</data>" +
						"<graph edgedefault=\"directed\" id=\"" + id + ":\">");
			}
			
			// write the rules i
			for (Rule r : rules) {
				writeNode(id + r.id, getNodeColor(r.modality));
			}
		}
		
		if (rules.size() > 0 && writeGroups) {
			out.write("</graph>\n");
			out.write("</node>\n");
		}
	}
	
	private void writeHeader() throws IOException {
		out.write(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n"
			+ "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			//+ "\txsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">\n"
			+ "\txmlns:y=\"http://www.yworks.com/xml/graphml\"\n"
			+ "\txmlns:yed=\"http://www.yworks.com/xml/yed/3\"\n"
			+ "\txsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">\n"
		);
		out.write(
			"<!--Created by yFiles for Java 2.7-->\n"
			+ "<key for=\"graphml\" id=\"d0\" yfiles.type=\"resources\"/>\n"
			+ "<key attr.name=\"url\" attr.type=\"string\" for=\"node\" id=\"d1\"/>\n"
			+ "<key attr.name=\"description\" attr.type=\"string\" for=\"node\" id=\"d2\"/>\n"
			+ "<key for=\"node\" id=\"d3\" yfiles.type=\"nodegraphics\"/>\n"
			+ "<key attr.name=\"Description\" attr.type=\"string\" for=\"graph\" id=\"d4\">\n"
			+ "  <default/>\n"
			+ "</key>\n"
			+ "<key attr.name=\"url\" attr.type=\"string\" for=\"edge\" id=\"d5\"/>\n"
			+ "<key attr.name=\"description\" attr.type=\"string\" for=\"edge\" id=\"d6\"/>\n"
			+ "<key for=\"edge\" id=\"d7\" yfiles.type=\"edgegraphics\"/>\n"
		);
		out.write(
				"<graph id=\"G\" edgedefault=\"directed\">\n"
		);
	}
	
	private String writeNode(String label, String color) throws IOException {
		String id = nodeID.get(label);
		if (id == null) {
			id = "n" + (nodeCounter++);
			nodeID.put(label, id);
		}
		out.write("<node id=\"" + id + "\">\n");
		
		double[] geo = null;
		String shape = null;
		String mName = "internal";
		String mPos = "c";
		String text = label;
		
		
		geo = new double[] { 30, 60 };
		shape = "roundrectangle";
		
		out.write("  <data key=\"d2\" />\n");
		out.write("  <data key=\"d3\">\n");
		out.write("    <y:ShapeNode>\n");
		out.write("      <y:Geometry height=\"" + geo[0] + "\" width=\"" + geo[1] + "\" x=\"0.0\" y=\"0.0\" />\n");
		out.write("      <y:Fill color=\"" + color + "\" transparent=\"false\"/>\n");
        out.write("      <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>\n");
		out.write("      <y:NodeLabel "
				+ "alignment=\"center\" "
				+ "autoSizePolicy=\"content\" "
				+ "fontFamily=\"Dialog\" "
				+ "fontSize=\"12\" "
				+ "fontStyle=\"plain\" "
				+ "hasBackgroundColor=\"false\" "
				+ "hasLineColor=\"false\" "
				+ "height=\"18.1328125\" "
				+ "modelName=\"" + mName + "\" "
				+ "modelPosition=\"" + mPos + "\" "
				+ "textColor=\"#000000\" "
				+ "visible=\"true\" "
				+ "width=\"42.0625\" "
				+ "x=\"0.0\" y=\"0.0\">" + text + "</y:NodeLabel>\n");
		out.write("      <y:Shape type=\"" + shape + "\"/>\n");
		out.write("    </y:ShapeNode>\n");
		out.write("  </data>\n");
		out.write("</node>\n");
		return id;
	}
}
