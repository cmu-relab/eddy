package eddy.lang.net;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import eddy.lang.Actor;
import eddy.lang.Policy;
import eddy.lang.net.Agent.Party;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

/**
 * Reads an {@link Agent} from a file, including their {@link Policy}.
 * 
 * @author Travis Breaux
 *
 */

public class AgentReader {
	
	public static Agent read(String filename) {
		Agent agent = null;
		BufferedReader in;
		
		try {
			in = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.trim().length() == 0) {
					continue;
				}
				
				String[] part = line.split("\\s+");
				
				if (part[0].equals("local")) {
					Policy policy  = readPolicy(part[1]);
					URI uri = URI.create(part[1]);
					agent = new Agent(uri);
					agent.setPolicy(policy);
				}
				else if (part[0].equals("remote")) {
					URI uri = URI.create(part[1]);
					agent = AgentClient.retrieve(uri);
				}
				else if (part[0].equals("recv")) {
					Actor role = new Actor(part[1]);
					URI uri = URI.create(part[2]);
					ServiceMap map = ServiceMapReader.read(new FileReader(part[3]));
					Party party = new Agent.Party(Agent.Party.Direction.IN, role, uri, map);
					agent.add(party);
				}
				else if (part[0].equals("send")) {
					Actor role = new Actor(part[1]);
					URI uri = URI.create(part[2]);
					ServiceMap map = ServiceMapReader.read(new FileReader(part[3]));
					Party party = new Agent.Party(Agent.Party.Direction.OUT, role, uri, map);
					agent.add(party);
				}
			}
			in.close();
			
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read agent specification: " + filename);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error reading agent specification: " + filename);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return agent;
	}
	
	private static Policy readPolicy(String filename) {
		BufferedReader in;
		StringBuffer buffer = new StringBuffer();
		try {
			in = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = in.readLine()) != null) {
				buffer.append(line + "\n");
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		Policy policy = null;
		try {
			policy = new Parser().parse(new StringReader(buffer.toString()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return policy;
	}
}
