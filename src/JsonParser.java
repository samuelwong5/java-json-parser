import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class JsonParser {
	
	/**
	 * Parses a String representation of a JSON object
	 * and returns a JSONTreeElement for easier traversal.
	 * 
	 * Usage: JsonParser.parse(String json)
	 * 
	 * @param json - the String representation of a JSON object
	 * @return JSONTreeElement representation of the JSON object.
	 * @throws Exception - if the input String is invalid.
	 */
	public static JsonTreeElement parse(String json) throws Exception {
		// Initialize stack 
		Stack<JsonTreeElement> stack = new Stack<JsonTreeElement>();
		
		// Initialize top-level JSONTreeElement
		JsonTreeElement head = new JsonTreeElement();
		stack.push(head);
		
		// Pointer to the current-level JSONTreeElement
		JsonTreeElement curr = head;
		
		// Holding variable for attribute tags
		String name = null;
		
		// Recursion depth of current JSONTreeElement
		int level = 0;
		
		for (int i = 0; i < json.length(); i++) {
			if (json.charAt(i) == '{') {
				if (level == 0) 
					level++;
				else {
					if (name == null) // Missing attribute tag before embedded JSON
						throw new Exception("Invalid input - missing attribute tag");
					
					// Add new JSONTreeElement as a child of current pointer
					// and then set the new element as current.
					JsonTreeElement hold = new JsonTreeElement();
					curr.addChild(name, hold);
					stack.push(curr);
					name = null;
					curr = hold;
				}
			} else if (json.charAt(i) == '}') {
			    curr = stack.pop();
			} else if (json.charAt(i) == '"') {
				if (name == null) { // Attribute string
					name = "";
					i++;
					while (json.charAt(i) != '"') {
						name += json.charAt(i);
						i++;
					}
				} else {  // Leaf value string
					String value = "";
					i++;
					while (json.charAt(i) != '"') {
						value += json.charAt(i);
						i++;
					}
					JsonTreeElement leaf = new JsonTreeElement();
					leaf.setValue(value);
					curr.addChild(name, leaf);
					name = null;
				}
			} 
		}
		
		return head;
	}
	
	private static class JsonTreeElement {
		private JsonTreeElementState state = JsonTreeElementState.NEW;
		private String value = null;
		private Map<String, JsonTreeElement> children;

		/** 
		 * Adds a children JSONTreeElement to this
		 * JSONTreeElemetn. Will also set the state flag
		 * to PARENT if not already set. Does nothing if
		 * the state flag is set to LEAF.
		 * 
		 * @param childName - the attribute tag of the child
		 * @param childElem - the child JSONTreeElement object
		 */
		public void addChild(String childName, JsonTreeElement childElem) {
			if (state == JsonTreeElementState.LEAF) {
				return;
			} else if (state == JsonTreeElementState.NEW){
				this.state = JsonTreeElementState.PARENT;
				this.children = new HashMap<String, JsonTreeElement>();
				this.children.put(childName, childElem);
			} else {
				this.children.put(childName, childElem);
			}
		}
		
		
		/**
		 * Sets the value of this JSONTreeElement to 
		 * the input String value. Will also set the 
		 * state flag to LEAF. Does nothing if this 
		 * element has a JSONTreeElement child(ren).
		 * 
		 * @param value - the value for this LEAF element
		 */
		public void setValue(String value) {
			if (state == JsonTreeElementState.PARENT) {
				return;
			} else if (state == JsonTreeElementState.NEW){
				this.state = JsonTreeElementState.LEAF;
			    this.value = value;
			} else {
				this.value = value;
			}
		}
		
		
		/**
		 * Returns the value of the JSONTreeElement. 
		 * The value will be null if the JSONTreeElement
		 * is not a LEAF element.
		 * 
		 * Sample usage:
		 * Given parser input {"status": "success"} that 
		 * returned variable elem, elem.get() will return
		 * the string "success".
		 * 
		 * @return the value of the LEAF element.
		 */
		public String get() {
		    return value;	
		}
		
		
		/** 
		 * Returns the JSONTreeElement corresponding to 
		 * give atribute value. 
		 * 
		 * Sample usage:
		 * Given parser input: 
		 * {"a" : {"b" : {"c" : {"d" : "e"}}}}
		 * that returned variable elem, 
		 * elem.get("a").get("b").get("c").get("d") will
		 * return the JSONTreeElement with value "e".
		 * The toString() method and the get() method will 
		 * return "e".
		 * 
		 * @param attr - the attribute tag
		 * @return the corresponding JSONTreeElement
		 */
		public JsonTreeElement get(String attr) {
			if (state == JsonTreeElementState.PARENT) 
     			return children.get(attr);
			return null;
		}
		
		
		/**
		 * Used by the toString method while building the 
		 * String representation of the JSON tree. 
		 * 
		 * @return whether the JsonTreeElement is a LEAF
		 */
		public boolean isLeaf() {
			return state == JsonTreeElementState.LEAF;
		}
		
		
		/**
		 * Overrided toString method that returns the String
		 * representation of a JSON object.
		 */
		@Override
		public String toString() {
			if (state == JsonTreeElementState.LEAF) {
				return value;
			} else if (state == JsonTreeElementState.NEW) {
				return "{}";
			} else {
				StringBuilder sb = new StringBuilder();
				for (String key : children.keySet()) {
					JsonTreeElement child = children.get(key);
					if (child.isLeaf()) 
						sb.append("\"" + key + "\": \"" + child.get() + "\", ");
					else
					    sb.append("\"" + key + "\": " + child.toString() + ", ");
				}
				return "{" + sb.substring(0, sb.length() - 2) + "}";
			}
		}
	}
	
	
	private static enum JsonTreeElementState {
		NEW,      // Empty {} 
		LEAF,     // Leaf value
		PARENT;   // JSON value 
	}
}
