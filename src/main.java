
/*
 * Copyright (C) 2012 The CampusEBookLoader Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Marcel Linke
 * @version 0.1
 */
public class main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //download it
        //Book myBook = new Book("978-3-8348-0822-6"); //test isbn which is open, no need for ciscovpn
        
        //test public methods
        //System.out.println(myBook.getTitle()+" ("+myBook.getAuthor()+")");
        //System.out.println(myBook.getIsbn());
        
        //download it
        OldenbourgBook myBook = new OldenbourgBook("978-3-486-27394-6"); //test isbn which is open, no need for ciscovpn
        
        //test public methods
        System.out.println(myBook.getTitle()+" ("+myBook.getAuthor()+")");
        System.out.println(myBook.getIsbn());
        
    }
}
