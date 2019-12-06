package team.magenta.db;
import java.util.HashMap;
import java.util.Map;
public enum TypeOfPage {
	
	LEAF((byte)13),
	INDEXOFLEAF((byte)10),
	INNER((byte)5),
	INDEXOFINNER((byte)2);
	
     
	private static final Map<Byte,TypeOfPage> lookupPageType = new HashMap<Byte,TypeOfPage>();
	
	static {
		for(TypeOfPage pageType : TypeOfPage.values()) {
			lookupPageType.put(pageType.getValue(), pageType);
	    }
	}
	
	private byte content;
	
	private TypeOfPage(byte content) {
		this.content = content;
	}
	
	public byte getValue() { 
		return content; 
	}
	
	public static TypeOfPage get(byte content) { 
		return lookupPageType.get(content); 
	}
 
}