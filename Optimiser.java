package sjdb;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Attempts to optimise a given query plan
 * 
 * Note: This class does not implement PlanVisitor. This is because,
 * although its method is predominantly depth-first, child nodes
 * require an understanding of their parents. The PlanVisitor interface
 * is unable to provide this.
 * 
 * @author Emily Shepherd
 *
 */
public class Optimiser
{
	/**
	 * A list of all select operations
	 * 
	 * These are removed when found, to be re-added above a Scan Operator.
	 */
	private ArrayList<Predicate> selects = new ArrayList<Predicate>();
	
	private ArrayList<Predicate> joins = new ArrayList<Predicate>();
	
	/**
	 * A list of all required attributes, with the number
	 * of times they are needed
	 * 
	 * This is to allow Project Operators to be added in
	 * the correct locations.
	 */
	private HashMap<Attribute, Integer> requiredAttrs = new HashMap<Attribute, Integer>();

	/**
	 * When true, Project Operations are not automatically added
	 */
	private boolean outputStar = true;
	
	/**
	 * The Estimator used to inform the join reordering process
	 */
	private Estimator estimator = new Estimator();
	
	/**
	 * Constructor
	 * 
	 * Does nothing other than to confirm to the given template
	 * 
	 * @param cat I don't use this... sorry! :/
	 */
	public Optimiser(Catalogue cat) {}
	
	/**
	 * Constructor
	 * 
	 * Does nothing
	 */
	public Optimiser() {}
	
	/**
	 * Optimises the given Operator
	 * 
	 * This method doesn't do much, other than to ascertain the
	 * type of Operator it has been given, and pass it to the
	 * specific method to be optimised.
	 * 
	 * @param o The Operator to be optimised
	 * @return A new, optimised, Operator
	 * @see optimise(Product)
	 * @see optimise(Scan)
	 * @see optimise(Select)
	 * @see optimise(Project)
	 */
	public Operator optimise(Operator o)
	{
		// Switching on class names... yeeesssssssss
		switch (o.getClass().getName())
		{
			case "sjdb.Scan":    return optimise((Scan)o);
			case "sjdb.Project": return optimise((Project)o);
			case "sjdb.Select":  return optimise((Select)o);
			case "sjdb.Product": 
			case "sjdb.Join":    return optimise((BinaryOperator)o);
			default:             return null;
		}
	}
	
	/**
	 * Optimises a Scan Operator
	 * 
	 * Scans aren't actually optimised, however this method adds any
	 * "attr=val" select statements directly above the new Scan.
	 * 
	 * @param plan The Scan to be optimised
	 * @return A new, optimised, Operator, headed with all appropriate
	 *    Select and Project Operators
	 */
	public Operator optimise(Scan plan)
	{
		Relation r             = plan.getRelation();
		List<Attribute> attrs  = r.getAttributes();
		Operator newPlan       = new Scan((NamedRelation)r);
		Iterator<Predicate> it = selects.iterator();
		estimator.visit((Scan)newPlan);
		
		// Look through the select operations that we have saved, to see if
		// any of them contain attributes provided by this relation
		while (it.hasNext())
		{
			Predicate predicate = it.next();
			Attribute attr      = predicate.getLeftAttribute();
			
			if (attrs.contains(attr))
			{
				Attribute a = new Attribute(attr);
				Predicate p = new Predicate(a, predicate.getRightValue());
				newPlan     = new Select(newPlan, p);
				
				estimator.visit((Select)newPlan);
				decreaseRequired(a);
				it.remove();
			}
		}
		
		return addRequiredProjections(newPlan);
	}
	
	/**
	 * Optimises a Project Operator
	 * 
	 * When a Project Operator appears anywhere in the tree, its attributes
	 * are noted, and it is removed. New Project Operators are added into
	 * the tree whenever required by the other functions.
	 * 
	 * @param plan The Project Operation to be optimised
	 * @return A new, optimised, Operator with Projects moved down
	 */
	public Operator optimise(Project plan)
	{
		// Make a note of all attributes, so they can be added into
		// a Project later down the line
		for (Attribute a : plan.getAttributes())
		{
			increaseRequired(a);
		}
		
		outputStar       = false; // Turn on Project Operators
		Operator newPlan = optimise(plan.getInput());
		newPlan          = addRequiredProjections(newPlan);
		outputStar       = true; // Turn off Project Operators
		
		return newPlan;
	}
	
