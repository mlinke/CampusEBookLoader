
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * A Class for downloading books by the ISBN
 *
 * Copyright (C) 2012 The CampusEBookLoader Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Marcel Linke @version 0.1
 */
public class Book {
    /**
     * Needed Variables
     */
    protected String isbn, author, title;
    protected ArrayList<String> chapterUrlList;
    protected List<String> cookies;
    
    //Our faked browser
    protected static final String userAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";

    /**
     * Setters and Getters
     */
    public String getIsbn() {
        return isbn;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }
    
    /**
     * Method sets proxy and port to be able to reach campus network on http
     */
    protected void setHttpProxy(){
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", "proxy.tfh-wildau.de");
        System.getProperties().put("proxyPort", "8080"); 
    }
    
    
    /**
     * Returns for a given html source the book author
     *
     * @param pageSource The string that contains the html source
     * @return String
     */
    protected String getBookAuthor(String pageSource, String _htmlAuthorPattern) {
        Pattern searchForAuthor =
                Pattern.compile(_htmlAuthorPattern, Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher m = searchForAuthor.matcher(pageSource);
        m.find();
        
        return m.group(2);
    }
    
    /**
     * Returns for a given html source the book title
     *
     * @param pageSource The string that contains the html source
     * @return String
     */
    protected String getBookTitle(String pageSource, String _htmlTitlePattern) {
        Pattern searchForTitle =
                Pattern.compile(_htmlTitlePattern, Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher m = searchForTitle.matcher(pageSource);
        m.find();

        return m.group(2);
    }
}
