package gate.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import gate.tools.SimpleManualAnnotator.Mode;

public class Configuration {
	Mode mode = Mode.OPTIONSFROMSTRING;
	String inputASName = null;
	String contextType = "Sentence";
	String mentionType = "Mention";
	String optionsType = null;
	String optionsFeat = null;
	String optionsSeparator = null;
	String[] options = {"Positive", "Negative", "Unknown"};
	String outputASName = "Key";
	String outputFeat = "class";
	boolean includeNoneOfAbove = false;
	boolean includeSpurious = true;
	boolean autoadvance = true;
        boolean includeNIL = true;
        boolean includeNewValue = false;
        boolean undoneOnly = true;
        String listSeparator = null;

	Configuration (File conf) {
		populateConfigOptions(conf);
	}
	
    public void populateConfigOptions(File conf){
    	try {
    		BufferedReader br = new BufferedReader(new FileReader(conf));
    	    String line;
    	    while ((line = br.readLine()) != null) {
    	       if(!line.startsWith("#") && line.contains(" = ")){
    	    	   String[] pair = line.split("\\s+=\\s+",-1);
    	    	   switch (pair[0]) {
    	            case "mode":
    	            	mode = Mode.valueOf(pair[1]);
	                    break;
    	            case "inputASName":
    	            	inputASName = pair[1];
    	            	if(inputASName.equals("null")) inputASName = null;
	                    break;
    	            case "contextType":
    	            	contextType = pair[1];
	                    break;
    	            case "mentionType":
    	            	mentionType = pair[1];
	                    break;
    	            case "optionsType":
    	            	optionsType = pair[1];
	                    break;
    	            case "optionsFeat":
    	            	optionsFeat = pair[1];
	                    break;
    	            case "optionsSeparator":
    	            	optionsSeparator = pair[1];
	                    break;
    	            case "options":
    	            	options = pair[1].split(";");
	                    break;
    	            case "outputASName":
    	            	outputASName = pair[1];
    	            	if(outputASName.equals("null")) outputASName = null;
	                    break;
    	            case "outputFeat":
    	            	outputFeat = pair[1];
	                    break;
    	            case "includeNoneOfAbove":
    	            	includeNoneOfAbove = false;
    	            	if(pair[1].toLowerCase().equals("true")) includeNoneOfAbove = true;
	                    break;
    	            case "includeSpurious":
    	            	includeSpurious = false;
    	            	if(pair[1].toLowerCase().equals("true")) includeSpurious = true;
	                    break;
    	            case "includeNIL":
    	            	includeNIL = false;
    	            	if(pair[1].toLowerCase().equals("true")) includeNIL = true;
	                    break;
    	            case "includeNewValue":
    	            	includeNewValue = false;
    	            	if(pair[1].toLowerCase().equals("true")) includeNewValue = true;
	                    break;
    	            case "undoneOnly":
    	            	undoneOnly = true;
    	            	if(pair[1].toLowerCase().equals("false")) undoneOnly = false;
	                    break;
    	            case "listSeparator":
    	            	listSeparator = pair[1];
	                    break;
    	            case "autoadvance":
    	            	autoadvance = true;
    	            	if(pair[1].toLowerCase().equals("false")) autoadvance = false;
	                    break;
    	    	   }    	    			   
    	       }
    	    }
 	       br.close();
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
}