	/**
	 * Optimises a Select Operator
	 * 
	 * When a Select Operator is found, it is noted and removed. 
	 * These will then be either added directly above a Scan Operator (in
	 * the case of "attr=val") or used to create a Join statement.
	 * 
	 * @param plan The Select Operator to optimise
	 * @return A new, optimised, Operator with Selects moved down / converted
	 *     to Joins
	 */
	public Operator optimise(Select plan)
	{
		// Register the attribute as required so Project Operations
		// down the line know to include it
		increaseRequired(plan.getPredicate().getLeftAttribute());
		
		if (plan.getPredicate().equalsValue())
		{
			selects.add(plan.getPredicate());
		}
		else
		{
			increaseRequired(plan.getPredicate().getRightAttribute());
			joins.add(plan.getPredicate());
		}
		
		return optimise(plan.getInput());
	}
	
	/**
	 * Optimises a Product Operator
	 * 
	 * This method works differently to the optimise() methods above. Whereas
	 * they continued recursively searching the tree in a depth-first manner,
	 * this processes each side width-first, and brings up any Product
	 * operations below it:
	 * <pre>
	 *        PRODUCT                     PRODUCT
	 *          / \                         /|\
	 *         /   \                       / | \
	 *     PRODUCT  A      Becomes        /  |  \
	 *       / \          =========>     /   |   \
	 *      /   \                       /    |    \
	 *     B     C                    O(A)  O(B)  O(C)
	 * 
	 * (Where O(X) is the optimised operator)
	 * </pre>
	 * 
	 * It then compares each possible pair with the set of known Select
	 * Statements, to determine if it is a possible join, and to estimate
	 * its cost. For example:
	 * <pre>
	 *   Known Selects
	 *   -------------
	 *     SELECT * FROM A, B WHERE A_a=B_b
	 *     SELECT * FROM B, C WHERE B_b=C_c
	 *   
	 *   Pairs
	 *   -----
	 *     JOIN (A, B)           Possible (SELECT * FROM A, B...), T = 1000
	 *     JOIN (B, C)           Possible (SELECT * FROM B, C...), T = 40
	 *     JOIN (A, C)           Not Possible (There is no "SELECT * FROM A, C...")
	 * </pre>
	 * 
	 * Of those that are possible, the JOIN with the lowest cost is
	 * chosen. In this example, it was the JOIN of B and C:
	 *   J = JOIN(B, C)
	 * 
	 * In the remaining join possibilities, any reference to B or C is
	 * replaced with a reference to the new Join of B and C:
	 *   JOIN(A, B) -> JOIN(A, J)
	 * 
	 * The process is then repeated until the list of pairs is reduced to zero
	 *   
	 * @param plan The Product Operator to optimise
	 * @return A new, optimised, Operator with Selects moved down / converted
	 *     to Joins
	 */
	public Operator optimise(BinaryOperator op)
	{
		Operator newOp                                     = null;
		HashMap<Predicate, ArrayList<OperatorHolder>> rels = getAllRels(op);
		
		// Happens if all the Products were redundant. Eg:
		//   PROJECT [age] ((Department) TIMES (Project))
		// As age is not a member of either of those NamedRelations, getAllRels
		// won't return them
		if (rels.size() == 0)
		{
			Scan ret = new Scan(new NamedRelation("<Empty>", 0));
			estimator.visit(ret);
			return ret;
		}
		
		// Our "rels" array is a set of Predicates with the appropriate relations
		// attached.
		// While this isn't empty, there's potential a'joining to do!
		while (rels.size() > 0)
		{
			OperatorHolder leftHolder      = null;
			OperatorHolder rightHolder     = null;
			Predicate completedPredicate   = null;
			BinaryOperator mostRestrictive = null;
			Iterator<Entry<Predicate, ArrayList<OperatorHolder>>> l =
					rels.entrySet().iterator();
			
			// Do a pass over the pairs that we have, to estimate the cost of each
			while (l.hasNext())
			{
				Entry<Predicate, ArrayList<OperatorHolder>> e = l.next();
				Predicate p                                   = e.getKey();
				ArrayList<OperatorHolder> ops                 = e.getValue();
				
				// We expect this to have 2 Operators as all names are mutally exclusive.
				// If it only has the one, there are a couple of exceptional cases...
				if (ops.size() == 1)
				{
					// If we've got an empty Predicate, this is getAllRels' way of telling
					// us it only found one valid Relation, so we can just throw it out
					if (p.getLeftAttribute() == null)
					{
						return ops.get(0).getOperator();
					}
					// If there was a Predicate attached to it, one of its attributes
					// must reference an attribute that doesn't exist
					else
					{
						l.remove();
						continue;
					}
				}
				
				Operator left                                 = ops.get(0).getOperator();
				Operator right                                = ops.get(1).getOperator();
				BinaryOperator testOp;
				
				// If these are null, it means they were originally products,
				// but they've been dealt with now, so we can ignore them
				if (left == null || right == null)
				{
					l.remove();
					continue;
				}
				// If the left attribute is null, this is one of the "empty" Predicates
				// created by getAllRels to indicate this must be checked as a Product
				// Operator rather than a Join
				else if (p.getLeftAttribute() == null)
				{
					testOp = new Product(left, right);
					
					estimator.visit((Product)testOp);
				}
				else
				{
					Attribute leftA  = null;
					Attribute rightA = null;
					
					// This is safe to do as we've constructed this array, so we know they
					// are in here... we just don't know which way round.
					try
					{
						leftA  = left.getOutput().getAttribute(p.getLeftAttribute());
						rightA = right.getOutput().getAttribute(p.getRightAttribute());
					}
					catch (Exception ex)
					{
						leftA  = left.getOutput().getAttribute(p.getRightAttribute());
						rightA = right.getOutput().getAttribute(p.getLeftAttribute());
					}
					
					leftA  = new Attribute(leftA);
					rightA = new Attribute(rightA);
					
					testOp = new Join(left, right, new Predicate(leftA, rightA));
					
					// Do the cost calculation!
					estimator.visit((Join)testOp);
				}
	
				// If this is the first, we have nothing to compare it to, so we'll
				// declare it the most restrictive for now
				if (mostRestrictive == null)
				{
					mostRestrictive    = testOp;
					completedPredicate = p;
					leftHolder         = ops.get(0);
					rightHolder        = ops.get(1);
				}
				// We have a previous most restrictive... check if this join would be
				// less costly.
				// If it is, update it to be the winner.
				else if (testOp.getOutput().getTupleCount() < mostRestrictive.getOutput().getTupleCount())
				{
					mostRestrictive    = testOp;
					completedPredicate = p;
					leftHolder         = ops.get(0);
					rightHolder        = ops.get(1);
				}
			}
			
			if (completedPredicate == null) continue;
			
			// Whichever predicate we've completed needs to be deleted from
			// our stores, as we've dealt with it now
			rels.remove(completedPredicate);
			
			if (completedPredicate.getLeftAttribute() != null)
			{
				// Same goes for any variables it used... we no longer need to
				// project them above here
				decreaseRequired(completedPredicate.getLeftAttribute());
				decreaseRequired(completedPredicate.getRightAttribute());
			}
			else
			{
				leftHolder.close();
				rightHolder.close();
			}
			
			// Add any projections that we *do* need then push this new join
			// into the mix. The OperatorHolder is used to replace all references
			// of the two Operators below this Join, with the Join itself.
			newOp                   = addRequiredProjections(mostRestrictive);
			OperatorHolder opHolder = new OperatorHolder(newOp);
			leftHolder.setOperator(opHolder);
			rightHolder.setOperator(opHolder);
		}
		
		return newOp;
	}
	
