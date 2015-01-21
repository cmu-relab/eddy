package eddy.lang.net;

import java.io.Reader;
import java.net.URI;
import java.util.InputMismatchException;
import java.util.Scanner;

import eddy.lang.Actor;
import eddy.lang.Type;

/**
 * Reads a service map that aligns terminology between two {@link eddy.lang.Policy} objects.
 * 
 * @author Travis Breaux
 *
 */
public class ServiceMapReader {

	public static ServiceMap read(Reader reader) {
		Scanner scanner = new Scanner(reader);
		
		// read first agent's namespace and role constraint
		scanner.next("\\S+"); // NS1
		String ns1 = scanner.next("\\S+");
		URI uri1 = URI.create(ns1);
		String r1 = scanner.next("\\S+");
		Actor role1 = new Actor(r1);
		scanner.nextLine();

		// read second agent's namespace and role constraint
		scanner.next("\\S+"); // NS2
		String ns2 = scanner.next("\\S+");
		URI uri2 = URI.create(ns2);
		String r2 = scanner.next("\\S+");
		Actor role2 = new Actor(r2);
		scanner.nextLine();
		
		// initialize the service map
		ServiceMap map = new ServiceMap(uri1, role1, uri2, role2);

		int line = 3;
		// read all terminology mappings and add to the map
		try {
			while (scanner.hasNext()) {
				char type = scanner.next("\\S+").charAt(0);
				if (type == '#') {
					scanner.nextLine();
					continue;
				}
				
				int typeCode = -1;
				switch (type) {
					case 'A':
						typeCode = Type.CLASS_ACTOR;
						break;
					case 'D':
						typeCode = Type.CLASS_DATUM;
						break;
					case 'P':
						typeCode = Type.CLASS_PURPOSE;
						break;
				}

				char op;
				String term1, term2;
				
				try {
					term1 = scanner.next("\\S+");
					op = scanner.next("\\S+").charAt(0);
					term2 = scanner.next("\\S+");
					scanner.nextLine();
					line++;
					
				} catch (Exception e) {
					System.err.println("Error reading service map at line " + line);
					e.printStackTrace();
					continue;
				}
				
				int opCode = -1;
				switch (op) {
					case '=':
						opCode = Type.EQUIVALENT;
						break;
					case '>':
						opCode = Type.SUPERCLASS;
						break;
					case '<':
						opCode = Type.SUBCLASS;
						break;
					case '\\':
						opCode = Type.DISJOINT;
						break;
				}
				
				Type t = new Type(typeCode, term1, opCode, new String[] { term2 });
				map.add(t);
			}
		}
		catch (InputMismatchException e) {
			System.err.println("Error reading service map at line " + line);
			scanner.close();
			throw e;
		}
		scanner.close();
		
		return map;
	}
}
