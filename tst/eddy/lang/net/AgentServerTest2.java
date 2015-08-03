package eddy.lang.net;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import eddy.lang.Policy;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

public class AgentServerTest2 {
	
	@Test
	public void test1_AgentServer() throws ParseException, IOException {
		// create the policy
		String text = "SPEC HEADER\n" +
			"SPEC POLICY\n" +
			"\tR COLLECT customer-rental FROM customer FOR marketing\n" +
			"\tO USE customer-rental FROM customer\n" +
			"\tO USE customer-rental FOR marketing\n" +
			"\tP TRANSFER customer-rental.title FROM customer TO advertiser FOR billing\n" +
			"\tE TRANSFER customer-rental FROM customer FOR marketing\n";
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		// create the agent
		Agent agent = new Agent(URI.create("http://localhost:9000/agent"));
		agent.setPolicy(policy);
		
		// create the agent server
		AgentServer server = new AgentServer(agent);
		Thread thread = new Thread(server);
		thread.start();

		while (!thread.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Agent agent2 = AgentClient.retrieve(agent.uri);
		Assert.assertEquals(agent2.toString(), policy.toString());
	}
}
