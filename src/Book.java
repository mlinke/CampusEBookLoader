
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
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

    private String isbn, author, title;
    private ArrayList<String> chapters;
    private ArrayList<String> files;

    public Book(String isbn) {
        this.isbn = isbn;
        collectBookInfo();
        downloadChapters();
        mergeChapters();
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

    private void collectBookInfo() {
        String contents = downloadURL("http://springerlink.com/content/" + isbn + "/");  // a sample URL
        //System.out.print(contents);
        title = parseString(contents);
    }

    private void downloadChapters() {
        //TODO
    }

    private void mergeChapters() {
        //TODO
    }

    private String downloadURL(String theURL) {
        URL u;
        InputStream is = null;
        DataInputStream dis;
        String s;
        StringBuilder sb = new StringBuilder();

        try {
            u = new URL(theURL);
            URLConnection test = u.openConnection();
            test.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            is = test.getInputStream();
            //is = u.openStream();

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

    public String parseString(String s) {
        String output = "";
        
//       <h1 lang="de" class="title">
//			Programmierung naturanaloger Verfahren<br/>
//			<span class="subtitle">Soft Computing und verwandte Methoden</span>
//		</h1>                   <(\"[^\"]*\"|'[^']*'|[^'\">])*>

        Pattern searchForTitle =
        Pattern.compile("<h1[^<]+class=\"title\">(.+?)(?:<br/>\\s*<span class=\"subtitle\">(.+?)</span>\\s*)?</h1>",Pattern.DOTALL | Pattern.UNIX_LINES);
        Matcher m = searchForTitle.matcher(s);
        m.find();
        output = m.group(1).replaceAll("\u0009","").replaceAll("\\n","");// +" - " + m.group(2); //for subtitle
        return output;

    }
}
