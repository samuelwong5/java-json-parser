import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonParser {
	
	/**
	 * Parses a String representation of a JSON object
	 * and returns a JSONTreeElement for easier traversal.
	 * 
	 * Usage: JsonParser.parse(String json)
	 * 
	 * @param json - the String representation of a JSON object
	 * @return JsonElement representation of the JSON object
	 * @throws IllegalArgumentException if the input is malformed
	 */
	public JsonElement parse(String json) throws IllegalArgumentException {
		JsonTokenStream stream = new JsonTokenStream(json);
		JsonElement j = parseJson(stream);
		return j;
	}
	
	
	protected ArrayJsonElement parseArray(JsonTokenStream stream) throws IllegalArgumentException {
		stream.skipWhiteSpace();
		stream.match('[');
		ArrayJsonElement elem = new ArrayJsonElement();
		if (stream.peekToken().getId() == JsonToken.BRACKET_SQUARE_CLOSE) {
			stream.nextToken();
			return elem;
		}
		while (true) {
			stream.skipWhiteSpace();
			switch (stream.peekToken().getId()) {
			case JsonToken.BRACKET_SQUARE_OPEN:
				elem.add(parseArray(stream));
				break;
			case JsonToken.BRACKET_CURLY_OPEN:
				elem.add(parseJson(stream));
				break;
			case JsonToken.DOUBLE_QUOTE:
				elem.add(new LeafJsonElement(parseString(stream)));
				break;
			default:
			    throw new IllegalArgumentException("Malformed JSON at index " + stream.getIndex());
			}
			stream.skipWhiteSpace();
			if (stream.peekToken().getId() == JsonToken.BRACKET_SQUARE_CLOSE) {
				stream.nextToken();
				return elem;
			} else {
				stream.match(',');
			}
		}
	}
	
	
	protected JsonElement parseJson(JsonTokenStream stream) throws IllegalArgumentException {
	    stream.skipWhiteSpace();
		stream.match('{');
		JsonElement elem = new JsonElement();
		if (stream.peekToken().getId() == JsonToken.BRACKET_CURLY_CLOSE) {
			stream.nextToken();
			return elem;
		}
		while (true) {
			stream.skipWhiteSpace();
			String attribute_id = parseString(stream);
			stream.match(':');
			stream.skipWhiteSpace();
			switch (stream.peekToken().getId()) {
			case JsonToken.BRACKET_SQUARE_OPEN:
				elem.add(attribute_id, parseArray(stream));
				break;
			case JsonToken.BRACKET_CURLY_OPEN:
				elem.add(attribute_id, parseJson(stream));
				break;
			case JsonToken.DOUBLE_QUOTE:
				elem.add(attribute_id, new LeafJsonElement(parseString(stream)));
				break;
			default:
			    throw new IllegalArgumentException("Malformed JSON at index " + stream.getIndex());
			}
			stream.skipWhiteSpace();
			if (stream.peekToken().getId() == JsonToken.BRACKET_CURLY_CLOSE) {
				stream.nextToken();
				return elem;
			} else {
				stream.match(',');
			}
		}
		
	}
	
	protected String parseString(JsonTokenStream stream) throws IllegalArgumentException {
		stream.skipWhiteSpace();
		String s = "";
		stream.match('"');
		JsonToken t = stream.nextToken();
		while (t.getId() != JsonToken.DOUBLE_QUOTE) {
			s += t.getChar();
			t = stream.nextToken();
		}
		stream.skipWhiteSpace();
		return s;
	}
	
	
	private abstract class AbstractJsonElement {
		
		/**
		 * Returns the String value of LeafJsonElement
		 * contained in its _value_ member. If it is not
		 * a LeafJsonElement it should return the String
		 * representation of the JsonElement by calling
		 * the toString() method.
		 * 
		 * @return the String value of the element
		 */
		abstract String getValue();
		
		/** 
		 * Returns the AbstractJsonElement corresponding to 
		 * give attribute value of a JsonElement. If it is 
		 * not a JsonElement, it will return null.
		 * 
		 * Sample usage:
		 * Given parser input: 
		 * {"a" : {"b" : {"c" : {"d" : "e"}}}}
		 * that returned variable elem, 
		 * elem.get("a").get("b").get("c").get("d") will
		 * return the JSONTreeElement with value "e".
		 * The getValue() method will then return "e".
		 * 
		 * @param attr - the attribute tag
		 * @return the corresponding AbstractJsonElement
		 */
		abstract AbstractJsonElement get(String attr);
	}
	
	
	private class ArrayJsonElement extends AbstractJsonElement {
		private List<AbstractJsonElement> children;
		
		public ArrayJsonElement() {
			this.children = new ArrayList<AbstractJsonElement>();
		}
		
		public void add(AbstractJsonElement child) {
			this.children.add(child);
		}
		
		public List<AbstractJsonElement> getChildren() {
			return children;
		}
		
		@Override
		public String getValue() {
			return toString();
		}
		
		@Override 
		public AbstractJsonElement get(String attr) {
			return null;
		}
		
		@Override
		public String toString() {
			if (children.size() == 0)
				return "[]";
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (int i = 0; i < children.size() - 1; i++) {
				sb.append(children.get(i).toString() + ", ");
			}
			sb.append(children.get(children.size() - 1).toString());
			sb.append("]");
			return sb.toString();
		}
	}
	
	
	private class JsonElement extends AbstractJsonElement {
		private Map<String, AbstractJsonElement> children;
		
		public JsonElement() {
			this.children = new HashMap<String, AbstractJsonElement>();
		}
		
		public void add(String attribute, AbstractJsonElement child) {
			this.children.put(attribute, child);
		}
		
		@Override
		public String getValue() {
			return toString();
		}
		
		@Override 
		public AbstractJsonElement get(String attr) {
			return children.get(attr);
		}
		
		@Override
		public String toString() {
			if (children.size() == 0) {
				return "{}";
			}
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			Iterator<String> i = children.keySet().iterator();
			while (i.hasNext()) {
			    String key = i.next();
			    sb.append("\"" + key + "\":" + children.get(key).toString());
			    if (i.hasNext())
			    	sb.append(", ");
			}
			sb.append("}");
			return sb.toString();
		}
	}
	
	
	private class LeafJsonElement extends AbstractJsonElement {
		private String value;
		
		public LeafJsonElement(String value) {
			this.value = value;
		}
		
		@Override
		public String getValue() {
			return this.value;
		}
		
		@Override 
		public AbstractJsonElement get(String attr) {
			return null;
		}
		
		@Override
		public String toString() {
			return "\"" + this.value + "\"";
		}
	}
	
	
	private class JsonTokenStream {
		private List<JsonToken> tokens;
		private int index;
		
		public JsonTokenStream(String json) {
			this.tokens = new ArrayList<JsonToken>();
			for (char c : json.toCharArray()) {
			    this.tokens.add(new JsonToken(c));
			}
			this.index = 0;
		}
		
		public int getIndex() {
			return index;
		}
		
		public JsonToken peekToken() {
			return index >= tokens.size() ? null : tokens.get(index);
		}
		
		public JsonToken nextToken() {
			return index >= tokens.size() ? null : tokens.get(index++);
		}
		
		public JsonToken getLastToken() {
			return tokens.get(index - 1 > -1 ? index - 1 : 0);
		}
		
		public void skipWhiteSpace() {
			while (tokens.get(index).tokenId == JsonToken.WHITESPACE)
				index++;
		}
		
		public void match(char c) throws IllegalArgumentException {
			JsonToken t = nextToken();
			if (t.getChar() != c) 
				throw new IllegalArgumentException("Malformed JSON at index " + getIndex()
				                                       + "\nExpected: '" + c
				                                       + "'\nGot: '" + t.getChar() + "'");
		}
		
	}
	
	
	private class JsonToken {
		static final int BRACKET_CURLY_OPEN   = 0;
		static final int BRACKET_CURLY_CLOSE  = 1;
		static final int BRACKET_SQUARE_OPEN  = 2;
		static final int BRACKET_SQUARE_CLOSE = 3;
		static final int DOUBLE_QUOTE         = 4;
		static final int COMMA                = 5;
		static final int COLON                = 6;
		static final int WHITESPACE           = 7;
		static final int ID                   = 8;
		
		private int tokenId;
		private char tokenChar;
		
		JsonToken(char c) {
			switch (c) {
			case '{':
				this.tokenId = BRACKET_CURLY_OPEN;
				break;
			case '}':
				this.tokenId = BRACKET_CURLY_CLOSE;
				break;
			case '[':
				this.tokenId = BRACKET_SQUARE_OPEN;
				break;
			case ']':
				this.tokenId = BRACKET_SQUARE_CLOSE;
				break;
			case '"':
				this.tokenId = DOUBLE_QUOTE;
				break;
			case ',':
				this.tokenId = COMMA;
				break;
			case ':':
				this.tokenId = COLON;
				break;
			case ' ':
				this.tokenId = WHITESPACE;
				break;
			case '\t':
				this.tokenId = WHITESPACE;
				break;
			case '\n':
				this.tokenId = WHITESPACE;
				break;
			}
			this.tokenChar = c;
		}
		
		public int getId() {
			return tokenId;
		}
		
		public char getChar() {
			return tokenChar;
		}
		
	}
}
