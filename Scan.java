package sjdb;

import java.util.List;
import java.util.Iterator;

/**
 * This class implements a Scan operator that feeds a NamedRelation into
 * a query plan.
 * @author nmg
 */
public class Scan extends Operator {
	/**
	 * The named relation to be scanned
	 */
	private NamedRelation relation;
	
	/**
	 * Create a new scan of a given named relation
	 * @param relation Named relation to be scanned
	 */
	public Scan(NamedRelation relation) {
		this.relation = relation;
		this.output = new Relation(relation.getTupleCount());
		Iterator<Attribute> iter = relation.getAttributes().iterator();
		
		while (iter.hasNext()) {
			this.output.addAttribute(new Attribute(iter.next()));
		}
	}

	/* (non-Javadoc)
	 * @see sjdb.Operator#getInputs()
	 */
	@Override
	public List<Operator> getInputs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Return the named relation to be scanned
	 * @return Named relation to be scanned
	 */
	public Relation getRelation() {
		return this.relation;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.relation.toString();
	}
	
	/* (non-Javadoc)
	 * @see sjdb.Operator#accept(sjdb.OperatorVisitor)
	 */
	public void accept(PlanVisitor visitor) {
		visitor.visit(this);
	}
}
