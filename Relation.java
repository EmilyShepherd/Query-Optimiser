package sjdb;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * The Relation class represents an unnamed relation. It consists of a 
 * number of attributes and a size (tuple count).
 * 
 * @author nmg
 */
public class Relation {
	private List<Attribute> attributes;
	private int size;

	/**
	 * Create a new relation with the given tuple count
	 */
	protected Relation(int size) {
		this.attributes = new ArrayList<Attribute>();
		this.size = size;
	}
	
	/**
	 * Return the list of attributes contained in this relation
	 * 
	 * @return the attributes
	 */
	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	
	/**
	 * Get an attribute from this relation, using another attribute as
	 * a template (the attributes are compared using .equals(), so only 
	 * the name of the attribute is significant).
	 * 
	 * @param attribute
	 * @return
	 */
	public Attribute getAttribute(Attribute attribute) {
		return this.attributes.get(this.attributes.indexOf(attribute));
	}

	/**
	 * Add an attribute to this relation, checking to make sure that the
	 * value count on the attribute is less than the relation's tuple
	 * count.
	 * 
	 * @param attribute the attribute to add
	 */
	public void addAttribute(Attribute attribute) {
		if (attribute.getValueCount() > this.size) {
			// If the attribute has more distinct values than there are tuples
			// in this relation, limit the distinct values to the number of
			// tuples
			this.attributes.add(new Attribute(attribute.getName(), this.size));
		} else {
			this.attributes.add(attribute);	
		}
	}
	
	/**
	 * Return the tuple count for this relation
	 * 
	 * @return the tuples
	 */
	public int getTupleCount() {
		return size;
	}
	
	
	/**
	 * Render this relation and its statistics in a form suitable for debugging 
	 * (i.e. the syntax used in the system catalogue)
	 * 
	 * The output from this method will be used to judge the success of the 
	 * cost estimation
	 * 
	 * @return the rendering of this relation
	 */
	public String render() {
		String ret = size + "";
		Iterator<Attribute> iter = this.attributes.iterator();
		while (iter.hasNext()) {
			ret += ":" + iter.next().render();
		}
		return ret;
	}
}
