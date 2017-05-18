import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Normalizer {

    private final String manifestPath;
    
    /**
     * Utilizes our section and row names to map to our section and row id's.
     * Way faster than an O(n) ArrayList search.
     */
    HashMap<SectionAndRowName, ArrayList<SectionAndRowId>> manifestMap;
    
    public Normalizer(String manifestPath) {
        this.manifestPath = manifestPath;
        manifestMap = new HashMap<SectionAndRowName, ArrayList<SectionAndRowId>>();
    }

    /**
     * reads a manifest file
     * manifest should be a CSV containing the following columns
     * * section_id
     * * section_name
     * * row_id
     * * row_name
     * @throws IOException 
     */
    public void readManifest() throws IOException {
        System.out.println("Reading from " + manifestPath);
        BufferedReader reader = null;
        
		try {
			reader = new BufferedReader(new FileReader(manifestPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
        String row = "";

        // Split our csv rows into 4 or 2 fields (depending on if rows are needed or not). 
        // Create an object for names, and id's, then let the names map to the id's.
        while((row = reader.readLine()) != null){
        	String[] columns = row.split(",");

        	SectionAndRowId id;
        	SectionAndRowName name;
        	
        	String sectionId = columns[0],
          		   sectionName = columns[1],
          		   uniqueChars = getSectionChars(sectionName);
        		 
        	
        	// Check that the rows are being used
        	if(columns.length > 2){
        		
        		String rowId = columns[2],
             		   rowName = columns[3];

        		id = new SectionAndRowId(sectionId, rowId, uniqueChars);
        		name = new SectionAndRowName(sectionName, rowName.toUpperCase());
        		
        	} else {
        		id = new SectionAndRowId(sectionId, null, uniqueChars);
        		name = new SectionAndRowName(sectionName, null);
        	}
        	
			addToManifest(name, id);
        }

    }
	
	/**
	* Adds a {name => list of ids} to the manifest
	* @param name - Section and row names that map to id's
	* @param id - Section and row id's with similar names
	*/
	public void addToManifest(SectionAndRowName name, SectionAndRowId id){
		// If we've found a similar rowName and sectionName before, add to the existing list, otherwise make a new list
        	// and add to it.
        if(manifestMap.containsKey(name)){
        	manifestMap.get(name).add(id);
        } else{
			ArrayList<SectionAndRowId> idList = new ArrayList<SectionAndRowId>();
        	idList.add(id);
        	manifestMap.put(name, idList);
        }
	}

    /**
     * normalize a single (section, row) input
     * Given a (Section, Row) input, returns (section_id, row_id, valid)
     * where
     * section_id = int or None
     * row_id = int or None
     * valid = True or False
     * Arguments:
     * section {[type]} -- [description]
     * row {[type]} -- [description]
     */
    public NormalizationResult normalize(String section, String row) {
        // initialize return data structure
        NormalizationResult r = new NormalizationResult();
        r.sectionId = -1;
        r.rowId = -1;
        r.valid = true;
        
        String sectionCopy = section, 
        	   rowCopy = row;
        
        String sectionName = findValidSection(sectionCopy);
        String rowName = findValidRow(rowCopy);
        
        SectionAndRowName name = new SectionAndRowName(sectionName, rowName);
        SectionAndRowId id = null;

        // Check our manifest. If the sectionName and rowName don't match any listing in the manifest, set it to invalid.
        if(manifestMap.containsKey(name)){
        	ArrayList<SectionAndRowId> relevantIds = manifestMap.get(name);
        	id = findMostRelevant(relevantIds, section);
        } else {
        	r.valid = false;
        }
        
        if(id == null){
        	r.valid = false;
        }
        
        // Set our rowId and sectionId to our data structure.
        if(r.valid){
        	//nullcheck
        	r.rowId = id.rowId != null ? Integer.parseInt(id.rowId) : r.rowId;
        	r.sectionId = id.sectionId != null ? Integer.parseInt(id.sectionId) : r.sectionId;
        }
        
        return r;
    }
    
    /**
     * finds the most relevant id's. The section String is compared to all
     * of our unique characters in the section name (i.e. characters that are not numeric). We check that the uniqueChar String
     * matches the section String by iterating over each char at the same index. If the two chars are equal, increment the 
     * relevancyCount. Return the row and id with the highest relevancy count.
     * 
     * @param relevantIds - list of ids with a section name that seems probable to match with the supplied section name.
     * @param section - section name supplied in normalize
     * @return most relevant id compared to the supplied section name, or null if nothing is relevant
     */
    public SectionAndRowId findMostRelevant(ArrayList<SectionAndRowId> relevantIds, String section){
		if(relevantIds.size() == 1){
			return relevantIds.get(0);
			
		} else {
			// Keeps track of most relevant id
			SectionAndRowId mostRelevant = null;
			int relevancyCount = 0;
			
			String sectionCopy = section;
			
			// Remove any non-alphabetical characters
			sectionCopy = getSectionChars(sectionCopy);
			
			// Count the characters in the section name
			HashMap<Character, Integer> sectionCharCount = countCharacters(sectionCopy);
			
			for(SectionAndRowId id : relevantIds){
				int count = 0;
				HashMap<Character, Integer> idCharCount = countCharacters(id.uniqueChars);
				
				// Compare occurences of each character from idCharCount and sectionCharCount
				for(Map.Entry<Character, Integer> c : idCharCount.entrySet()){
					// Do the same characters in the vendor's sectionName match the exact amount in the current id's sectionName?
					if(sectionCharCount.containsKey(c.getKey()) && sectionCharCount.get(c.getKey()) == c.getValue()){
						count += c.getValue();
					}
					
					if(count > relevancyCount){
						relevancyCount = count;
						mostRelevant = id;
					}
				}
			}
			// If the amount of characters that matched missed more than 100 characters, it doesn't seem worth the risk, so return null.
			mostRelevant = sectionCopy.length() - relevancyCount > 100 ? null : mostRelevant;
			
			return mostRelevant;
		}
    }
    
    /**
     * Counts the number of characters in the string
     * @param str - string whose characters are to be counted
     * @return hashmap with character -> number of occurences count
     */
    public HashMap<Character, Integer> countCharacters(String str){
		HashMap<Character, Integer> sectionCharCount = new HashMap<Character, Integer>();
		
		for(int index = 0; index < str.length(); index++){
			sectionCharCount.putIfAbsent(Character.toLowerCase(str.charAt(index)), 0);
			
			sectionCharCount.put(Character.toLowerCase(str.charAt(index)), sectionCharCount.get(Character.toLowerCase(str.charAt(index))) + 1);
		}
		return sectionCharCount;
    }

    /**
     * Checks if a section is valid, if there is a valid section, return the section, otherwise return an empty String.
     * A valid section exists if there is a numeric value that matches a numerica value in the manifest.
     * @param section - supplied section name
     * @return an empty string if no match is found, or a number extracted from the section name, or null.
     */
    public static String findValidSection(String section){
    	if(section == null){
    		return null;
    	}
    	
    	// Look for a numeric value first
    	Pattern pattern = Pattern.compile("\\d+"); 
    	Matcher match = pattern.matcher(section);

    	String sectionName = "";
    	
    	if(match.find()){
    		sectionName = match.group(0);
    	}
    	
    	return sectionName;
    }
    
    /**
     * uses regular expressions to check if a row name is valid. A row name is valid if it's a number from 1 to 10, 1 capital letter from A to Z, 
     * or 2 capital letters from A-D.
     * @param row
     * @return -1 if invalid, or the rowId if valid
     */
    public static String findValidRow(String row){
    	if(row == null){
    		return null;
    	}
    	
    	// Must be a number from 1 to 10, or 1 char from A to Z, or 2 chars from A-Z.
    	Pattern pattern = Pattern.compile("^[1-9]{0,1}[0-9]{1}$"
    									+ "|^[A-Za-z]{1}$"
    									+ "|^[A-Za-z]{2}$"); 
    	
    	Matcher match = pattern.matcher(row);
    	
    	String rowName = "";
    	
    	if(match.find()){
    		rowName = match.group(0).toUpperCase();
    	}
    	
		return rowName; 
    }
    
    /**
     * Removes all non-alphabetical characters from our section name. 
     * To be used when comparing similar section names.
     * 
     *
     * @param section - section name to be compared with each relevant id.
     * @return most relevant id's.
     */

    public static String getSectionChars(String section){
    	String sectionCopy = section;
    	// Remove everything but letters
    	return sectionCopy.replaceAll("[^a-zA-Z]", "");
    }

    public void normalize(ArrayList<SampleRecord> samples) {
        for (SampleRecord sample : samples) {
            NormalizationResult result = normalize(sample.input.section, sample.input.row);
            sample.output.sectionId = result.sectionId;
            sample.output.rowId = result.rowId;
            sample.output.valid = result.valid;
        }
    }
}