	/**
	 * Goes down the tree, collecting all children of Product statements into
	 * a single list
	 * 
	 * @param rels The HashMap to populate with relations
	 * @param op The Operator to search
	 */
	private HashMap<Predicate, ArrayList<OperatorHolder>> getAllRels(BinaryOperator op)
	{
		HashMap<Predicate, ArrayList<OperatorHolder>> rels =
				new HashMap<Predicate, ArrayList<OperatorHolder>>();
		ArrayDeque<Operator> stack         = new ArrayDeque<Operator>();
		ArrayList<OperatorHolder> products = new ArrayList<OperatorHolder>();
		ArrayList<OperatorHolder> all      = new ArrayList<OperatorHolder>();

		stack.push(op);
		
		// Do a width-first search of the nodes for a bit, as we want to flatten
		// all the Products up into one manageable bunch.
		// Also, having a dedicated stack just for pending BinaryOperators is
		// way better than filling up the call stack with recursive calls
		while (!stack.isEmpty())
		{
			Operator look = stack.pop();
			
			// Ok, this says "BinaryOperator" but it actually only supports
			// Product Operators.
			// It can read Joins without crashing, but they will get ignored
			// as their predicate isn't recovered.
			// To implement Join understanding, add something like:
			//   if (look instanceof Join) joins.add(((Join)look).getPredicate());
			//
			// ...and by "something like" I'm pretty sure I mean exactly that.
			if (look instanceof BinaryOperator)
			{
				stack.push(((BinaryOperator) look).getLeft());
				stack.push(((BinaryOperator) look).getRight());
				continue;
			}
			
			Operator newOp         = optimise(look);
			
			// Check that this output actually has some benefit to the system
			// If not, there's no point attempting to Join it to anything
			//
			// This'll happen if optimise(Scan) rejects a NamedRelation on the
			// basis that it doesn't add anything to a query.
			//   EG: SELECT age FROM Department
			if (newOp.getOutput().getTupleCount() == 0)
			{
				continue;
			}
			
			Iterator<Predicate> it = joins.iterator();
			Relation R             = newOp.getOutput();
			List<Attribute> attrs  = R.getAttributes();
			OperatorHolder hold    = new OperatorHolder(newOp);
			boolean found          = false;
			all.add(hold);
			
			// Loop over the known joins to see if we can add this operator to one
			while (it.hasNext())
			{
				Predicate p = it.next();
				Attribute a = null;
				
				// Search for the attribute:
				//   attrs.contains(left) xor attrs.contains(right)
				//       => applicable join
				//   attrs.contains(left) && attrs.contains(right)
				//       => self-join -> select attr=attr
				//   !attrs.contains(left) && !attrs.contains(right)
				//       => join predicate inapplicable to this relation
				if (attrs.contains(p.getLeftAttribute()))
				{
					a = p.getLeftAttribute();
					
					// This should be fail for joins
					// If the attrs contains both left and right, this'd be a self-join
					// so we've got to turn the predicate back into a select attr=attr
					// Operator
					if (attrs.contains(p.getRightAttribute()))
					{
						a           = new Attribute(a);
						Attribute b = new Attribute(p.getRightAttribute());
						newOp       = new Select(newOp, new Predicate(a, b));
						decreaseRequired(a);
						decreaseRequired(b);
						estimator.visit((Select)newOp);
						hold.replaceOperator(addRequiredProjections(newOp));
						it.remove();
						continue;
					}
				}
				// Pretty much the same as the if statement above, except we don't need
				// the nested if statement because the one above'll catch it
				else if (attrs.contains(p.getRightAttribute()))
				{
					a = p.getRightAttribute();
				}
				// Not found anywhere, so this predicate can't be used on this
				// relation
				else
				{
					continue;
				}
				
				// Make a note of the fact we've found a predicate for this guy
				found = true;
				
				if (rels.containsKey(p))
				{
					rels.get(p).add(hold);
					
					// A predicate has two relations. If it already exists in the
					// HashMap, it must already have one, so this is the second.
					// Therefore, it's safe to remove it from the list, to keep from
					// wasting time checking the next relations on it...
					it.remove();
				}
				else
				{
					ArrayList<OperatorHolder> al = new ArrayList<OperatorHolder>();
					al.add(hold);
					rels.put(p, al);
				}
			}
			
			// If we didn't find this relation in any of the predicates, that means
			// it is actually a product (egh!). Record it so we can add it to the
			// output
			if (!found)
			{
				hold.closeable();
				products.add(hold);
			}
		}
		
		// Special Case
		// We've only found one acceptable relation
		// This is a bit of a hack to get this to return properly, but
		// optimise(BinaryOperator) will understand this and simply
		// return the Operator without doing anything to it
		if (products.size() == 1 && all.size() == 1)
		{
			Predicate empty              = new Predicate(null, "");
			
			ArrayList<OperatorHolder> al = new ArrayList<OperatorHolder>();
			al.add(products.get(0));
			rels.put(empty, al);
			
			return rels;
		}
		
		// If we've found some products, we have to compare them to every single
		// other relation to check if their product would be most restrictive
		//
		// NB: Products and Joins are checked together as products *can* sometimes
		// be less costly. For example:
		//   A JOIN[foo=bar] B TIMES C
		// If B and C were both very small, and A was massive, doing (B TIMES C)
		// first may be preferable...
		for (OperatorHolder c : products)
		{
			// Remove it to prevent:
			//   a) The following loop comparing the operator with itself
			//   b) Double comparisons in future loops
			//          EG: (A TIMES B) and (B TIMES A)
			all.remove(c);
			
			for (OperatorHolder pair : all)
			{
				// We create an empty predicate to indicate this is a bog-standard
				// Product operation
				// (This Predicate won't leave the Optimiser class, it'll be replaced
				// with a Product Operator before output, fear not!)
				Predicate empty              = new Predicate(null, "");
				
				ArrayList<OperatorHolder> al = new ArrayList<OperatorHolder>();
				al.add(c);
				al.add(pair);
				rels.put(empty, al);
			}
		}
		
		return rels;
	}
	
