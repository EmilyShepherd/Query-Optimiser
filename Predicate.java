package sjdb;

/**
 * This class is used to represent the predicates associated with 
 * joins and select operators. Note that, while a string value is
 * required for predicates of the form attr=value, this values is 
 * only used by the toString() method; a future version of
 * Attribute which uses more expressive synopses may change this.
 * 
 * @author nmg
 */
public class Predicate {
	private Attribute leftAttribute;
	private Attribute rightAttribute;
	private String rightValue;

	/**
	 * Create a predicate of the form attr=attr
	 * @param left
	 * @param right
	 */
	public Predicate(Attribute left, Attribute right) {
		this.leftAttribute = left;
		this.rightAttribute = right;
	}

	/**
	 * Create a predicate of the form attr=value
	 * @param left
	 * @param value
	 */
	public Predicate(Attribute left, String value) {
		this.leftAttribute = left;
		this.rightValue = value;
	}

	/**
	 * Return true if this predicate is of the form attr=value
	 * @return
	 */
	public boolean equalsValue() {
		return this.rightValue != null;
	}
	
	/**
	 * Return ATTR for predicates of the form ATTR=attr or ATTR=value
	 * @return left attribute
	 */
	public Attribute getLeftAttribute() {
		return this.leftAttribute;
	}
	
	/**
	 * Return ATTR for predicates of the form attr=ATTR
	 * @return right attribute
	 */
	public Attribute getRightAttribute() {
		return this.rightAttribute;
	}
	
	/**
	 * Return VALUE for predicates of the form attr=VALUE
	 * @return right value
	 */
	public String getRightValue() {
		return this.rightValue;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (this.rightValue == null) {
			return this.leftAttribute.toString() + "=" + this.rightAttribute.toString(); 
		} else {
			return this.leftAttribute.toString() + "=\"" + this.rightValue + "\"";
		}
	}
}
