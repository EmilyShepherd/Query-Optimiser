package sjdb;

/**
 * This is an interface for a visitor class that performs a depth-first,
 * left-to-right traversal of a query plan.
 * @author nmg
 */
public interface PlanVisitor {
	/**
	 * Visit a Scan operator.
	 * @param op Scan operator to be visited
	 */
	public void visit(Scan op);
	/**
	 * Visit a Project operator.
	 * @param op Project operator to be visited
	 */
	public void visit(Project op);
	/**
	 * Visit a Select operator.
	 * @param op Select operator to be visited
	 */
	public void visit(Select op);
	/**
	 * Visit a Product operator.
	 * @param op Product operator to be visited
	 */
	public void visit(Product op);
	/**
	 * 
	 * @param op
	 */
	public void visit(Join op);
}
