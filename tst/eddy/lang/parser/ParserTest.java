package eddy.lang.parser;

import java.io.StringReader;

import junit.framework.TestCase;
import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.Type;

public class ParserTest extends TestCase {

	public void test1_Rules() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental FROM customer FOR marketing\n" +
				"\tO USE customer-rental FROM customer\n" +
				"\tO USE customer-rental FOR marketing\n" +
				"\tP TRANSFER customer-rental.title FROM customer TO advertiser FOR billing\n" +
				"\tE TRANSFER customer-rental FROM customer FOR marketing\n";
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		Rule[] rule = policy.rules();
		assertEquals(5, rule.length);

		// test rule #0
		assertEquals(Modality.REFRAINMENT, rule[0].modality);
		assertEquals("COLLECT", rule[0].action.name);
		assertEquals(false, rule[0].only);

		// test the object role for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		assertEquals(Role.Type.OBJECT, role.type);
		assertEquals("", role.prefix);
		assertTrue(role.values.isSingle());
		Datum datum = (Datum) role.values.getValue();
		assertEquals("customer-rental", datum.name);

		// test the source role for rule #0
		role = rule[0].action.getRole(Role.Type.SOURCE);
		assertEquals(Role.Type.SOURCE, role.type);
		assertEquals("FROM", role.prefix);
		assertTrue(role.values.isSingle());
		Actor actor = (Actor) role.values.getValue();
		assertEquals("customer", actor.name);
		
		// test the purpose role for rule #0
		role = rule[0].action.getRole(Role.Type.PURPOSE);
		assertEquals(Role.Type.PURPOSE, role.type);
		assertEquals("FOR", role.prefix);
		assertTrue(role.values.isSingle());
		Purpose purpose = (Purpose) role.values.getValue();
		assertEquals("marketing", purpose.name);
		
		// test the default purpose role for rule #1
		role = rule[1].action.getRole(Role.Type.PURPOSE);
		assertEquals(Role.Type.PURPOSE, role.type);
		assertEquals("FOR", role.prefix);
		assertTrue(role.values.isSingle());
		purpose = (Purpose) role.values.getValue();
		assertEquals(Purpose.ANYTHING.name, purpose.name);
		
		// test the default source role for rule #2
		role = rule[2].action.getRole(Role.Type.SOURCE);
		assertEquals(Role.Type.SOURCE, role.type);
		assertEquals("FROM", role.prefix);
		assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		assertEquals(Actor.ANYONE.name, actor.name);
		
		// test the target role for rule #3
		role = rule[3].action.getRole(Role.Type.TARGET);
		assertEquals(Role.Type.TARGET, role.type);
		assertEquals("TO", role.prefix);
		assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		assertEquals("advertiser", actor.name);
		
		// test the target role for rule #3
		role = rule[3].action.getRole(Role.Type.TARGET);
		assertEquals(Role.Type.TARGET, role.type);
		assertEquals("TO", role.prefix);
		assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		assertEquals("advertiser", actor.name);
		
