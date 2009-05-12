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
    Scan bus for devices with ZTEX descriptor 1 and/or Cypress EZ-USB FX2 devices
*/
package ztex;

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;

public class ZtexScanBus1 {
    private Vector<ZtexDevice1> devices = new Vector<ZtexDevice1>();

// ******* ZtexScanBus1 ********************************************************
    public ZtexScanBus1 (int usbVendorId, int usbProductId, boolean scanCypress, boolean quiet, int interfaceVersion, int productId0, int productId1, int productId2, int productId3 ) {
	LibusbJava.usb_find_busses();
	LibusbJava.usb_find_devices();

	Usb_Bus bus = LibusbJava.usb_get_busses();

	while ( bus != null ) {
	    Usb_Device dev = bus.getDevices();
	    while ( dev != null ) { 
		try {
		    try {
	    		ZtexDevice1 zdev = new ZtexDevice1( dev, usbVendorId, usbProductId );
			if ( ( scanCypress && zdev.isCypress() ) ||
			     ( zdev.valid() && (interfaceVersion<0 || zdev.interfaceVersion()==interfaceVersion) && zdev.compatible(productId0, productId1, productId2, productId3) ) ) {
			    devices.add( zdev );
			}
		    }
		    catch ( ZtexDescriptorException e ) {
			if ( scanCypress && usbVendorId == ZtexDevice1.cypressVendorId && usbProductId == ZtexDevice1.cypressProductId ) {
			    try {
	    			ZtexDevice1 zdev = new ZtexDevice1( dev, -1, -1 );
				if ( zdev.isCypress() ) devices.add( zdev );
			    }
			    catch ( ZtexDescriptorException e2 ) {
				if ( ! quiet )
				    System.err.println( e2.getLocalizedMessage() );		// should never occur
			    }
			}
			else {
			    if ( ! quiet )
				System.err.println( e.getLocalizedMessage() );
			} 
		    }
		}
		catch ( UsbException e ) {
		    if ( ! quiet )
			System.err.println( e.getLocalizedMessage() );
		}
		dev = dev.getNext();
	    }
	    bus = bus.getNext();
	}
    }

    public ZtexScanBus1 (int usbVendorId, int usbProductId, boolean scanCypress, boolean quiet, int interfaceVersion ) {
	this(usbVendorId, usbProductId, scanCypress, quiet, interfaceVersion, -1,-1,-1,-1 );
    }

    public ZtexScanBus1 (int usbVendorId, int usbProductId, boolean scanCypress, boolean quiet ) {
	this(usbVendorId, usbProductId, scanCypress, quiet, -1, -1,-1,-1,-1 );
    }

// ******* printBus ************************************************************
    public void printBus( PrintStream out ) {
	for (int i=0; i<devices.size(); i++ ) {
	    out.println( i + ": " + devices.elementAt(i).toString() );
	}
    }
    
// ******* numberOfDevices *****************************************************
    public final int numberOfDevices () {
	return devices.size();
    }

// ******* device **************************************************************
    public final ZtexDevice1 device (int i) throws IndexOutOfBoundsException {
	if ( i<0 || i>=devices.size() ) 
	    throw new IndexOutOfBoundsException( "Device number out of range. Valid numbers are 0.." + (devices.size()-1) ); 
	return devices.elementAt(i);
    }
}    

