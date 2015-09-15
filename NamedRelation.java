/**
 * 
 */
package sjdb;

/**
 * This class represents a named relation which is fed into a query plan
 * @author nmg
 */
public class NamedRelation extends Relation {
	/**
	 * The name of the named relation
	 */
	private String name;
	
	/**
	 * Create a new named relation with a given name and tuple count
	 * @param name The name of the relation
	 * @param size The tuple count
	 */
	public NamedRelation(String name, int size) {
		super(size);
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	
	/* (non-Javadoc)
	 * @see sjdb.Relation#render()
	 */
	public String render() {
		return name + ":" + super.render();
	}
}
