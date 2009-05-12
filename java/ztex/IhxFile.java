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
    Reads an ihx file
*/
package ztex;

import java.io.*;
import java.util.*;
import java.net.*;

public class IhxFile {
    public short ihxData[] = new short[65536];
    
// ******* readHexDigit ********************************************************
    private static final int readHexDigit( InputStream in ) throws IOException, IhxParseException {
        int b = in.read();
	if ( b>=(byte) '0' && b<=(byte) '9' )
	    return b-(byte) '0';
	if ( b>=(byte) 'a' && b<=(byte) 'f' )
	    return 10+b-(byte) 'a';
	if ( b>=(byte) 'A' && b<=(byte) 'F' )
	    return 10+b-(byte) 'A';
	if ( b == -1 )
	    throw new IhxParseException( "Inexpected end of file" );
	throw new IhxParseException( "Hex digit expected: " + (char) b );
    }

// ******* readHexByte *********************************************************
    private static final int readHexByte(InputStream in) throws IOException, IhxParseException {
	return (readHexDigit(in) << 4) | readHexDigit(in);
    }
    
// ******* IhxFile *************************************************************
    public IhxFile ( String fileName ) throws IOException, IhxFileDamagedException {
	InputStream in = JInputStream.getInputStream( fileName );
	int b, len, cs, addr;
	byte buf[] = new byte[255];
	boolean eof = false;
	int line = 0;
	
	for ( int i=0; i<ihxData.length; i++ )
	    ihxData[i] = -1;
	
	try {
	    while ( ! eof ) {
		do	{
		    b = in.read();
		    if ( b<0 )
			throw new IhxParseException( "Inexpected end of file" );
		} while ( b != (byte) ':' );
		
		line ++;

		len = readHexByte(in);		// length field 
		cs = len;
	    
		b = readHexByte(in);		// address field 
		cs += b;
		addr = b << 8;
		b = readHexByte(in);
		cs += b;
		addr |= b;
	    
		b = readHexByte(in);		// record type field
		cs += b;
	    
		for ( int i=0; i<len; i++ ) {	// data
		    buf[i] = (byte) readHexByte(in);
		    cs+=buf[i];
		}
	    
		cs += readHexByte(in);		// checksum
		if ( (cs & 0xff) != 0 ) {
		    throw new IhxParseException( "Checksum error" );
		}
	    
		if ( b == 0 ) {			// data record
		    for (int i=0; i<len; i++ ) {
			if ( ihxData[addr+i]>=0 ) System.err.println ( "Warning: Memory at position " + Integer.toHexString(i) + " overwritten" );
			ihxData[addr+i] = (short) (buf[i] & 255);
		    }
		}
		else if (b == 1 ) {		// eof record
		    eof = true;
		}
		else {
		    throw new IhxParseException( "Invalid record type: " + b );
		}
	    }
	}
	catch ( IhxParseException e ) {
	    throw new IhxFileDamagedException ( fileName, line, e.getLocalizedMessage() );
	}

	try {
	    in.close();
	}
	catch ( Exception e ) {
	    System.err.println( "Warning: Error closing file " + fileName + ": " + e.getLocalizedMessage() );
	}
    }

// ******* dataInfo ************************************************************
    public void dataInfo( PrintStream out ) {
	int addr=-1;
	for ( int i=0; i<=65536; i++ ) {	// data
	    if ( (i==65536 || ihxData[i]<0) && addr>=0 ) {
		System.out.println( i-addr + " Bytes from " + Integer.toHexString(addr) + " to " + Integer.toHexString(i-1) );
		addr = -1;
	    }
	    if ( i<65536 && ihxData[i]>=0 && addr<0 ) 
		addr = i;
	}
    }

}    

