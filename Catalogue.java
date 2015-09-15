package sjdb;

import java.util.HashMap;

/**
 * This class contains the system catalogue for the database; it
 * is responsible for:
 * 
 * - creating new NamedRelations
 * - creating new Attributes
 * 
 * The catalogue maintains a directory of NamedRelations and
 * Attributes, indexed by name.
 * 
 * Note that any statistical information about named relations or
 * the attributes therein is stored on the relations or attributes,
 * and not in the catalogue.
 * 
 * @author nmg
 *
 */
public class Catalogue {
	
	private HashMap<String, NamedRelation> relations;
	private HashMap<String, Attribute> attributes;


	public Catalogue() {
		this.relations = new HashMap<String, NamedRelation>();
		this.attributes = new HashMap<String, Attribute>();
	}
	
	/**
	 * Create a new NamedRelation with the specified name and size and 
	 * add it to the directory.
	 * 
	 * @param relName
	 * @param size
	 */
	public NamedRelation createRelation(String relName, int size) {
		NamedRelation reln = new NamedRelation(relName, size);
		relations.put(relName, reln);
		return reln;
	}
	
	/**
	 * Create a new Attribute with the specified name and number of distinct
	 * values, add it to the directory and associate it with the specified 
	 * NamedRelation.
	 * 
	 * @param relName
	 * @param attName
	 * @param values
	 * @return
	 */
	public Attribute createAttribute(String relName, String attName, int values) {
		Attribute attr = new Attribute(attName, values);
		attributes.put(attName, attr);
		relations.get(relName).addAttribute(attr);
		return attr;
	}
	
	/**
	 * Return the NamedRelation with the specified name.
	 * 
	 * @param name
	 * @return
	 */
	public NamedRelation getRelation(String name) throws DatabaseException {
		NamedRelation reln = relations.get(name);
		
		if (reln==null) {
			throw new DatabaseException("Named relation " + name + " not found");
		}
		
		return reln;
	}
	
	/**
	 * Return the Attribute with the specified name.
	 * 
	 * @param name
	 * @return
	 */
	public Attribute getAttribute(String name) throws DatabaseException {
		Attribute attr = attributes.get(name);
		
		if (attr==null) {
			throw new DatabaseException("Attribute " + name + " not found");
		}
		
		return attr;
	}
}
