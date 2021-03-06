/* OGDL, Ordered Graph Data Language 
 * (c) R.Veen, 2005-2013.
 * License: zlib (see http://ogdl.org/license.htm)
 */

package ogdl.template;

import ogdl.*;
import ogdl.support.IFunction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Logger;



/** Instrospection front-end that converts Java classes into IFunction's.
 * 
 * XXX Spec: from String to IGraph to Java method/field call. This is what ComplexPath should implement
 * XXX Look at org.apache.velocity.util.introspection before redesigning this class.
 */

public class LocalFunction implements IFunction 
{
	HashMap<String, Field> fields;
	Method[] methods;
	Class<?>[] argClass, aC;
	Object[] argObject, aO;
	int nargs;
	Class<?> c = null;
	Object ci = null;
	
	Logger log = Logger.getLogger(this.getClass().getName()); 

	public LocalFunction(String className)
	{
		this(className, null);
	}

	public LocalFunction(String className, IGraph cfg) 
	{
		//log.setLevel(Level.FINE); // during debug
        
		try {
			c = Class.forName(className);
			ci = newInstance(c, cfg);
			
			methods = c.getMethods();
			getFields();

			argClass = new Class[10];
			argObject = new Object[10];
		} 
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		
	}

    public Object exec(IGraph g) throws Exception 
	{
    	if (ci == null)
    		throw new Exception("cannot exec() because no class could be instantiated in newInstance(). Maybe wrong constructor config parameters");
    	
    	log.fine("input\n"+g);
// System.out.println("LocalFuntion.exec:\n"+g); 
// System.out.println("LocalFuntion.class:\n"+ci.getClass().getName());    	

		String token = g.getName();

		/* fields */

		if (g.size() == 0 && fields != null) {
			Field f = (Field) fields.get(token);
			if (f != null)
				return f.get(ci);
			// add getter
		}

		getArguments(g);

		try {
			Method m = getMethod(token, aC);	
			Object o;

// System.out.println("LocalFuntion.class: method "+m);    				

            if (aO.length==0)
				o = m.invoke(ci);
			else
				o = m.invoke(ci, aO);
		    log.fine("invoked: "+methodString(token));
		    return o;
		}

		catch (Exception e) {
			e.printStackTrace();
			log.warning("method not found or invocation failed: "+message(token));
			return message(token);
		}
	}

	private Method getMethod(String name, Class<?>[] args) throws Exception 
	{	
		Method m = c.getMethod(name, args);

		if (m != null)
			return m;

		/* No method with exact the same arguments.
		 * Get the first less specific
		 * method: XXX search for the best !!
		 */

		for (int i = 0; i < methods.length; i++) {

			if (!name.equals(methods[i].getName()))
				continue;
			Class ca[] = methods[i].getParameterTypes();
			if (ca.length != args.length)
				continue;
			int j;
			for (j = 0; j < ca.length; j++) {
				if (!args[j].isAssignableFrom(ca[j]))
					break;
			}
			if (j != ca.length)
				continue;
			return methods[i];
		}
		return null;
	}	

	/** Get all public fields in the inspected class
	 * 
	 * This function creates a hashmap with field
	 * string pointing to Field objects.
	 */

	private void getFields() throws Exception 
	{
		Field[] f = c.getFields();
		if (f == null)
			return;

		fields = new HashMap<String, Field>();

		for (int i = 0; i < f.length; i++)
			fields.put(f[i].getName(), f[i]);
	}

	private Object newInstance(Class c, IGraph cfg) throws Exception 
	{
		if (cfg == null) 
		{
			try {
				Object o = c.newInstance();
				return o;
			} catch (Exception ex) {
				log.severe("Class has no default constructor");
				return null;
			}
		}
		
		try {
			Constructor co = c.getConstructor(new Class[] { IGraph.class });
			return co.newInstance(new Object[] { cfg });
		} catch (NoSuchMethodException e) {
			try {
				Object o = c.newInstance();
				return o;
			} catch (Exception ex) {
				log.severe("Class could not be instantiated");
				return null;
			}
		}
	}

	private void getArguments(IGraph g) throws Exception 
	{
		Object o;
		nargs = 0;
		
		for (int i = 0; i < g.size(); i++) 
		{
			o = Evaluate.toScalar(g.get(i));
// System.out.println("LocalFunction: argument "+g.get(i).getName()+" = "+o.getClass().getName());
			if (o instanceof Double)
				argClass[i] = double.class;
			else if (o instanceof Long)
				argClass[i] = long.class;
			else if (o instanceof Boolean)
				argClass[i] = boolean.class;
			else if (o instanceof IGraph)
				argClass[i] = ogdl.IGraph.class;
			else
				argClass[i] = o.getClass();
			
			argObject[i] = o;
			nargs++;
		}

		aC = new Class[nargs];
		aO = new Object[nargs];
		for (int i = 0; i < nargs; i++) {
			aC[i] = argClass[i];
			aO[i] = argObject[i];
		}
	}

	private String message(String token) 
	{
		StringBuffer sb = new StringBuffer();
		sb.append("No method ");
		sb.append(token);
		sb.append('(');

		for (int i = 0; i < nargs; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(argClass[i].getName());
		}

		sb.append(") found in class ");
		sb.append(c.getName());

		return sb.toString();
	}
	
	private String methodString(String token) 
	{
		StringBuffer sb = new StringBuffer();
		sb.append(c.getName());
		sb.append('.');
		sb.append(token);
		sb.append('(');

		for (int i = 0; i < nargs; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(argClass[i].getName());
		}

		sb.append(')');
		

		return sb.toString();
	}

	public void close() {
		IGraph c = new Graph("close");
		try {
			exec(c);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void finalize() throws Throwable {
		close();
	}

}