	/**
	 * Increases the number of times this attribute is required
	 * 
	 * If the attribute doesn't yet exist in the HashMap, it is
	 * added with value 1. Otherwise its value is increased.
	 * 
	 * @param a The Attribute to record as required
	 */
	private void increaseRequired(Attribute a)
	{
		if (requiredAttrs.containsKey(a))
		{
			requiredAttrs.put(a, requiredAttrs.get(a) + 1);
		}
		else
		{
			requiredAttrs.put(a, 1);
		}
	}
	
	/**
	 * Decreased the number of times this attribute is required
	 * 
	 * @param a The Attribute to record as no longer required
	 */
	private void decreaseRequired(Attribute a)
	{
		requiredAttrs.put(a, requiredAttrs.get(a) - 1);
	}
	
	/**
	 * 
	 * @param newPlan
	 * @return
	 */
	private Operator addRequiredProjections(Operator newPlan)
	{
		// If we have to output everything, no Projects are required
		if (outputStar) return newPlan;
		
		List<Attribute> attrs = newPlan.getOutput().getAttributes();
		List<Attribute> found = new ArrayList<Attribute>();
		Iterator<Entry<Attribute, Integer>> req  = requiredAttrs.entrySet().iterator();
		
		while (req.hasNext())
		{
			Entry<Attribute, Integer> pair = req.next();
			Attribute a                    = pair.getKey();
			
			if (pair.getValue() != 0 && attrs.contains(a) && !found.contains(a))
			{
				found.add(a);
			}
		}
		
		if (found.size() == 0)
		{
			return new Scan(new NamedRelation("<Empty>", 0));
		}
		else if (found.size() != attrs.size())
		{
			newPlan = new Project(newPlan, found);
			estimator.visit((Project)newPlan);
		}
		
		return newPlan;
	}
	