		// test the target role for rule #4
		role = rule[4].action.getRole(Role.Type.TARGET);
		assertEquals(Role.Type.TARGET, role.type);
		assertEquals("TO", role.prefix);
		assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		assertEquals(Actor.ANYONE.name, actor.name);
	}
	
	public void test2_Types() throws ParseException {
		String text = "SPEC HEADER\n" +
				"\tA customer < advertiser\n" +
				"\tD rental < record\n" +
				"\tP marketing > direct-marketing,oba\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental.title FROM customer FOR marketing\n";
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Type[] type = policy.types();
		assertEquals(4, type.length);
		
		// test actor type and disjoint
		assertEquals(Type.CLASS_ACTOR, type[0].type);
		assertEquals(Type.SUBCLASS, type[0].op);
		assertEquals("customer", type[0].lhs);
		assertEquals(1, type[0].rhs.length);
		assertEquals("advertiser", type[0].rhs[0]);
		
		// test datum type and less than
		assertEquals(Type.CLASS_DATUM, type[1].type);
		assertEquals(Type.SUBCLASS, type[1].op);
		assertEquals("rental", type[1].lhs);
		assertEquals(1, type[1].rhs.length);
		assertEquals("record", type[1].rhs[0]);
		
		// test purpose type and greater than
		assertEquals(Type.CLASS_PURPOSE, type[2].type);
		assertEquals(Type.SUPERCLASS, type[2].op);
		assertEquals("marketing", type[2].lhs);
		assertEquals(2, type[2].rhs.length);
		assertEquals("direct-marketing", type[2].rhs[0]);
		assertEquals("oba", type[2].rhs[1]);

		// test purpose type and greater than
		assertEquals(Type.CLASS_DATUM, type[3].type);
		assertEquals(Type.SUPERCLASS, type[3].op);
		assertEquals("customer-rental", type[3].lhs);
		assertEquals(1, type[3].rhs.length);
		assertEquals("title", type[3].rhs[0]);
		
	}
	
	public void test3_Exceptions() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental \\ customer-rental.title FROM customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer \\ valued-customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer FOR marketing \\ direct-marketing\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		assertEquals(3, rule.length);
		
		// test the object exception for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		assertEquals(Role.Type.OBJECT, role.type);
		assertEquals("", role.prefix);
		assertFalse(role.values.isSingle());
		assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		set = role.values.getLHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		Datum datum = (Datum) set.getValue();
		assertNotNull(datum);
		assertEquals("customer-rental", datum.name);
		set = role.values.getRHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		datum = (Datum) set.getValue();
		assertEquals("title", datum.name);
	}
	
	public void test4_ComplexExpressions() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental \\ customer-rental.title, customer-rental.date FROM customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer \\ valued-customer + preferred-customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer FOR marketing \\ direct-marketing \\ oba\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		assertEquals(3, rule.length);
		
		// test the object exception for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		assertEquals(Role.Type.OBJECT, role.type);
		assertEquals("", role.prefix);
		assertFalse(role.values.isSingle());
		assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		set = role.values.getLHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, set.type);
		Datum datum = (Datum) set.getValue();
		assertNotNull(datum);
		assertEquals("customer-rental", datum.name);
		set = role.values.getRHS();
		assertNotNull(set);
		assertFalse(set.isSingle());
		assertEquals(RoleValueSet.Type.UNION, set.type);
		RoleValueSet subSet;
		subSet = set.getLHS();
		assertNotNull(subSet);
		assertTrue(subSet.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		datum = (Datum) subSet.getValue();
		assertNotNull(datum);
		assertEquals("title", datum.name);
		subSet = set.getRHS();
		assertNotNull(subSet);
		assertTrue(subSet.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		assertTrue(subSet.isSingle());
		datum = (Datum) subSet.getValue();
		assertNotNull(datum);
		assertEquals("date", datum.name);
		
		// test the object exception for rule #0
		role = rule[1].action.getRole(Role.Type.SOURCE);
		assertEquals(Role.Type.SOURCE, role.type);
		assertEquals("FROM", role.prefix);
		assertFalse(role.values.isSingle());
		assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		set = role.values.getLHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, set.type);
		Actor actor = (Actor) set.getValue();
		assertNotNull(actor);
		assertEquals("customer", actor.name);
		set = role.values.getRHS();
		assertNotNull(set);
		assertFalse(set.isSingle());
		assertEquals(RoleValueSet.Type.INTERSECT, set.type);
		subSet = set.getLHS();
		assertNotNull(subSet);
		assertTrue(subSet.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		actor = (Actor) subSet.getValue();
		assertNotNull(actor);
		assertEquals("valued-customer", actor.name);
		subSet = set.getRHS();
		assertNotNull(subSet);
		assertTrue(subSet.isSingle());
		assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		assertTrue(subSet.isSingle());
		actor = (Actor) subSet.getValue();
		assertNotNull(actor);
		assertEquals("preferred-customer", actor.name);
	}
	
	// public void test3.5_Exceptions() test for "anyone \ actor"
	
	public void test5_Only() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR ONLY COLLECT customer.rental FROM customer FOR marketing\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		assertEquals(2, rule.length);
		
		// test the object role for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		assertEquals(Role.Type.OBJECT, role.type);
		assertEquals("", role.prefix);
		assertTrue(role.values.isSingle());
		Datum datum = (Datum) role.values.getValue();
		assertEquals("rental", datum.name);
		
		// test the source role for rule #0
		role = rule[0].action.getRole(Role.Type.SOURCE);
		assertEquals(Role.Type.SOURCE, role.type);
		assertEquals("FROM", role.prefix);
		assertTrue(role.values.isSingle());
		Actor actor = (Actor) role.values.getValue();
		assertEquals("customer", actor.name);
		
		// test the purpose role for rule #0
		role = rule[0].action.getRole(Role.Type.PURPOSE);
		assertEquals(Role.Type.PURPOSE, role.type);
		assertEquals("FOR", role.prefix);
		assertTrue(role.values.isSingle());
		Purpose purpose = (Purpose) role.values.getValue();
		assertEquals("marketing", purpose.name);
		
		// test the object role for rule #1
		role = rule[1].action.getRole(Role.Type.OBJECT);
		assertEquals(Role.Type.OBJECT, role.type);
		assertEquals("", role.prefix);
		assertTrue(role.values.isSingle());
		datum = (Datum) role.values.getValue();
		assertEquals("rental", datum.name);
		
		// test the source role for rule #1
		role = rule[1].action.getRole(Role.Type.SOURCE);
		assertEquals(Role.Type.SOURCE, role.type);
		assertEquals("FROM", role.prefix);
		assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		assertEquals("customer", actor.name);
		
		// test the purpose role for rule #1
		role = rule[1].action.getRole(Role.Type.PURPOSE);
		assertEquals(Role.Type.PURPOSE, role.type);
		assertEquals("FOR", role.prefix);
		assertFalse(role.values.isSingle());
		assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		
		set = role.values.getLHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		purpose = (Purpose) set.getValue();
		assertEquals("anything", purpose.name);
		set = role.values.getRHS();
		assertNotNull(set);
		assertTrue(set.isSingle());
		purpose = (Purpose) set.getValue();
		assertEquals("marketing", purpose.name);
	}
}
