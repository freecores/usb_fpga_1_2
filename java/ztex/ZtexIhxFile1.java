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
    Reads an ihx file with ZTEX descriptor 1
*/
package ztex;

import java.io.*;
import java.util.*;

public class ZtexIhxFile1 extends IhxFile {
    private static final int defaultZtexDescriptorOffs = 0x6c;

    private int ztexDescriptorOffs = defaultZtexDescriptorOffs;

    private byte productId[] = { 0,0,0,0 };	// product ID from the ZTEX descpriptor, not the USB product ID
    private byte fwVersion = 0;
    private byte interfaceVersion = 0;
    private byte interfaceCapabilities[] = { 0,0,0,0, 0,0 };
    private byte moduleReserved[] = { 0,0,0,0, 0,0,0,0, 0,0,0,0 };
    private char snString[] = new char[10];

// ******* ZtexIhxFile1 ********************************************************
    public ZtexIhxFile1( String fileName, int pZtexDescriptorOffs ) throws IOException, IhxFileDamagedException, IncompatibleFirmwareException {
	super( fileName );

	ztexDescriptorOffs = pZtexDescriptorOffs;
	
	if ( ihxData[ztexDescriptorOffs]!=40 || ihxData[ztexDescriptorOffs+1]!=1 || ihxData[ztexDescriptorOffs+2]!='Z' || ihxData[ztexDescriptorOffs+3]!='T' || ihxData[ztexDescriptorOffs+4]!='E' || ihxData[ztexDescriptorOffs+5]!='X' )
	    throw new IncompatibleFirmwareException( "Invalid ZTEX descriptor" );
	    
	productId[0] = (byte) ihxData[ztexDescriptorOffs+6];
	productId[1] = (byte) ihxData[ztexDescriptorOffs+7];
	productId[2] = (byte) ihxData[ztexDescriptorOffs+8];
	productId[3] = (byte) ihxData[ztexDescriptorOffs+9];
	fwVersion = (byte) ihxData[ztexDescriptorOffs+10];
	interfaceVersion = (byte) ihxData[ztexDescriptorOffs+11];
	interfaceCapabilities[0] = (byte) ihxData[ztexDescriptorOffs+12];
	interfaceCapabilities[1] = (byte) ihxData[ztexDescriptorOffs+13];
	interfaceCapabilities[2] = (byte) ihxData[ztexDescriptorOffs+14];
	interfaceCapabilities[3] = (byte) ihxData[ztexDescriptorOffs+15];
	interfaceCapabilities[4] = (byte) ihxData[ztexDescriptorOffs+16];
	interfaceCapabilities[5] = (byte) ihxData[ztexDescriptorOffs+17];
	moduleReserved[0] = (byte) ihxData[ztexDescriptorOffs+18];
	moduleReserved[1] = (byte) ihxData[ztexDescriptorOffs+19];
	moduleReserved[2] = (byte) ihxData[ztexDescriptorOffs+20];
	moduleReserved[3] = (byte) ihxData[ztexDescriptorOffs+21];
	moduleReserved[4] = (byte) ihxData[ztexDescriptorOffs+22];
	moduleReserved[5] = (byte) ihxData[ztexDescriptorOffs+23];
	moduleReserved[6] = (byte) ihxData[ztexDescriptorOffs+24];
	moduleReserved[7] = (byte) ihxData[ztexDescriptorOffs+25];
	moduleReserved[8] = (byte) ihxData[ztexDescriptorOffs+26];
	moduleReserved[9] = (byte) ihxData[ztexDescriptorOffs+27];
	moduleReserved[10] = (byte) ihxData[ztexDescriptorOffs+28];
	moduleReserved[11] = (byte) ihxData[ztexDescriptorOffs+29];

	for (int i=0; i<10; i++ ) {
	    int b = ihxData[ztexDescriptorOffs+30+i];
	    if ( b>=0 && b<=255 ) {
		snString[i] = (char) b;
	    }
	    else {
		throw new IncompatibleFirmwareException( "Invalid serial number string" );
	    }
	}

	// ensure word bounded upload data
	for ( int i=0; i+1<ihxData.length; i+=2 )
	    if ( ihxData[i]<0 && ihxData[i+1]>=0 )
		ihxData[i] = 0;
    }

    public ZtexIhxFile1( String fileName ) throws IOException, IhxFileDamagedException, IncompatibleFirmwareException {
	this( fileName, defaultZtexDescriptorOffs );
    }

// ******* productId ***********************************************************
    public final byte[] productId() {
	return productId;
    }

    public int productId( int i ) {
	return productId[i] & 255;
    }

// ******* fwVersion ***********************************************************
    public final int fwVersion() {
	return fwVersion & 255;
    }

// ******* interfaceVersion *****************************************************
    public final int interfaceVersion() {
	return interfaceVersion & 255;
    }

// ******* interfaceCapabilities ************************************************
    public final byte[] interfaceCapabilities() {
	return interfaceCapabilities;
    }

    public final int interfaceCapabilities( int i ) {
	return interfaceCapabilities[i] & 255;
    }

// ******* moduleReserved ******************************************************
    public final byte[] moduleReserved() {
	return moduleReserved;
    }

    public final int moduleReserved( int i ) {
	return moduleReserved[i] & 255;
    }
// ******* snString ************************************************************
    public final String snString() {
	return new String( snString );
    }

// ******* setSnString **********************************************************
    public final void setSnString( String s ) throws IncompatibleFirmwareException {
	if ( s.length()>10 ) 
	    throw new IncompatibleFirmwareException( "Serial number too long (max. 10 characters)" );
	
	int i=0;
	for (; i<s.length(); i++ ) {
	    ihxData[ztexDescriptorOffs+26+i] = (byte) snString[i];
	}
	for (; i<10; i++ ) {
	    ihxData[ztexDescriptorOffs+26+i] = 0;
	}
    }

// ******* toString ************************************************************
    public String toString () {
	return "productID=" + ZtexDevice1.byteArrayString(productId) + "  fwVer="+(fwVersion & 255) + "  ifVer="+(interfaceVersion & 255)+ "  snString=\"" + snString() + "\"";
    }

}    