	/**
	 * This class is used by the Product optimiser to wrap an Operator
	 * 
	 * This is so that references to Operators that are joined can be
	 * easily replaced.
	 * 
	 * @author Emily Shepherd
	 *
	 */
	private class OperatorHolder
	{
		/**
		 * The Operator
		 */
		private Operator op;
		
		private OperatorHolder opHolder;
		
		private boolean closeable = false;
		
		private boolean closed = false;
		
		/**
		 * Constructor
		 * 
		 * Just sets the value of op
		 * 
		 * @param op The Operator
		 */
		public OperatorHolder(Operator op)
		{
			this.op = op;
		}
		
		public void setOperator(OperatorHolder opHolder)
		{
			if (this.opHolder == null)
			{
				this.opHolder = opHolder;
			}
			else
			{
				this.opHolder.setOperator(opHolder);
			}
		}
		
		public void replaceOperator(Operator op)
		{
			this.op = op;
		}
		
		public Operator getOperator()
		{
			if (closed)
			{
				return null;
			}
			else if (opHolder == null)
			{
				return op;
			}
			else
			{
				return opHolder.getOperator();
			}
		}
		
		public void closeable()
		{
			closeable = true;
		}
		
		public void close()
		{
			if (closeable)
			{
				closed = true;
			}
		}
		
		@Override
		public String toString()
		{
			return getOperator().toString();
		}
	}
}
