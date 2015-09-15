package sjdb;

/**
 * This class represents a join operator.
 * @author nmg
 */
public class Join extends BinaryOperator {
	private Predicate predicate;
	/**
	 * Create a new join operator.
	 * @param left Left child operator
	 * @param right Right child operator
	 * @param predicate Join predicate
	 */
	public Join(Operator left, Operator right, Predicate predicate) {
		super(left, right);
		this.predicate = predicate;
	}

	public Predicate getPredicate() {
		return this.predicate;
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
		return "(" + this.getLeft().toString() + ") JOIN [" + 
				this.getPredicate().toString() +
				"] (" + this.getRight().toString() + ")";
	}
	
}
