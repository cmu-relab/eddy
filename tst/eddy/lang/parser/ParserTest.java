package eddy.lang.parser;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import eddy.lang.Actor;
import eddy.lang.Datum;
import eddy.lang.Policy;
import eddy.lang.Purpose;
import eddy.lang.Role;
import eddy.lang.RoleValueSet;
import eddy.lang.Rule;
import eddy.lang.Rule.Modality;
import eddy.lang.Type;
import eddy.lang.parser.ParseException;
import eddy.lang.parser.Parser;

public class ParserTest {

	@Test
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
		Assert.assertEquals(5, rule.length);

		// test rule #0
		Assert.assertEquals(Modality.REFRAINMENT, rule[0].modality);
		Assert.assertEquals("COLLECT", rule[0].action.name);
		Assert.assertEquals(false, rule[0].only);

		// test the object role for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		Assert.assertEquals(Role.Type.OBJECT, role.type);
		Assert.assertEquals("", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Datum datum = (Datum) role.values.getValue();
		Assert.assertEquals("customer-rental", datum.name);

		// test the source role for rule #0
		role = rule[0].action.getRole(Role.Type.SOURCE);
		Assert.assertEquals(Role.Type.SOURCE, role.type);
		Assert.assertEquals("FROM", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Actor actor = (Actor) role.values.getValue();
		Assert.assertEquals("customer", actor.name);
		
		// test the purpose role for rule #0
		role = rule[0].action.getRole(Role.Type.PURPOSE);
		Assert.assertEquals(Role.Type.PURPOSE, role.type);
		Assert.assertEquals("FOR", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Purpose purpose = (Purpose) role.values.getValue();
		Assert.assertEquals("marketing", purpose.name);
		
		// test the default purpose role for rule #1
		role = rule[1].action.getRole(Role.Type.PURPOSE);
		Assert.assertEquals(Role.Type.PURPOSE, role.type);
		Assert.assertEquals("FOR", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		purpose = (Purpose) role.values.getValue();
		Assert.assertEquals(Purpose.ANYTHING.name, purpose.name);
		
		// test the default source role for rule #2
		role = rule[2].action.getRole(Role.Type.SOURCE);
		Assert.assertEquals(Role.Type.SOURCE, role.type);
		Assert.assertEquals("FROM", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		Assert.assertEquals(Actor.ANYONE.name, actor.name);
		
		// test the target role for rule #3
		role = rule[3].action.getRole(Role.Type.TARGET);
		Assert.assertEquals(Role.Type.TARGET, role.type);
		Assert.assertEquals("TO", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		Assert.assertEquals("advertiser", actor.name);
		
		// test the target role for rule #3
		role = rule[3].action.getRole(Role.Type.TARGET);
		Assert.assertEquals(Role.Type.TARGET, role.type);
		Assert.assertEquals("TO", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		Assert.assertEquals("advertiser", actor.name);
		
		// test the target role for rule #4
		role = rule[4].action.getRole(Role.Type.TARGET);
		Assert.assertEquals(Role.Type.TARGET, role.type);
		Assert.assertEquals("TO", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		Assert.assertEquals(Actor.ANYONE.name, actor.name);
	}

	@Test
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
		Assert.assertEquals(4, type.length);
		
		// test actor type and disjoint
		Assert.assertEquals(Type.CLASS_ACTOR, type[0].type);
		Assert.assertEquals(Type.SUBCLASS, type[0].op);
		Assert.assertEquals("customer", type[0].lhs);
		Assert.assertEquals(1, type[0].rhs.length);
		Assert.assertEquals("advertiser", type[0].rhs[0]);
		
		// test datum type and less than
		Assert.assertEquals(Type.CLASS_DATUM, type[1].type);
		Assert.assertEquals(Type.SUBCLASS, type[1].op);
		Assert.assertEquals("rental", type[1].lhs);
		Assert.assertEquals(1, type[1].rhs.length);
		Assert.assertEquals("record", type[1].rhs[0]);
		
		// test purpose type and greater than
		Assert.assertEquals(Type.CLASS_PURPOSE, type[2].type);
		Assert.assertEquals(Type.SUPERCLASS, type[2].op);
		Assert.assertEquals("marketing", type[2].lhs);
		Assert.assertEquals(2, type[2].rhs.length);
		Assert.assertEquals("direct-marketing", type[2].rhs[0]);
		Assert.assertEquals("oba", type[2].rhs[1]);

		// test purpose type and greater than
		Assert.assertEquals(Type.CLASS_DATUM, type[3].type);
		Assert.assertEquals(Type.SUPERCLASS, type[3].op);
		Assert.assertEquals("customer-rental", type[3].lhs);
		Assert.assertEquals(1, type[3].rhs.length);
		Assert.assertEquals("title", type[3].rhs[0]);
		
	}

	@Test
	public void test3_Exceptions() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental \\ customer-rental.title FROM customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer \\ valued-customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer FOR marketing \\ direct-marketing\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		Assert.assertEquals(3, rule.length);
		
		// test the object exception for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		Assert.assertEquals(Role.Type.OBJECT, role.type);
		Assert.assertEquals("", role.prefix);
		Assert.assertFalse(role.values.isSingle());
		Assert.assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		set = role.values.getLHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		Datum datum = (Datum) set.getValue();
		Assert.assertNotNull(datum);
		Assert.assertEquals("customer-rental", datum.name);
		set = role.values.getRHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		datum = (Datum) set.getValue();
		Assert.assertEquals("title", datum.name);
	}

	@Test
	public void test4_ComplexExpressions() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR COLLECT customer-rental \\ customer-rental.title, customer-rental.date FROM customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer \\ valued-customer + preferred-customer FOR marketing\n" +
				"\tR COLLECT customer-rental FROM customer FOR marketing \\ direct-marketing \\ oba\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		Assert.assertEquals(3, rule.length);
		
		// test the object exception for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		Assert.assertEquals(Role.Type.OBJECT, role.type);
		Assert.assertEquals("", role.prefix);
		Assert.assertFalse(role.values.isSingle());
		Assert.assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		set = role.values.getLHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, set.type);
		Datum datum = (Datum) set.getValue();
		Assert.assertNotNull(datum);
		Assert.assertEquals("customer-rental", datum.name);
		set = role.values.getRHS();
		Assert.assertNotNull(set);
		Assert.assertFalse(set.isSingle());
		Assert.assertEquals(RoleValueSet.Type.UNION, set.type);
		RoleValueSet subSet;
		subSet = set.getLHS();
		Assert.assertNotNull(subSet);
		Assert.assertTrue(subSet.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		datum = (Datum) subSet.getValue();
		Assert.assertNotNull(datum);
		Assert.assertEquals("title", datum.name);
		subSet = set.getRHS();
		Assert.assertNotNull(subSet);
		Assert.assertTrue(subSet.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		Assert.assertTrue(subSet.isSingle());
		datum = (Datum) subSet.getValue();
		Assert.assertNotNull(datum);
		Assert.assertEquals("date", datum.name);
		
		// test the object exception for rule #0
		role = rule[1].action.getRole(Role.Type.SOURCE);
		Assert.assertEquals(Role.Type.SOURCE, role.type);
		Assert.assertEquals("FROM", role.prefix);
		Assert.assertFalse(role.values.isSingle());
		Assert.assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		set = role.values.getLHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, set.type);
		Actor actor = (Actor) set.getValue();
		Assert.assertNotNull(actor);
		Assert.assertEquals("customer", actor.name);
		set = role.values.getRHS();
		Assert.assertNotNull(set);
		Assert.assertFalse(set.isSingle());
		Assert.assertEquals(RoleValueSet.Type.INTERSECT, set.type);
		subSet = set.getLHS();
		Assert.assertNotNull(subSet);
		Assert.assertTrue(subSet.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		actor = (Actor) subSet.getValue();
		Assert.assertNotNull(actor);
		Assert.assertEquals("valued-customer", actor.name);
		subSet = set.getRHS();
		Assert.assertNotNull(subSet);
		Assert.assertTrue(subSet.isSingle());
		Assert.assertEquals(RoleValueSet.Type.SINGLE, subSet.type);
		Assert.assertTrue(subSet.isSingle());
		actor = (Actor) subSet.getValue();
		Assert.assertNotNull(actor);
		Assert.assertEquals("preferred-customer", actor.name);
	}
	
	// public void test3.5_Exceptions() test for "anyone \ actor"
	@Test
	public void test5_Only() throws ParseException {
		String text = "SPEC HEADER\n" +
				"SPEC POLICY\n" +
				"\tR ONLY COLLECT customer.rental FROM customer FOR marketing\n";
		
		Parser parser = new Parser();
		Policy policy = parser.parse(new StringReader(text));
		
		Rule[] rule = policy.rules();
		Assert.assertEquals(2, rule.length);
		
		// test the object role for rule #0
		Role role = rule[0].action.getRole(Role.Type.OBJECT);
		Assert.assertEquals(Role.Type.OBJECT, role.type);
		Assert.assertEquals("", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Datum datum = (Datum) role.values.getValue();
		Assert.assertEquals("rental", datum.name);
		
		// test the source role for rule #0
		role = rule[0].action.getRole(Role.Type.SOURCE);
		Assert.assertEquals(Role.Type.SOURCE, role.type);
		Assert.assertEquals("FROM", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Actor actor = (Actor) role.values.getValue();
		Assert.assertEquals("customer", actor.name);
		
		// test the purpose role for rule #0
		role = rule[0].action.getRole(Role.Type.PURPOSE);
		Assert.assertEquals(Role.Type.PURPOSE, role.type);
		Assert.assertEquals("FOR", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		Purpose purpose = (Purpose) role.values.getValue();
		Assert.assertEquals("marketing", purpose.name);
		
		// test the object role for rule #1
		role = rule[1].action.getRole(Role.Type.OBJECT);
		Assert.assertEquals(Role.Type.OBJECT, role.type);
		Assert.assertEquals("", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		datum = (Datum) role.values.getValue();
		Assert.assertEquals("rental", datum.name);
		
		// test the source role for rule #1
		role = rule[1].action.getRole(Role.Type.SOURCE);
		Assert.assertEquals(Role.Type.SOURCE, role.type);
		Assert.assertEquals("FROM", role.prefix);
		Assert.assertTrue(role.values.isSingle());
		actor = (Actor) role.values.getValue();
		Assert.assertEquals("customer", actor.name);
		
		// test the purpose role for rule #1
		role = rule[1].action.getRole(Role.Type.PURPOSE);
		Assert.assertEquals(Role.Type.PURPOSE, role.type);
		Assert.assertEquals("FOR", role.prefix);
		Assert.assertFalse(role.values.isSingle());
		Assert.assertEquals(RoleValueSet.Type.COMPLEMENT, role.values.type);
		RoleValueSet set;
		
		set = role.values.getLHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		purpose = (Purpose) set.getValue();
		Assert.assertEquals("anything", purpose.name);
		set = role.values.getRHS();
		Assert.assertNotNull(set);
		Assert.assertTrue(set.isSingle());
		purpose = (Purpose) set.getValue();
		Assert.assertEquals("marketing", purpose.name);
	}
}
