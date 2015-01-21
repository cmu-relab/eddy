package eddy.lang.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eddy.lang.Policy;

/**
 * Provides an HTTP server for pushing a policy to a policy reader.
 * 
 * @author Travis Breaux
 *
 */

public class AgentServer implements Runnable {
	private final int requestLimit = 100;
	private final Agent agent;
	private final HttpServer server;
	private int port;
	
	public AgentServer(Agent agent) throws IOException {
		this.agent = agent;
		this.port = this.agent.uri.getPort();
		
		InetSocketAddress addr = new InetSocketAddress(port);
		server = HttpServer.create(addr, requestLimit);
		server.createContext("/", new PolicyHttpHandler(agent.getPolicy()));
	}
	
	public void run() {
		System.err.println("Starting policy server on port " + port);
		server.start();
	}
	
	public void stop() {
		System.err.println("Stopping policy server on port " + port);
		server.stop(0);
	}
	
	public Agent getAgent() {
		return agent;
	}
	
	class PolicyHttpHandler implements HttpHandler {
		private String responseBody;
		
		public PolicyHttpHandler(Policy policy) {
			this.responseBody = policy.toString();
		}
		
		public void handle(HttpExchange ex) {
			ex.getRequestMethod();
			ex.getRequestHeaders();
			System.err.println("Received request from " + ex.getRemoteAddress());
			
			// read the request body, but ignore
			BufferedReader in = new BufferedReader(new InputStreamReader(ex.getRequestBody()));
			try {
				while (in.readLine() != null) {
					continue;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// send the policy with response code 200 OK
			Headers headers = ex.getResponseHeaders();
			headers.add("Content-Type", "text/plain");
			try {
				ex.sendResponseHeaders(200, responseBody.length());
				PrintWriter out = new PrintWriter(ex.getResponseBody());
				out.write(responseBody);
				out.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
