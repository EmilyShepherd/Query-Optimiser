/**
 * 
 */
package sjdb;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class parses a canonical query provided on stdin
 * 
 * The canonical query is of the form:
 * 
 * SELECT <attribute name>,<attribute name>,...,<attribute name>
 * FROM <relation name>,<relation name>,...,<relation name>
 * WHERE <predicate>,<predicate>,...,<predicate>
 * 
 * where <predicate> is of one of the following two forms:
 * 
 * <attribute name>="<value>"
 * <attribute name>=<attribute name>
 * 
 * The WHERE line (corresponding to the select operators) is optional and 
 * may be omitted; the other lines are required.
 * 
 * To form the canonical query, a left-deep tree of cartesian
 * products over scans over the named relations is built, following by a series
 * of select with the given predicates, and then a single project 
 * with the given attributes.
 * 
 * Note that the author of this class was extremely lazy, and so the parsing 
 * is unforgiving and may be sensitive to extraneous whitespace. In particular, 
 * values in predicates that contain spaces (or for that matter commas) will 
 * break the parsing of the WHERE clause.
 * 
 * @author nmg
 */
public class QueryParser {
	private BufferedReader reader;
	private Catalogue catalogue;

	/**
	 * Create a new QueryParser. This class is intended to be used once only;
	 * repeated calls to parse() may cause unexpected behaviour.
	 * 
	 * @param catalogue
	 * @param input
	 * @throws Exception
	 */
	public QueryParser(Catalogue catalogue, Reader input) throws Exception {
		this.catalogue = catalogue;
		this.reader = new BufferedReader(input);
	}
	
	/**
	 * Read a query from the input (via the BufferedReader) and parse it
	 * to create a canonical query plan.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Operator parse() throws Exception {
		Operator product, select, project;
		String projectLine = this.reader.readLine();
		String productLine = this.reader.readLine();
		String selectLine = this.reader.readLine();
		
		product = parseProduct(productLine);
		if (selectLine != null && selectLine.startsWith("WHERE")) {
			select = parseSelect(selectLine, product); 
			project = parseProject(projectLine, select);
		} else {
			project = parseProject(projectLine, product);
		}
		
		return project;
	}
	
	/**
	 * Parse a "FROM ..." line 
	 * @param line
	 * @return
	 */
	public Operator parseProduct(String line) {
		String[] rels = line.split("FROM\\s+");
		String[] reln = rels[1].split("\\s*,\\s*");
		
		return buildProduct(reln);
	}
	
	/**
	 * Build a left-deep cartesian product tree from the relations
	 * with the given names
	 * @param names
	 * @return
	 */
	private Operator buildProduct(String[] names) {
		Operator left = buildScan(names[0].trim());
		Operator right;
		Operator accum;
		
		if (names.length>1) {
			for (int i = 1; i < names.length; i++) {
				right = buildScan(names[i].trim());
				accum = new Product(left, right);
				left = accum;
			}
		}
		
		return left;
	}
	
	/**
	 * Build a scan operator that reads the relation with the given
	 * name
	 * @param name
	 * @return
	 */
	private Operator buildScan(String name) {
		Operator op = null;
		try {
			op = new Scan(this.catalogue.getRelation(name));
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return op;
	}
	
	/**
	 * Parse a "WHERE ..." line.
	 * @param line
	 * @param op
	 * @return
	 */
	private Operator parseSelect(String line, Operator op) {
		String[] prds = line.split("WHERE\\s+");

		String[] pred = prds[1].split("\\s*,\\s*");
		Operator ret = op;
		
		for (int i=0; i<pred.length; i++) {
			ret = buildSelect(pred[i].trim(), ret);
		}
		
		return ret;
	}
	
	/**
	 * Build a chain of select operators.
	 * @param pred
	 * @param op
	 * @return
	 */
	private Operator buildSelect(String pred, Operator op) {
		Pattern p = Pattern.compile("(\\w+)=\"(\\w+)\"");
		Matcher m = p.matcher(pred);
		Predicate ret;
		
		if (m.matches()) {
			ret = new Predicate(new Attribute(m.group(1)), m.group(2));
		} else {
			String[] atts = pred.split("=");
			ret = new Predicate(new Attribute(atts[0]), new Attribute(atts[1]));
		}
		
		return new Select(op, ret);
	}
	
	/**
	 * Parse a "SELECT ..." line and build the corresponding project operator.
	 * @param line
	 * @param op
	 * @return
	 */
	private Operator parseProject(String line, Operator op) {
		String[] atts = line.split("SELECT\\s+");		
		if (atts[1].trim().equals("*")) {
			return op;
		} else {
			String[] attr = atts[1].split("\\s*,\\s*");
			ArrayList<Attribute> attributes = new ArrayList<Attribute>();

			for (int i=0; i<attr.length; i++) {
				attributes.add(new Attribute(attr[i].trim()));
			}

			return new Project(op, attributes);
		}
	}
}