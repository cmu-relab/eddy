package eddy.lang.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import eddy.lang.Policy;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

/**
 * Provides an HTTP client for pulling a policy from a policy server.
 * 
 * @author Travis Breaux
 *
 */

public class AgentClient {

	public static Agent retrieve(URI uri) throws IOException {
		// open the http connection to the agent
		URL url = uri.toURL();
		HttpURLConnection connect = (HttpURLConnection) url.openConnection();
		connect.setRequestMethod("GET");
		
		// read the requested policy text
		BufferedReader in = new BufferedReader(new InputStreamReader((InputStream)connect.getContent()));
		StringBuffer buffer = new StringBuffer();
		String line;
		
		while ((line = in.readLine()) != null) {
			buffer.append(line + "\n");
		}
		connect.disconnect();
		
		// parse the policy text
		String text = buffer.toString();
		Policy policy = null;
		
		try {
			policy = new Parser().parse(new StringReader(text));
		} catch (ParseException e) {
			throw new IOException("Cannot parse policy text received from server");
		}
		
		Agent agent = new Agent(uri);
		agent.setPolicy(policy);		
		return agent;
	}
}
