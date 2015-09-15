package sjdb;

import java.util.Iterator;

/**
 * Estimates the cost of the given query plan
 * 
 * For normal usage, you need not call any methods yourself, simply
 * instantiate and pass to a query plan via Operator.accept():
 * <code>
 *   Operator plan = instantiated_somehow();
 *   Estimator myEstimator = new Estimator();
 *   plan.accept(myEstimator);
 * </code>
 * 
 * In any relational algebra shown in the below JavaDoc comments:
 *   + T(R) is the number of tuples in relation R
 *   + V(R, A) is the value count for attribute A of relation R
 *   + pi_(A)(R) is a projection of attribute A on relation R
 *   + sigma_(A=B)(R) is a selection from relation, R, where attribute, A,
 *     equals B (where B is either an attribute or a string value)
 *   + min(x, y) and max(x, y) are minimum and maximum
 * 
 * @see Operator.accept()
 * @author Emily Shepherd
 *
 */
public class Estimator implements PlanVisitor
{
	/**
	 * Assesses the cost of a scan operation
	 * 
	 * For input, R, and output, O:
	 *   T(O) = T(R)
	 * 
	 * @param op The Scan Operator to be assessed
	 */
	@Override
	public void visit(Scan op)
	{
		Relation R               = new Relation(op.getRelation().getTupleCount());
		Iterator<Attribute> iter = op.getRelation().getAttributes().iterator();
		
		while (iter.hasNext())
		{
			R.addAttribute(new Attribute(iter.next()));
		}
		
		op.setOutput(R);
	}

	/**
	 * Assesses the cost of a project operation
	 * 
	 * For input, R, projecting on attribute, A, with output, O:
	 *   T(PI_A(O)) = T(R)
	 * 
	 * @param op The Project Operator to be assessed
	 */
	@Override
	public void visit(Project op)
	{
		// The relation we are projecting
		Relation R = op.getInput().getOutput();
		
		// The projected relation
		Relation rel = new Relation(R.getTupleCount());
		
		// We only want to carry forward the attributes
		// that are projected
		for (Attribute a : op.getAttributes())
		{
			try
			{
				rel.addAttribute(new Attribute(R.getAttribute(a)));
			}
			// If you try Projecting an attribute that doesn't exist
			catch (Exception e)
			{
				rel.addAttribute(new Attribute(a.getName(), 0));
			}
		}
		
		op.setOutput(rel);
	}

	/**
	 * Assesses the cost of a select operation
	 * 
	 * For input, R, with output, O:
	 * 	 For predicates in the form: attr=val (A and C):
	 *     T(sigma_(A=C)(O) = T(R)/V(R, A)
	 *   For predicates in the form: attr=attr (A and B):
	 *     T(sigma_(A=B)(O) = T(R)/max(V(R, A), V(R, B))
	 * 
	 * @param op The Select Operator to be assessed
	 */
	@Override
	public void visit(Select op)
	{
		Relation R      = op.getInput().getOutput();
		Attribute Left  = null;
		Attribute Right = null;
		int RightCount;
		int LeftCount;
		
		try
		{
			Right      = R.getAttribute(op.getPredicate().getRightAttribute());
			RightCount = Right.getValueCount();
		}
		catch (Exception ex)
		{
			RightCount = 0;
		}
		
		try
		{
			Left      = R.getAttribute(op.getPredicate().getLeftAttribute());
			LeftCount = Left.getValueCount();
		}
		catch (Exception ex)
		{
			LeftCount = 0;
			
			op.setOutput(new Relation(0));
			return;
		}
		
		int V;
		Relation rel;
		
		if (op.getPredicate().equalsValue())
		{
			rel = new Relation(R.getTupleCount() / LeftCount);
			V   = 1;
		}
		else
		{
			V   = Math.min(LeftCount, RightCount);
			rel = new Relation(R.getTupleCount() / Math.max(LeftCount, RightCount));
		}
		
		for (Attribute a : R.getAttributes())
		{
			if (a.equals(Left) || a.equals(Right))
			{
				rel.addAttribute(new Attribute(a.getName(), V));
			}
			else
			{
				rel.addAttribute(new Attribute(a));
			}
		}
		
		op.setOutput(rel);
	}

	/**
	 * Assesses the cost of a product operation
	 * 
	 * For inputs, R and S, with output, O:
	 * 	 T(O) = T(R).T(S)
	 * 
	 * @param op The Product Operator to be assessed
	 */
	@Override
	public void visit(Product op)
	{
		Relation Left  = op.getLeft().getOutput();
		Relation Right = op.getRight().getOutput();
		Relation R     = new Relation(Left.getTupleCount() * Right.getTupleCount());
		
		for (Attribute a : Left.getAttributes())
		{
			R.addAttribute(new Attribute(a));
		}
		for (Attribute a : Right.getAttributes())
		{
			R.addAttribute(new Attribute(a));
		}
		
		op.setOutput(R);
	}

	/**
	 * Assesses the cost of a join operation
	 * 
	 * For inputs, R and S, joining on attributes, A and B, with output, O:
	 * 	 T(O) = T(R).T(S)/max(V(R, A), V(S, B))
	 * 
	 * @param op The Select Operator to be assessed
	 */
	@Override
	public void visit(Join op)
	{
		Relation Left  = op.getLeft().getOutput();
		Relation Right = op.getRight().getOutput();

		op.setOutput(estimateJoin(Left, Right, op.getPredicate()));
	}
	
	/**
	 * @deprecated
	 * @see visit(Join)
	 * @param Left
	 * @param Right
	 * @param p
	 * @return
	 */
	private Relation estimateJoin(Relation Left, Relation Right, Predicate p)
	{
		int LeftCount  = Left.getAttribute(p.getLeftAttribute()).getValueCount();
		int RightCount = Right.getAttribute(p.getRightAttribute()).getValueCount();
		Relation R     = new Relation(Left.getTupleCount() * Right.getTupleCount() / Math.max(LeftCount, RightCount));
		
		for (Attribute a : Left.getAttributes())
		{
			if (a.equals(p.getLeftAttribute()))
			{
				R.addAttribute(new Attribute(a.getName(), Math.min(LeftCount, RightCount)));
			}
			else
			{
				R.addAttribute(new Attribute(a));
			}
		}
		for (Attribute a : Right.getAttributes())
		{
			if (a.equals(p.getLeftAttribute()))
			{
				R.addAttribute(new Attribute(a.getName(), Math.min(LeftCount, RightCount)));
			}
			else
			{
				R.addAttribute(new Attribute(a));
			}
		}
		
		return R;
	}

}
