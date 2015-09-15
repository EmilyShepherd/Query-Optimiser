/**
 * 
 */
package sjdb;

/**
 * @author nmg
 *
 */
public class Attribute {

	private String name;
	private int values;

	public Attribute(String name) {
		this.name = name;
		this.values = 0;
	}
	/**
	 * @param name
	 * @param values
	 */
	public Attribute(String name, int values) {
		this.name = name;
		this.values = values;
	}
	
	public Attribute(Attribute attr) {
		this.name = attr.name;
		this.values = attr.values;
	}
	
	/**
	 * @return the name of the attribute
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the number of distinct values taken by this attribute
	 */
	public int getValueCount() {
		return values;
	}
	
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Attribute)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		Attribute rhs = (Attribute) obj;
		
		return this.name.equals(rhs.getName());
	}
	
	public String toString() {
		return this.name;
	}
	
	public String render() {
		return name + "," + values;
	}
	
	
}
