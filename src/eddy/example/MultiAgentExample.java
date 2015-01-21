package eddy.example;

import java.io.File;
import java.io.FileReader;
import java.net.URI;

import eddy.lang.Actor;
import eddy.lang.Policy;
import eddy.lang.net.Agent;
import eddy.lang.net.Agent.Party;
import eddy.lang.net.AgentClient;
import eddy.lang.net.AgentServer;
import eddy.lang.net.ServiceMap;
import eddy.lang.net.ServiceMapReader;
import eddy.lang.parser.Parser;

/**
 * Demonstrates how to use the {@link Agent} class to serve policies.
 * 
 * @author Travis Breaux
 */

public class MultiAgentExample {

	public static void main(String[] args) throws Exception {
		final String exampleName = MultiAgentExample.class.getName();
		
		// parse the agent policies
		Parser parser = new Parser();
		Policy policy1 = parser.parse(new File("examples/example.stream1.policy"));
		Policy policy2 = parser.parse(new File("examples/example.stream2.policy"));
		
		// parse the thesaurus
		ServiceMap thesaurus = ServiceMapReader.read(new FileReader(new File("examples/example.thesaurus")));
		
		// create the agents and party relationships
		Agent agent1 = new Agent(URI.create("http://localhost:9001/agent1"));
		agent1.setPolicy(policy1);
		Agent agent2 = new Agent(URI.create("http://localhost:9002/agent2"));
		agent2.setPolicy(policy2);
		
		// create the party relationships
		agent1.add(new Party(Party.Direction.OUT, new Actor("billing-service"), agent2.uri, thesaurus));
		agent2.add(new Party(Party.Direction.OUT, new Actor("retailer"), agent1.uri, thesaurus));
		
		// create the agent servers
		AgentServer server1 = new AgentServer(agent1);
		AgentServer server2 = new AgentServer(agent2);
		
		System.err.print(exampleName + ": Starting agent server..");
		Thread thread1 = new Thread(server1);
		Thread thread2 = new Thread(server2);
		thread1.start();
		thread2.start();
		while (!thread1.isAlive() && !thread2.isAlive()) {
			Thread.sleep(1000);
		}
		System.err.println(". started");
		
		System.err.println(exampleName + ": Retrieving policies");
		agent1 = AgentClient.retrieve(agent1.uri);
		agent2 = AgentClient.retrieve(agent2.uri);
		
		server1.stop();
		server2.stop();
	}
}
