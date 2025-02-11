package gqr;

import java.util.HashMap;
import java.util.Map;

public class PoolOfNames {

	private static int count=0; 
	private static Map<String, String> map = new HashMap<String, String>(); 
	
	public static String getName(String var) {
		
		if(var.startsWith("DC"))
			return "C"+count++;//"__";
			//TODO this above is temporary for junit tests -- uncomment "__" ande everything else later
		String name = map.get(var);
		if(name == null)
		{
			name = "C"+count++;
			map.put(var, name);	
		}
		
		return name;
	}
	
	
//	public String getNameTemp(String var) {
//		
//		if(var.startsWith("UR"))
//			return "DC";
//		
//		return var;
//	}

	public static String getName2(String var) {
		
//		System.out.println(var);
		//2@v44.m14004{0}_0
		var = var.replaceAll("UR", "");
		var = var.replaceAll("AT", "");
		var = var.replaceAll("\\(", "");
		var = var.replaceAll("\\)", "");
		var = var.replaceAll("\\{", "");
		var = var.replaceAll("\\}", "");
		var = var.replaceAll("\\.", "");

		
		return "Z"+var;
	}

}
