/*!
   Java Driver API for the ZTEX Firmware Kit
   Copyright (C) 2008-2009 ZTEX e.K.
   http://www.ztex.de

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License version 3 as
   published by the Free Software Foundation.

   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, see http://www.gnu.org/licenses/.
!*/

/* 
    creates an input stream from a file as system resource (i.e. from the current .jar file)
*/
package ztex;

import java.io.*;
import java.util.*;
import java.net.*;

public class JInputStream {

    static public InputStream getInputStream ( String fileName ) throws SecurityException, FileNotFoundException {
	InputStream is = null;
	try {
// Step 1: Normal way (i.e. from the current directory)
	    is = new FileInputStream(fileName);
	}
	catch ( FileNotFoundException e ) {
// Step 2: As system ressource
	    is = ClassLoader.getSystemResourceAsStream( fileName );
	}
	if ( is == null )
	    throw new FileNotFoundException("File not found: "+fileName);
	    
	return is;
    }
}    

