
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
public class OldenbourgBook {

    /**
     * Needed Variables
     */
    private String isbn, author, title;
    private ArrayList<String> chapterUrlList;
    private ArrayList<BufferedInputStream> chapterStreams = new ArrayList<BufferedInputStream>();
    private List<String> cookies;
    /**
     * Constants
     */
    //Patterns needed for matching in html source
    private static final String htmlTitlePattern = "(<span class=\"author\">\\s*<b>.+?</b>\\s*<br>\\s*(.+?)<br />)";
    private static final String htmlChapterLinksPattern = "\"\\shref=\"/content/([^\"]+\\.pdf)\"";
    private static final String htmlAuthorPattern = "(<span class=\"author\">\\s*<b>(.+?)</b>)";
    //Our faked browser
    private static final String userAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";
    //Springerlink
    private static final String baseUrl = "http://www.oldenbourg-link.com/isbn/";

    /**
     * Constructor that sets isbn and initiates everything till a merged pdf
     *
     * @param isbn
     */
    public OldenbourgBook(String isbn) {
        this.isbn = isbn;
        setHttpProxy();
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
     * Method sets proxy and port to be able to reach campus network on http
     */
    private void setHttpProxy() {
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("proxyHost", "proxy.tfh-wildau.de");
        System.getProperties().put("proxyPort", "8080");
    }

    /**
     * Collects the author, title and chapterlinks + downloads the chapters and
     * saves them as file
     */
    private void collectBookInfo() {
        String pageSource = getPageSource(baseUrl + isbn);
        title = getBookTitle(pageSource);
        chapterUrlList = getChapterLinks(pageSource);
        System.out.println(pageSource);
        author = getBookAuthor(pageSource);
        for (String chapterUrl : chapterUrlList) {
            System.out.println(chapterUrl);
        }
        try {
            int counter = 1;
            for (String chapterUrl : chapterUrlList) {
                if (counter > 9) {
                    downloadChapter("Chapter" + counter + ".pdf", chapterUrl);
                } else {
                    downloadChapter("Chapter0" + counter + ".pdf", chapterUrl);
                }
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

        u = new URL(urlString);
        URLConnection test = u.openConnection();
        for (String cookie : cookies) {
            test.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
        }

        //We need to fake a browser, otherwise Springer blocks us ...
        test.setRequestProperty("User-Agent", userAgent);

        try {
            new File("temp").mkdir();
            in = new BufferedInputStream(test.getInputStream());
            fout = new FileOutputStream("temp/" + filename);

            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }

    /**
     * Merges each chapter into one file
     */
    private void mergeChapters() throws Exception {

        PDFMergerUtility merger = new PDFMergerUtility();

        File folder = new File("temp/");
        File[] listOfFiles = folder.listFiles();

        // Sort files by name
        Arrays.sort(listOfFiles, new Comparator() {

            @Override
            public int compare(Object f1, Object f2) {
                return ((File) f1).getName().compareTo(((File) f2).getName());
            }
        });

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                //System.out.println(listOfFiles[i].getName());
                merger.addSource("temp/"+listOfFiles[i].getName());
            }
        }

        merger.setDestinationFileName(title + ".pdf");
        merger.mergeDocuments();

        //if success -> delete folder temp
        if (!deleteDir(new File("temp")))//delete folder
            //folder could not be deleted exception
            ;
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
            // Gather all cookies on the first request.
            URLConnection connection = new URL("http://www.oldenbourg-link.com").openConnection();
            cookies = connection.getHeaderFields().get("Set-Cookie");

            // Then use the same cookies on all subsequent requests.
            connection = new URL(theURL).openConnection();
            for (String cookie : cookies) {
                connection.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
            }

            is = connection.getInputStream();

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

        author = m.group(2);
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

        title = m.group(2);
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

        Pattern searchForChapterLinks = // href="/doi/pdfplus/10.1524/9783486594089.fm">
                Pattern.compile("href=\"/doi/pdfplus/(.+?)\">", Pattern.DOTALL | Pattern.UNIX_LINES);

        Matcher matcher = searchForChapterLinks.matcher(pageSource);

        while (matcher.find()) {
            returnList.add("http://www.oldenbourg-link.com/doi/pdfplus/" + matcher.group(1));
        }

        return returnList;
    }
    
    /**
     * This method deletes a non empty directory
     * @param dir Dirname
     * @return boolean
     */
     public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    } 
}
