package sjdb;

/**
 * This class represents a cartesian product operator.
 * @author nmg
 */
public class Product extends BinaryOperator {

	/**
	 * Create a new cartesian product operator
	 * @param left Left child operator
	 * @param right Right child operator
	 */
	public Product(Operator left, Operator right) {
		super(left, right);
	}
	
	/* (non-Javadoc)
	 * @see sjdb.BinaryOperator#accept(sjdb.OperatorVisitor)
	 */
	public void accept(PlanVisitor visitor) {
		super.accept(visitor);
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + this.getLeft().toString() + ") TIMES (" + 
				this.getRight().toString() + ")";
	}
}
