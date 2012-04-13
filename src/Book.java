
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.PDFMerger;
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
public class Book {

    /**
     * Needed Variables
     */
    private String isbn, author, title;
    private ArrayList<String> chapterUrlList;
    private ArrayList<BufferedInputStream> chapterStreams = new ArrayList<BufferedInputStream>();
    /**
     * Constants
     */
    //Patterns needed for matching in html source
    private static final String htmlTitlePattern = "<h1[^<]+class=\"title\">(.+?)(?:<br/>\\s*<span class=\"subtitle\">(.+?)</span>\\s*)?</h1>";
    private static final String htmlChapterLinksPattern = "\"\\shref=\"/content/([^\"]+\\.pdf)\"";
    private static final String htmlAuthorPattern = "\"\\shref=\"/content/\\?Author=.+?\">(.+?)</a>";
    //Our faked browser
    private static final String userAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    //Springerlink
    private static final String baseUrl = "http://springerlink.com/content/";

    /**
     * Constructor that sets isbn and initiates everything till a merged pdf
     *
     * @param isbn
     */
    public Book(String isbn) {
        this.isbn = isbn;
        collectBookInfo();
        try {
            mergeChapters();
        } catch (Exception ex) {
            Logger.getLogger(Book.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
     * Collects the author, title and chapterlinks + downloads the chapters and
     * saves them as file
     */
    private void collectBookInfo() {
        String pageSource = getPageSource(baseUrl + isbn + "/contents/");
        title = getBookTitle(pageSource);
        chapterUrlList = getChapterLinks(pageSource);
        author = getBookAuthor(pageSource);
        try {
            int counter = 1;
            for (String chapterUrl : chapterUrlList) {
                downloadChapter("Chapter" + counter + ".pdf", chapterUrl);
                System.out.println("downloaded Chapter" + counter);
                counter++;
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Book.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Book.class.getName()).log(Level.SEVERE, null, ex);
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
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        URL u;

        File dir = new File(title);
        dir.mkdir();

        u = new URL(urlString);
        URLConnection test = u.openConnection();

        //We need to fake a browser, otherwise Springer blocks us ...
        test.setRequestProperty("User-Agent", userAgent);

        in = new BufferedInputStream(test.getInputStream());
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
        URL u;
        InputStream is = null;
        DataInputStream dis;
        String s;
        StringBuilder sb = new StringBuilder();

        try {
            u = new URL(theURL);
            URLConnection test = u.openConnection();

            //We need to fake a browser, otherwise Springer blocks us ...
            test.setRequestProperty("User-Agent", userAgent);

            is = test.getInputStream();

            dis = new DataInputStream(new BufferedInputStream(is));
            while ((s = dis.readLine()) != null) {
                sb.append(s + "\n");
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
     * Returns for a given html source the book author
     *
     * @param pageSource The string that contains the html source
     * @return String
     */
    private String getBookAuthor(String pageSource) {
        String author = "";

        Pattern searchForAuthor =
                Pattern.compile(htmlAuthorPattern, Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher m = searchForAuthor.matcher(pageSource);

        m.find();

        author = m.group(1);
        return author;
    }

    /**
     * Returns for a given html source the book title
     *
     * @param pageSource The string that contains the html source
     * @return String
     */
    private String getBookTitle(String pageSource) {
        String title = "";

        Pattern searchForTitle =
                Pattern.compile(htmlTitlePattern, Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher m = searchForTitle.matcher(pageSource);

        m.find();

        title = m.group(1).replaceAll("\u0009", "").replaceAll("\\n", "");// +" - " + m.group(2); //for subtitle
        return title;
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
