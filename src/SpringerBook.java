
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.util.PDFMergerUtility;


/*
 * A Class for downloading books from SpringerLink by the ISBN
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
public class SpringerBook extends Book {

    /**
     * Needed Variables
     */
    private ArrayList<BufferedInputStream> chapterStreams = new ArrayList<BufferedInputStream>();
    /**
     * Constants
     */
    //Patterns needed for matching in html source
    //private static final String htmlTitlePattern = "(<h1[^<]+class=\"title\">(.+?)?:<br/>)";
    private static final String htmlTitlePattern = "(<dt>Title</dt><dd>(.+?)<br/>)";
    private static final String htmlChapterLinksPattern = "\"\\shref=\"/content/([^\"]+\\.pdf)\"";
    private static final String htmlAuthorPattern = "(\"\\shref=\"/content/\\?Author=.+?\">(.+?)</a>)";
    //Springerlink
    private static final String baseUrl = "http://springerlink.com/content/";

    /**
     * Constructor that sets isbn and initiates everything till a merged pdf
     *
     * @param isbn
     */
    public SpringerBook(String isbn) {
        this.isbn = isbn;
        setHttpProxy();
        collectBookInfo();
        try {
            mergeChapters();
        } catch (Exception ex) {
            Logger.getLogger(SpringerBook.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Collects the author, title and chapterlinks + downloads the chapters and
     * saves them as file
     */
    private void collectBookInfo() {
        String pageSource = getPageSource(baseUrl + isbn + "/contents/");
        System.out.println(pageSource);
        title = getBookTitle(pageSource, htmlTitlePattern );
        chapterUrlList = getChapterLinks(pageSource);
        author = getBookAuthor(pageSource, htmlAuthorPattern);
        try {
            int counter = 1;
            for (String chapterUrl : chapterUrlList) {
                downloadChapter("Chapter" + counter + ".pdf", chapterUrl);
                System.out.println("downloaded Chapter" + counter);
                counter++;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(SpringerBook.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SpringerBook.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Downloads a chapter and saves it as a file in /booktitle/filename
     *
     * @param filename the filename to save the downloaded file
     * @param urlString the download link
     * @throws MalformedURLException
     * @throws IOException
     */
    private void downloadChapter(String filename, String urlString) throws MalformedURLException, IOException {
        URL u = new URL(urlString);
        URLConnection test = u.openConnection();

        //We need to fake a browser, otherwise Springer blocks us ...
        test.setRequestProperty("User-Agent", userAgent);

        BufferedInputStream in = new BufferedInputStream(test.getInputStream());
        chapterStreams.add(in);
    }

    /**
     * Merges each chapter into one file
     */
    private void mergeChapters() throws Exception {

        PDFMergerUtility merger = new PDFMergerUtility();

        for (BufferedInputStream in : chapterStreams) {
            merger.addSource(in);
        }
        merger.setDestinationFileName(title + ".pdf");
        merger.mergeDocuments(); //Error when not connected to VPN -> files are empty

        for (BufferedInputStream in : chapterStreams) {
            in.close();
        }
    }

    /**
     * Returns the HTML source code from an Url as string
     *
     * @param theURL The Url
     * @return String
     */
    private String getPageSource(String theURL) {
        DataInputStream dis;
        InputStream is = null;
        String s;
        StringBuilder sb = new StringBuilder();

        try {
            
            URL u = new URL(theURL);
            URLConnection test = u.openConnection();

            //We need to fake a browser, otherwise Springer blocks us ...
            test.setRequestProperty("User-Agent", userAgent);

            is = test.getInputStream();

            dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                sb.append(s).append("\n");
            }
        } catch (MalformedURLException mue) {
            System.out.println("Ouch - a MalformedURLException happened.");
            mue.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Oops- an IOException happened.");
            ioe.printStackTrace();
            System.exit(1);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
        return sb.toString();
    }

    /**
     * Returns for a given html source the link of each chapter
     *
     * @param pageSource The string that contains the html source
     * @return ArrayList<String>
     */
    public ArrayList<String> getChapterLinks(String pageSource) {

        ArrayList<String> returnList = new ArrayList<String>();

        Pattern searchForChapterLinks =
                Pattern.compile(htmlChapterLinksPattern, Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher matcher = searchForChapterLinks.matcher(pageSource);

        while (matcher.find()) {
            returnList.add(baseUrl + matcher.group(1));
        }

        return returnList;
    }
}
