package sjdb;
import java.io.*;

/**
 * This class parses a serialised system catalogue, and uses the
 * Catalogue class to create instances of the named relations and
 * attributes described in the system catalogue.
 * 
 * The lines in the serialised system catalogue are of the form:
 * 
 * <relation name>:<tuple count>:<attr name>,<value count>:<attr name>,<value count>
 * 
 * @author nmg
 */
public class CatalogueParser {
	private BufferedReader read;
	private Catalogue catalogue;

	/**
	 * Create a parser that reads from the file of the given name
	 * @param catFilename
	 * @param catalogue
	 */
	public CatalogueParser(String catFilename, Catalogue catalogue) {
		this.catalogue = catalogue;
		try {
			this.read = new BufferedReader(new FileReader(catFilename));
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	public void parse() {
		String line;

		try {
			while ((line = this.read.readLine()) != null) {
				parseRelation(line.split(":", 0));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseRelation(String[] parts) {
		String reln = parts[0];
		int size = Integer.decode(parts[1]).intValue();
		
		catalogue.createRelation(reln, size);
		
		for (int i = 2; i < parts.length; i++) {
			parseAttribute(reln, parts[i].split(",", 0));
		}
	}

	private void parseAttribute(String reln, String[] parts) {
		String attr = parts[0];
		int values = Integer.decode(parts[1]).intValue();
		
		catalogue.createAttribute(reln, attr, values);
	}
}
