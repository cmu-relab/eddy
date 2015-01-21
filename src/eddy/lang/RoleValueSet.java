package eddy.lang;

/**
 * Describes a logical relation between one or more {@link RoleValue} objects. This includes
 * the Description Logic union, intersection, and complement.
 * 
 * @author Travis Breaux
 *
 */

public abstract class RoleValueSet implements Cloneable {
	/**
	 * Describes the logical set type. This is one of: {@link #UNION}, {@link #INTERSECT},
	 * {@link #COMPLEMENT}, and {@link SINGLE}.
	 *  
	 * @author Travis Breaux
	 *
	 */
	public static enum Type { UNION, INTERSECT, COMPLEMENT, SINGLE };
	public final Type type;
	
	private RoleValueSet(Type type) {
		this.type = type;
	}
	public abstract boolean isSingle();
	public abstract RoleValueSet getLHS();
	public abstract RoleValueSet getRHS();
	public abstract RoleValue getValue();
	public abstract String toString();
	
	public RoleValue first() {
		RoleValueSet s = this;
		while (!s.isSingle()) {
			s = s.getLHS();
		}
		return s.getValue();
	}
	
	public abstract RoleValueSet clone();
	
	/**
	 * Describes a generic set of two {@link RoleValueSet} objects. This class is subclassed
	 * by typed sets to characterize the DL composition of non-atomic concepts.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class BinarySet extends RoleValueSet {
		public final RoleValueSet lhs, rhs;
		private BinarySet(Type type, RoleValueSet lhs, RoleValueSet rhs) {
			super(type);
			this.lhs = lhs;
			this.rhs = rhs;
		}
		private BinarySet(Type type, RoleValueSet lhs, RoleValue rhs) {
			super(type);
			this.lhs = lhs;
			this.rhs = new Singleton(rhs);
		}
		private BinarySet(Type type, RoleValue lhs, RoleValueSet rhs) {
			super(type);
			this.lhs = new Singleton(lhs);
			this.rhs = rhs;
		}
		private BinarySet(Type type, RoleValue lhs, RoleValue rhs) {
			super(type);
			this.lhs = new Singleton(lhs);
			this.rhs = new Singleton(rhs);
		}
		
		public BinarySet clone() {
			return new BinarySet(type, lhs.clone(), rhs.clone());
		}
		public boolean isSingle() {
			return false;
		}
		public RoleValueSet getLHS() {
			return lhs;
		}
		public RoleValueSet getRHS() {
			return rhs;
		}
		public RoleValue getValue() {
			return null;
		}
		public String toString() {
			String s = lhs.toString();
			
			switch (type) {
				case UNION: {
					s += ", ";
					break;
				}
				case INTERSECT: {
					s += " + ";
					break;
				}
				case COMPLEMENT: {
					s += " \\ ";
					break;
				}
				default: {
					s += " ? ";
				}
			}
			return s + rhs.toString();
		}
	}
	
	/**
	 * Describes the set complement of two {@link RoleValueSet} objects.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Complement extends BinarySet {
		public Complement(RoleValueSet lhs, RoleValueSet rhs) {
			super(Type.COMPLEMENT, lhs, rhs);
		}
		public Complement(RoleValueSet lhs, RoleValue rhs) {
			super(Type.COMPLEMENT, lhs, rhs);
		}
		public Complement(RoleValue lhs, RoleValueSet rhs) {
			super(Type.COMPLEMENT, lhs, rhs);
		}
		public Complement(RoleValue lhs, RoleValue rhs) {
			super(Type.COMPLEMENT, lhs, rhs);
		}
		public Complement clone() {
			return new Complement(lhs.clone(), rhs.clone());
		}
	}
	
	/**
	 * Describes the set intersection of two {@link RoleValueSet} objects.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Intersect extends BinarySet {
		public Intersect(RoleValueSet lhs, RoleValueSet rhs) {
			super(Type.INTERSECT, lhs, rhs);
		}
		public Intersect(RoleValueSet lhs, RoleValue rhs) {
			super(Type.INTERSECT, lhs, rhs);
		}
		public Intersect(RoleValue lhs, RoleValueSet rhs) {
			super(Type.INTERSECT, lhs, rhs);
		}
		public Intersect(RoleValue lhs, RoleValue rhs) {
			super(Type.INTERSECT, lhs, rhs);
		}
		public Intersect clone() {
			return new Intersect(lhs.clone(), rhs.clone());
		}
	}
	
	/**
	 * Describes the set union of two {@link RoleValueSet} objects.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Union extends BinarySet {
		public Union(RoleValueSet lhs, RoleValueSet rhs) {
			super(Type.UNION, lhs, rhs);
		}
		public Union(RoleValueSet lhs, RoleValue rhs) {
			super(Type.UNION, lhs, rhs);
		}
		public Union(RoleValue lhs, RoleValueSet rhs) {
			super(Type.UNION, lhs, rhs);
		}
		public Union(RoleValue lhs, RoleValue rhs) {
			super(Type.UNION, lhs, rhs);
		}
		public Union clone() {
			return new Union(lhs.clone(), rhs.clone());
		}
	}
	
	/**
	 * Describes the set containing one {@link RoleValueSet} object.
	 * 
	 * @author Travis Breaux
	 *
	 */
	
	public static class Singleton extends RoleValueSet {
		public final RoleValue value;
		public Singleton(RoleValue value) {
			super(Type.SINGLE);
			this.value = value;
		}
		public Singleton clone() {
			return new Singleton(value);
		}
		
		public boolean isSingle() {
			return true;
		}
		public RoleValueSet getLHS() {
			return null;
		}
		public RoleValueSet getRHS() {
			return null;
		}
		public RoleValue getValue() {
			return value;
		}
		public String toString() {
			return value.toString();
		}
	}
}