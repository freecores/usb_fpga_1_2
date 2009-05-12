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
    Upload firmware to Cypress EZ-USB device
*/
package ztex;

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;

public class EzUsb {
// ******* reset **************************************************************
    public static void reset ( int handle, boolean r ) throws FirmwareUploadException {
	byte buffer[] = { (byte) (r ? 1 : 0) };
	int k = LibusbJava.usb_control_msg(handle, 0x40, 0xA0, 0xE600, 0, buffer, 1, 1000);   // upload j bytes
	if ( k<0 ) 
	    throw new FirmwareUploadException( LibusbJava.usb_strerror() + ": unable to set reset="+buffer[0] );
	else if ( k!=1 ) 
	    throw new FirmwareUploadException( "Unable to set reset="+buffer[0] );
	try {
    	    Thread.sleep( 50 );
	}
	    catch ( InterruptedException e ) {
	}
    }
     
// ******* uploadFirmware ******************************************************
//  returns upload time in ms
    public static long uploadFirmware (int handle, IhxFile ihxFile ) throws FirmwareUploadException {
	final int transactionBytes = 256;
	byte[] buffer = new byte[transactionBytes];
	long t0 = new Date().getTime();

	reset( handle, true );  // reset = 1
	
	int j = 0;
	for ( int i=0; i<=ihxFile.ihxData.length; i++ ) {
	    if ( i >= ihxFile.ihxData.length || ihxFile.ihxData[i] < 0 || j >=transactionBytes ) {
		if ( j > 0 ) {
		    int k = LibusbJava.usb_control_msg(handle, 0x40, 0xA0, i-j, 0, buffer, j, 1000);   // upload j bytes
		    if ( k<0 ) 
			throw new FirmwareUploadException(LibusbJava.usb_strerror());
		    else if ( k!=j ) 
			throw new FirmwareUploadException();
		    try {
		        Thread.sleep( 1 );	// to avoid package loss
		    }
			catch ( InterruptedException e ) {
		    }
		}
		j = 0;
	    }

	    if ( i < ihxFile.ihxData.length && ihxFile.ihxData[i] >= 0 && ihxFile.ihxData[i] <= 255 ) {
		buffer[j] = (byte) ihxFile.ihxData[i];
		j+=1;
	    }
	}

	try {
	    EzUsb.reset(handle,false);		// error (may caused re-numeration) can be ignored
	}
	catch ( FirmwareUploadException e ) {
	}
	return new Date().getTime() - t0;
    }
}    

