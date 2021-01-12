/*
Program: Craigslist Scraper
This: Web.java
Date: 01/06/2020
Purpose: 
This class contains all the necessary webscraping functions as well as any of 
there direct dependencies unless they belong to the Validate class. 
*/
package scraper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Web {
       
  
/*
=============================== Web Class Functions ============================
*/    
    
    /*
    ======================== String[][] createGasTabl() ========================
    This method returns a 2d array that contains the Name of the state, 
    abbreviation for the state, as well as the avg fuel cost in the state for 
    regular unleaded as of that moment.
    */
    public static String[][] createGasTable()
    {
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String url = "https://www.gasbuddy.com/usa";
        
        //creates list of elements for each state that contains states name
        //(full and abbrev.) as well as the avg prive of fuel currently in that
        //state
        List<HtmlElement> elements = null;
        try{
            HtmlPage page = client.getPage(url);
            elements = page.getByXPath("//*[@class=\"col-sm-12 col-xs-12\"]/a");
        }
        catch(IOException e){
            System.out.println("Gas table not available.");
        }
        
        String[][] table = new String[52][3];
        
        for (int index = 0; index < table.length; index++) {
            HtmlElement element = elements.get(index);
            HtmlElement siteName = elements.get(index).getFirstByXPath
            (".//*[@class=\"col-sm-6 col-xs-6 siteName\"]");
            
            String stateName = siteName.asText();
            String stateCode = elements.get(index).getAttribute("id");
            HtmlElement avgPriceElement = 
                   elements.get(index).getFirstByXPath
                   (".//*[@class=\"col-sm-2 col-xs-3 text-right\"]");
            
            String avgPrice = avgPriceElement.asText();
            // set the values
            table[index][0] = stateCode;
            table[index][1] = stateName;
            table[index][2] = avgPrice;
            
            //creates array such as this
        //           [stateAbbr] [stateAbbr] 
        //           [stateName] [stateName] 
        //           [avgPrice]  [avgPrice]  
        }
        return table;
    }
    
    /*
    ========================== State[] createClStateArray() ===================
    Function takes the array that is generated by createGasTable() and creates an
    array of State objects that will be referenced for all later searches. 
    This method is dependent upon several of the functions in this progam. 
    Dependencies:
    cleanStateRegionLists()
    createUsStateList()
    createUsStateList()
    createStateObjectArray()
    **These methods will follow this function directly in order
    */
    public static State[] createClStateArray(String[][] gasTable){
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String url = "https://www.craigslist.org/about/sites";
        ArrayList<State> states = new ArrayList<>();
        
        try{
            HtmlPage page = client.getPage(url);
            HtmlElement usXml = page.getFirstByXPath("//*[@class=\"colmask\"]");
            
            List<HtmlElement> stateXmlList = usXml.getByXPath(".//*/h4");
            
            List<HtmlElement> stateRegionXmls = usXml.getByXPath(".//*/ul");
            
            cleanStateRegionLists(stateXmlList,stateRegionXmls);
            
            String[] stateArray = createUsStateList(stateXmlList);
            
            String[] regionArray = new String[stateRegionXmls.size()];
            
            State[] stateObjectArray = createStateObjectArray(stateArray, 
                    stateRegionXmls, gasTable);
            //printStateArray(stateObjectArray);
            System.out.println("\n");
            return stateObjectArray;
            
        }
        catch(IOException e){
            System.out.println("Unable to generate Craigslist global homepage data. ");
            return null;
        }
    }
    
    /*
    ==================== void cleanStateRegionLists() =========================
    takes 2 ArrayLists of HtmlElements that contains craigslist htmlelements,
    one of the state information and the other of the regions. 
    This method iterates over the lists and removes the non continental states 
    and the respective regions that would belong with them. If you can DRIVE
    to the state then it should be on the resulting lists. 
    *Note: takes advantage of the java convention that arraylists are changed 
    at there position in memory and not a copy of it is passed to the function. 
    */
    private static void cleanStateRegionLists(List<HtmlElement> states, 
            List<HtmlElement> regions ){
        for (int count = 0; count < states.size(); count++) {
            String stateString = states.get(count).asText();
            if(stateString.equals("Hawaii") || stateString.equals("Territories")){
                states.remove(count);
                regions.remove(count);
            }
        }
    }
    
    
    /*
    =========================== String[] createUsStateList() ==================
    Method takes List<HtmlElement> of elements that contain craigslist
    list of us states as argument and converts to String[]. THis is for 
    continental US only. Excludes territories and Hawaii. Includes Alaska. 
    CanYouDrive rule
    */
    private static String[] createUsStateList(List<HtmlElement> xmlList){
        ArrayList<String> list = new ArrayList<>();
        for (int count = 0; count < xmlList.size(); count++) {
            String state = xmlList.get(count).asText().toUpperCase().trim();
            if(!state.equals("HAWAII") && !state.equals("TERRITORIES")){
                list.add(state);
            }
        }
        String[] array = new String[list.size()];
        for (int count = 0; count < array.length; count++) {
            array[count] = list.get(count);
        }
        return array;
    }
    
     /*
    ============================ State[] createStateArray() ===================
    takes a String[] of state names as well as the same HtmlElement that contains
    the state name as well as the associated craigslist url. 
    Dependencies:
    createState()
    */
    private static State[] createStateObjectArray(String[] states, 
            List<HtmlElement> elements, String[][] gasTable){
        ArrayList<State> stateList = new ArrayList<>();
        
        for (int count = 0; count < states.length; count++) {
            
            
            System.out.print("\n" + states[count]);//Printing state
            
            
            String[] regions = createRegionList(elements.get(count));
            String[] regionUrls = createRegionUrlList(elements.get(count));
            String[] regionZips = getZipArray(regions,states[count]);
            double avgGas = 0;
        String stateAbbr = "";
            for (String[] gasTable1 : gasTable) {
                if (gasTable1[1].toLowerCase().equals(states[count].toLowerCase())) {
                    avgGas = Double.parseDouble(gasTable1[2]);
                    stateAbbr = gasTable1[0];
                }
                if (states[count].toLowerCase().trim().equals("district of columbia")) {
                    if (gasTable1[1].toLowerCase().trim().equals("washington dc")) {
                        avgGas = Double.parseDouble(gasTable1[2]);
                        stateAbbr = gasTable1[0];
                    }
                }
            }
            stateList.add(createState(states[count],stateAbbr, avgGas, 
                    regions, regionUrls, regionZips));
        }
        
        State[] array = stateList.toArray(new State[stateList.size()]);
        return array;
    }
    
    /*
    ++++++++++++++++++++ Dependency for createStateObjectArray() ++++++++++++++
    */
    /*
    ============================== State createState() ========================
    takes in a string of the state name, String array of all thats states 
    craigslist regions that matches in indices with the String[] of region urls 
    for craigslist. 
    */
    private static State createState(String state, String stateAbbr, 
                                     double avgGas, String[] regions,
                                     String[] regionUrl, String[] regionZip){
        State newState = new State(state);
        String[][] array = new String[regions.length][3];
        for (int count = 0; count < array.length; count++) {
            array[count][0]= regions[count];
            array[count][1] =regionUrl[count];
            array[count][2] = regionZip[count];
        }
        newState.setRegions(array);
        newState.setFuelPrice(avgGas);
        newState.setAbbr(stateAbbr);
        return newState;
    }
    
    //+++++++++++++++ End Dependency for createStateObjectArray() ++++++++++++++
    
    /*
    =========================== String[] createRegionList() ===================
    Takes an HtmlElement as argument. This should be element from craigslist 
    home site that contains the state and its regions with the urls. This returns
    String[] of the region names. 
    */
    private static String[] createRegionList(HtmlElement element){
        ArrayList<String> list = new ArrayList<>();
        List<HtmlElement> elementList = element.getByXPath(".//a");
        for (int count = 0; count < elementList.size(); count++) {
            String text = elementList.get(count).asText();
            list.add(text);
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }
    
    
    /*
    ======================== String[] createRegionUrlList() ===================
    Takes an HtmlElement as argument. This should be element from craigslist 
    home site that contains the state and its regions with the urls. This returns
    String[] of the urls. 
    */
    private static String[] createRegionUrlList(HtmlElement element){
       ArrayList<String> list = new ArrayList<>();
        List<HtmlAnchor> anchorList = element.getByXPath(".//a");
        for (int count = 0; count < anchorList.size(); count++) {
            String text = anchorList.get(count).getHrefAttribute();
            list.add(text);
        }
        String[] array = list.toArray(new String[list.size()]);
        return array;
    }
    /*
    ======================= String[] getZipArray() ============================
    takes the String[] that is compiled of the craigslist region names, and 
    the given state to produce an array of the relating zip codes to be associated 
    with that region. 
    **We have to make some assumptions here for the application an assume that 
    a region's name may be associated with a location that is specific town in the
    same area to be able to retrieve a zip code. 
    Ex. In California there is Humbolt county. Making the assumption that
    if there is a zipcode for a Humbolt, California, then it would be an appropriate
    zip code to associate with this Craigslist region.
    Dependecies:
    cleanRegionArray()
    isWithCounty()
    getZipWithCounty()
    
    */
    private static String[] getZipArray(String[] regions, String state){
        String[] cleanRegions = cleanRegionArray(regions);
        String[] zipArray = new String[regions.length]; 
        for(int index = 0; index < zipArray.length; index++){
            if(isWithCounty(cleanRegions[index])){
                zipArray[index] = getZipWithCounty(cleanRegions[index],state);
            }
            else
                zipArray[index] = getZip(cleanRegions[index], state);
        }
        return zipArray;
    }
    
    /*
    ++++++++++++++++++ Dependencies for getZipArray() ++++++++++++++++++++++++++
    */
    
    /*
    ========================= String[] cleanRegionArray() ======================
    Takes an array of strings of the craigslist regions and finds all regions that
    contain a '/' or '-' in them, drops this character and all proceeding text.
    This is done because you can easily find the zipcode associated with 
    Ex. Clovis, NM but not as well for Clovis/Portales ,NM. There should be no 
    appreciable difference with relation to zipcode proximity. 
    Dependency:
    Validate.cleanRegion()
    
    */
    private static String[] cleanRegionArray(String[] regions){
        String[] cleanRegions = new String[regions.length];
        for (int index = 0; index < cleanRegions.length; index++) {
            cleanRegions[index] = Validate.cleanRegion(regions[index]);
        }
        return cleanRegions;
        
    }

    /*
     ======================== Boolean isWithCounty() ===========================
    takes a String region as argument and returns boolean value for the presence
    of the text "county" in the string
    */    
    private static Boolean isWithCounty(String region){
        if(region.contains("county"))
            return true;
        else
            return false;
    }
    
    /*
    ========================= String getZipWithCounty() ========================
    This method takes takes a string that has been verified to contain 
    the word county as well as the String state to find the correct zip code
    associated with that county. This makes the assumption that a county name may 
    very well be either unique in the state or a prominent town or city in that 
    given state. 
    */
    private static String getZipWithCounty(String county, String state){
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String baseUrl = "http://uscounties.com/zipcodes/search.pl?query=";
        String cleanCounty = dropCounty(county);
        try{
            HtmlPage page = client.getPage(baseUrl + cleanCounty + "+" 
                                           + state.toLowerCase() 
                                           + "&stpos=0&stype = AND");
            HtmlElement element = page.getFirstByXPath("//*[@class=\"results\"]/td");
            if(element.asText().isEmpty()){
                cleanCounty = "NO DATA";
            }
            else
                cleanCounty = element.asText();
        }
        catch(IOException e){
            System.out.println("Unable to produce zipcode for county: " + county);
            cleanCounty = "NA";
        }
        return cleanCounty;
    }
    /*
    =========================== Sting dropCounty() =============================
    Takes a string with a county included and drops the "county" from it. To be 
    later used with the above methods. 
    */
     public static String dropCounty(String countyName){
        int index = countyName.indexOf("county", 0);
        String woCounty = countyName.substring(0, index);
        return woCounty.trim();
    }
     /*
     ============================== String getZip() ============================
     Takes a string region and a string for state and return an associates zip.
     If this function cannot find an associates zip then it will return "NA".
     
     */
     public static String getZip(String region, String state){
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String baseUrl = "http://uscounties.com/zipcodes/search.pl?query=";
        String zip = "";
        try{
          HtmlPage page = client.getPage(baseUrl + region + "+" 
                                           + state.toLowerCase() 
                                           + "&stpos=0&stype = AND");
          HtmlElement element = page.getFirstByXPath("//*[@class=\"results\"]/td");
            
          zip = element.asText();
          zip = element.asText();
        }
        catch(IOException | NullPointerException e){
          zip = "NA";
        }
        
        return zip;
        
    }
     /*
     ++++++++++++++++++++ End Dependencies for getZipArray() +++++++++++++++++++
     */
    
     /*
    =========================== String getState() =============================
    Takes a 5 digit zipcode as a String as an argument and returns the state 
    abbreviation for the state it belongs to in all uppercase.
    Ex. If the state is Illinois the returned abbreviation will be IL.
     Dependencies: 
     getCityState()
    */
    public static String getState(String zipcode)
    {
        String cityState = getCityState(zipcode);
        int index = cityState.indexOf(",");
        String state = cityState.substring(index + 1).trim();
        return state;
    }
    
    /*
    ========================== String getCityState() ==========================
    This method takes a 5 digit zipcode as a string and finds the city and state and 
    returns this string in this form: Ex. "Montgomery, AL"
    */
    public static String getCityState(String zipcode){
        
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        String url = "https://www.unitedstateszipcodes.org/" + zipcode + "/";
        try{
            HtmlPage page = client.getPage(url);
            HtmlElement cityStateElement = ((HtmlElement) page.getFirstByXPath(".//*[@class = 'dl-horizontal']/dd"));
            String cityState = cityStateElement.asText();
            return cityState;
        }
        catch(IOException e)
        {
            return null;
        }
    }
    /*
    ============================ State getStateObject() ========================
    Takes the state abbreviation and the State[] containing all state objects 
    and references the stateAbbr attribute for each State object in the array. 
    Once it finds the correct state, it returns that State object. 
    */
    public static State getStateObject(State[] states, String stateAbbr){
        String abbr = stateAbbr.toUpperCase();
        State state = new State();
        for (State state1 : states) {
            if (abbr.equals(state1.getAbbr())) {
                state = state1;
            }
        }
        return state;
    }
    /*
    ============================ void printSearchOutput() ======================
    This method is responsible for parsing the given search results page and
    creating Item objects for all of the specific results. This scrapes the search
    results page and then assigns the appropriate values for the Item attributes. 
    This also run a for loop that prints each Item object in a user friendly way. 
    */
    public static void printSearchOutput(String searchUrl, State state, 
            double toZipDist, String mileage){
        WebClient client = new WebClient();
            client.getOptions().setJavaScriptEnabled(false);
            client.getOptions().setCssEnabled(false);
            client.getOptions().setUseInsecureSSL(true);
        try{
          HtmlPage page = client.getPage(searchUrl);
          List<HtmlElement> searchItems = page.getByXPath("//li[@class='result-row']");
          System.out.println("");
            if(searchItems.isEmpty())
              System.out.println("No items Available with that searchterm");
              else{
                System.out.println("----------------------------------------------------");
                System.out.println("                   Search Results");
                System.out.println("====================================================");
                System.out.println("Number of items returned in search: " + searchItems.size());
                System.out.println("----------------------------------------------------");
                System.out.println("******************************************");
                  for(int count = 0; count < searchItems.size(); count++ ){
                    HtmlElement searchItem = searchItems.get(count);
                    HtmlAnchor urlAnchor = (HtmlAnchor)searchItem.getFirstByXPath(".//a");
                    Item item = Web.createItem(searchItem, urlAnchor, state, toZipDist, mileage);
                    item.printItem();
                    }
                    client.close();
                }
        }
        catch(IOException e){
            System.out.println("******Webpage unavailable."
                    + "Try again later******* ");
            client.close();
        }
    }
    
    /*
    ============================ String getLocation() ==========================
    Takes the HtmlElement for a specific item on the search results page and 
    returns the items location if available. Craigslist identifies the location
    differently depending on the proximity to a metro area(I'm guessing) so must
    prepare for this slight change if any zip code will remain searchable. 
    */
    private static String getLocation(HtmlElement item)
    {
        try{
        String location;
        HtmlElement locationElement = (HtmlElement)item.getFirstByXPath(".//*[@class='nearby']");
        
        if(locationElement == null){
            locationElement = (HtmlElement)item.getFirstByXPath(".//*[@class='result-hood']");
             location = locationElement.asText();
        }
        else
            location = locationElement.getAttribute("title");
        return location;
        }
        catch(NullPointerException e) {
            return "Location Unavailable";
        }       
    }
    
    
    
    /*
    =========================== double zipToZipDist() =========================
    method determines the driving distance between the home zip and the 
    zip code that is being searched.
    */
    public static double zipToZipDist(String homeZip, String searchZip){
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        double distance;
        String url = "https://zipdistance.com/how-far-from-" + homeZip + "-to-" + searchZip;
        try{
            
            HtmlPage page = client.getPage(url);
            HtmlElement dist = page.getFirstByXPath("/html/body/h2[1]/strong");
            distance = Double.parseDouble(dist.asText());
            return distance;
        }
        catch(IOException e){
            System.out.println("Unable to provide apx total distance. ");
            return 0.0;
        }
        
    }
    /*
    ============================= Item createItem() ============================
    This is basically the constructor for the Item object. The reason for it 
    not being in the Item class is that it relies on several other methods that
    are housed in this class that I would like to remain private and this was the
    easiest way to maintain that. Also, this allows the item class to be free 
    from any additional import statements. 
    */
    private static Item createItem(HtmlElement item, HtmlAnchor anchor, State state, double toZipDist, String mileage){
        Item uniqueItem = new Item();
        uniqueItem.setUrl(anchor.getHrefAttribute());
        HtmlAnchor titleAnchor= (HtmlAnchor)item.getFirstByXPath
        (".//*[@class='result-heading']/a");
        uniqueItem.setDescription(titleAnchor.asText());
        HtmlElement spanPrice = ((HtmlElement)item.getFirstByXPath
        (".//*[@class = 'result-price']"));
        String price = spanPrice.asText().replaceAll("[^0-9]", "");
        uniqueItem.setPrice(Double.parseDouble(price));
        HtmlElement distAnchor = (HtmlElement)item.getFirstByXPath
        (".//*[@class='maptag']");
        String distance = distAnchor.asText().replaceAll("[^0-9.]", "");
                        
        double dist = Double.parseDouble(distance);
        if(dist >= toZipDist)
            uniqueItem.setMilesAwayMin(roundUp(dist - toZipDist));
        if(dist < toZipDist)    
            uniqueItem.setMilesAwayMin(roundUp(toZipDist - dist));
        uniqueItem.setMilesAwayMax(roundUp(dist + toZipDist));
        uniqueItem.setLocation(getLocation(item));
        
        uniqueItem.setFuelCostMin(roundUp(((uniqueItem.getMilesAwayMin() /
                Double.parseDouble(mileage)) * state.getFuelPrice()) *2));
        uniqueItem.setFuelCostMax(roundUp(((uniqueItem.getMilesAwayMax() / 
                Double.parseDouble(mileage)) * state.getFuelPrice()) *2));
        
        return uniqueItem;
        
    }
    /*
    ========================== double roundUp() ================================
    Method simply takes any double with value on the right of the decimal and 
    rounds up to the nearest hundredth. Returns the new cleaned double value. 
    */
    private static double roundUp(double number){
        double newNumber = (int)(number * 100 +.05);
        newNumber = newNumber / 100;
                           //(int)(number * 100 + .05) / 100.0;
        return newNumber;
    }



    
    
}


