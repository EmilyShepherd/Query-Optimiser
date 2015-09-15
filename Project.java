package sjdb;

import java.util.List;
import java.util.Iterator;

/**
 * This class represents a Project operator.
 * @author nmg
 */
public class Project extends UnaryOperator {
	private List<Attribute> attributes;
	
	/**
	 * Create a new project operator.
	 * @param input Child operator
	 * @param attributes List of attributes to be projected
	 */
	public Project(Operator input, List<Attribute> attributes) {
		super(input);
		this.attributes = attributes;
	}

	/**
	 * Return the list of attributes projected by this operator
	 * @return List of attributes to be projected
	 */
	public List<Attribute> getAttributes() {
		return this.attributes;
	}
	
	/* (non-Javadoc)
	 * @see sjdb.UnaryOperator#accept(sjdb.OperatorVisitor)
	 */
	public void accept(PlanVisitor visitor) {
		// depth-first traversal - accept the 
		super.accept(visitor);
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String ret = "PROJECT [";
		Iterator<Attribute> iter = this.attributes.iterator();
		
		ret += iter.next().getName();
		
		while (iter.hasNext()) {
			ret += "," + iter.next().getName();
		}
		ret += "] (" + getInput().toString() + ")";
		
		return ret;
	}
}
