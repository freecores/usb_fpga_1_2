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
    USB device with ZTEX descriptor 1 and/or Cypress EZ-USB FX2 device
*/
package ztex;

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;

public class ZtexDevice1 {
    public static final int cypressVendorId = 0x4b4;
    public static final int cypressProductId = 0x8613;

    private Usb_Device dev = null;
    private boolean isCypress = false; 		// true if Cypress device
    private boolean valid = false;		// true if descriptor 1 is available
    private int usbVendorId = -1;
    private int usbProductId = -1;
    private String manufacturerString = null;
    private String productString = null;
    private String snString = null;
    private byte productId[] = { 0,0,0,0 };	// product ID from the ZTEX descpriptor, not the USB product ID
    private byte fwVersion = 0;
    private byte interfaceVersion = 0;
    private byte interfaceCapabilities[] = { 0,0,0,0, 0,0 };
    private byte moduleReserved[] = { 0,0,0,0, 0,0,0,0, 0,0,0,0 };

// ******* byteArrayString *****************************************************
    public static String byteArrayString ( byte buf[] ) {
	String s = new String( "" );
	for ( int i = 0; i<buf.length; i++ ) {
	    if ( i != 0 ) 
		s+=".";
	    s+=buf[i] & 255;
	}
	return s;
    }
    
// ******* ZtexDevice1 *********************************************************
// Read the ZTEX descriptor if usbVendorId=pUsbVendorId and usbProductId=pUsbProductId
    public ZtexDevice1 (Usb_Device p_dev, int pUsbVendorId, int pUsbProductId) throws UsbException, ZtexDescriptorException {
	dev = p_dev;

	Usb_Device_Descriptor dd = dev.getDescriptor();
	usbVendorId = dd.getIdVendor() & 65535;
	usbProductId = dd.getIdProduct() & 65535;
	
	if ( usbVendorId == cypressVendorId  &&  usbProductId == cypressProductId ) {
	    isCypress = true;
	}
	
	int handle = LibusbJava.usb_open(dev);

	if ( handle > 0 ) {
	    if ( dd.getIManufacturer() > 0 ) 
	    	manufacturerString = LibusbJava.usb_get_string_simple( handle, dd.getIManufacturer() );
	    if ( dd.getIProduct() > 0 ) 
	    	productString = LibusbJava.usb_get_string_simple( handle, dd.getIProduct() );
	    if ( dd.getISerialNumber() > 0 )  
	    	snString = LibusbJava.usb_get_string_simple( handle, dd.getISerialNumber() );
		
	    if ( usbVendorId == pUsbVendorId  &&  usbProductId == pUsbProductId ) {
		if ( snString == null ) {
		    LibusbJava.usb_close(handle);
		    throw new ZtexDescriptorException( dev, "Not a ZTEX device" );  // ZTEX devices always have a SN. See also the next comment a few lines below
		} 
	    
	    	byte[] buf = new byte[42];
		int i = LibusbJava.usb_control_msg(handle, 0xc0, 0x22, 0, 0, buf, 40, 500);	// Failing of this may cause problems under windows. Therefore we check for the SN above.
		if ( i < 0 ) {
		    LibusbJava.usb_close(handle);
		    throw new ZtexDescriptorException( dev, "Error reading ZTEX descriptor: " + LibusbJava.usb_strerror() );
		}
		else if ( i != 40 ) {
		    LibusbJava.usb_close(handle);
		    throw new ZtexDescriptorException( dev, "Error reading ZTEX descriptor: Invalid size: " + i );
		}
		if ( buf[0]!=40 || buf[1]!=1 || buf[2]!='Z' || buf[3]!='T' || buf[4]!='E' || buf[5]!='X' ) {
		    LibusbJava.usb_close(handle);
		    throw new ZtexDescriptorException( dev, "Invalid ZTEX descriptor" );
		}
		productId[0] = buf[6];
		productId[1] = buf[7];
		productId[2] = buf[8];
		productId[3] = buf[9];
		fwVersion = buf[10];
		interfaceVersion = buf[11];
		interfaceCapabilities[0] = buf[12];
		interfaceCapabilities[1] = buf[13];
		interfaceCapabilities[2] = buf[14];
		interfaceCapabilities[3] = buf[15];
		interfaceCapabilities[4] = buf[16];
		interfaceCapabilities[5] = buf[17];
		moduleReserved[0] = buf[18];
		moduleReserved[1] = buf[19];
		moduleReserved[2] = buf[20];
		moduleReserved[3] = buf[21];
		moduleReserved[4] = buf[22];
		moduleReserved[5] = buf[23];
		moduleReserved[6] = buf[24];
		moduleReserved[7] = buf[25];
		moduleReserved[8] = buf[26];
		moduleReserved[9] = buf[27];
		moduleReserved[10] = buf[28];
		moduleReserved[11] = buf[29];
		
		valid = true;
	    }
	}
	else {
	    throw new UsbException( dev, "Error opening device: " + LibusbJava.usb_strerror() );
	}
	
        LibusbJava.usb_close(handle);
    }

// ******* toString ************************************************************
    public String toString () {
	return "bus=" + dev.getBus().getDirname() + "  device=" + dev.getFilename() + 
	    "\n   " + ( isCypress ? "Cypress" : "" ) +
	      ( manufacturerString == null ? "" : ("    Manufacturer=\""  + manufacturerString + "\"") ) +
	      ( productString == null ? "" : ("  Product=\""  + productString + "\"") ) +
	      ( snString == null ? "" : ("    SerialNumber=\""  + snString + "\"") ) +
	    ( valid ? "\n   productID=" + byteArrayString(productId) + "  fwVer="+(fwVersion & 255) + "  ifVer="+(interfaceVersion & 255)  : "" );
    }

// ******* compatible **********************************************************
    public final boolean compatible( int productId0, int productId1, int productId2, int productId3 ) {
	return ( productId[0]==0 || productId0<=0 || (productId[0] & 255) == productId0 ) &&
	       ( productId[1]==0 || productId1<=0 || (productId[1] & 255) == productId1 ) &&
	       ( productId[2]==0 || productId2<=0 || (productId[2] & 255) == productId2 ) &&
	       ( productId[3]==0 || productId3<=0 || (productId[3] & 255) == productId3 );
    }
    
// ******* dev *****************************************************************
    public final Usb_Device dev() {
	return dev;
    }

// ******* isCypress ***********************************************************
    public final boolean isCypress() {
	return isCypress;
    }
    
// ******* valid ***************************************************************
    public final boolean valid() {
	return valid;
    }

// ******* usbVendorId *********************************************************
    public final int usbVendorId() {
	return usbVendorId;
    }

// ******* usbProductId *********************************************************
    public final int usbProductId() {
	return usbProductId;
    }

// ******* manufacturerString **************************************************
    public final String manufacturerString() {
	return manufacturerString;
    }

// ******* productString *******************************************************
    public final String productString() {
	return productString;
    }

// ******* snString ************************************************************
    public final String snString() {
	return snString;
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

    public final boolean interfaceCapabilities( int i, int j ) {
	return 	(i>=0) && (i<=5) && (j>=0) && (j<8) &&
		(((interfaceCapabilities[i] & 255) & (1 << j)) != 0);
    }

// ******* moduleReserved ******************************************************
    public final byte[] moduleReserved() {
	return moduleReserved;
    }

    public final int moduleReserved( int i ) {
	return moduleReserved[i] & 255;
    }

}
