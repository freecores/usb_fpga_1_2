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
    Functions for USB devices with ZTEX descriptor 1
*/
package ztex;

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;

public class Ztex1 {
    protected int handle;
    protected ZtexDevice1 dev = null;
    private boolean oldDevices[] = new boolean[128];  
    private String usbBusName = null;

// ******* Ztex1 ***************************************************************
    public Ztex1 ( ZtexDevice1 pDev ) throws UsbException {
	dev = pDev;

	handle = LibusbJava.usb_open(dev.dev());
	if ( handle<=0 ) 
	    throw new UsbException(dev.dev(), "Error opening device");
    }

// ******* finalize ************************************************************
    protected void finalize () {
	LibusbJava.usb_close(handle);
    }

// ******* handle **************************************************************
    public final int handle() 
    {
        return handle;
    }

// ******* dev *****************************************************************
    public final ZtexDevice1 dev() 
    {
        return dev;
    }

// ******* valid ***************************************************************
    public boolean valid ( ) {
	return dev.valid();
    }

// ******* checkValid **********************************************************
    public void checkValid () throws InvalidFirmwareException {
	if ( ! dev.valid() ) 
	    throw new InvalidFirmwareException(this, "Can't read ZTEX descriptor 1");
    }

// ******* vendorCommand *******************************************************
    public int vendorCommand (int cmd, String func, int value, int index, byte[] buf, int length) throws UsbException {
	int i;
	if ( (i=LibusbJava.usb_control_msg(handle, 0x40, cmd, value, index, buf, length, 1000)) < 0 )
	    throw new UsbException( dev.dev(), (func != null ? func + ": " : "" ) + LibusbJava.usb_strerror());
	try {
    	    Thread.sleep(1);				// avoid package loss (due to interrupts ?)
	}
	    catch ( InterruptedException e ) {
	} 
	return i;
    }

    public int vendorCommand (int cmd, String func, int value, int index) throws UsbException {
	byte[] buf = { 0 };
	return vendorCommand (cmd, func, value, index, buf, 0);
    }

    public int vendorCommand (int cmd, String func) throws UsbException {
	byte[] buf = { 0 };
	return vendorCommand (cmd, func, 0, 0, buf, 0);
    }

// ******* vendorRequest *******************************************************
    public int vendorRequest (int cmd, String func, int value, int index, byte[] buf, int maxlen) throws UsbException {
	int i = LibusbJava.usb_control_msg(handle, 0xc0, cmd, value, index, buf, maxlen, 1000);
	if ( i < 0 )
	    throw new UsbException( dev.dev(), (func != null ? func + ": " : "" ) + LibusbJava.usb_strerror());
	try {
    	    Thread.sleep(1);				// avoid package loss (due to interrupts ?)
	}
	    catch ( InterruptedException e ) {
	} 
	return i;
    }

    public int vendorRequest (int cmd, String func, byte[] buf, int maxlen) throws UsbException {
	return vendorRequest (cmd, func, 0, 0, buf, maxlen);
    }

// ******* vendorCommand2 ******************************************************
    public void vendorCommand2 (int cmd, String func, int value, int index, byte[] buf, int length) throws UsbException {
	int i = vendorCommand (cmd, func, value, index, buf, length);
	if ( i != length )
	    throw new UsbException( dev.dev(), (func != null ? func + ": " : "" ) + "Send " + i + " byte of data instead of " + length + " bytes");
    }

    public void vendorCommand2 (int cmd, String func, int value, int index) throws UsbException {
	byte[] buf = { 0 };
	vendorCommand2 (cmd, func, value, index, buf, 0);
    }

    public void vendorCommand2 (int cmd, String func) throws UsbException {
	byte[] buf = { 0 };
	vendorCommand2 (cmd, func, 0, 0, buf, 0);
    }

// ******* vendorRequest2 ******************************************************
    public void vendorRequest2 (int cmd, String func, int value, int index, byte[] buf, int maxlen) throws UsbException {
	int i = vendorRequest(cmd, func, value, index, buf, maxlen);
	if ( i != maxlen )
	    throw new UsbException( dev.dev(), (func != null ? func + ": " : "" ) + "Received " + i + " byte of data, expected "+maxlen+" bytes");
    }

    public void vendorRequest2 (int cmd, String func, byte[] buf, int maxlen) throws UsbException {
	vendorRequest2(cmd, func, 0, 0, buf, maxlen);
    }

// ******* findOldDevices ******************************************************
    private void findOldDevices () {
	Usb_Bus bus = dev.dev().getBus();
	usbBusName = bus.getDirname();

	for ( int i=0; i<=127; i++ ) 
	    oldDevices[i] = false;
	
	Usb_Device d = bus.getDevices();
	while ( d != null ) { 
	    byte b = d.getDevnum();
	    if ( b > 0 ) 
		oldDevices[b] = true;
	    d = d.getNext();
	}
	oldDevices[dev.dev().getDevnum()] = false;
    }

// ******* findNewDevice *******************************************************
    private Usb_Device findNewDevice ( String errMsg ) throws DeviceLostException {
	Usb_Device newDev = null;
	LibusbJava.usb_find_busses();
	LibusbJava.usb_find_devices();
	
	Usb_Bus bus = LibusbJava.usb_get_busses();
	while ( bus != null && ! bus.getDirname().equals(usbBusName) ) 
	    bus = bus.getNext();
	
	Usb_Device d = bus != null ? bus.getDevices() : null;
	while ( d != null ) { 
	    byte b = d.getDevnum();
	    if ( b > 0 && ! oldDevices[b] ) {
		if ( newDev != null )
		    throw new DeviceLostException( errMsg + "More than 2 new devices found" );
		newDev = d;
	    }
	    d = d.getNext();
	}
	
	return newDev;
    }

// ******* initNewDevice *******************************************************
    private void initNewDevice ( String errBase ) throws DeviceLostException, UsbException, ZtexDescriptorException {
// scan the bus for up to 5 s for a new device
	Usb_Device newDev = null;
	for ( int i=0; i<20 && newDev==null; i++ ) {
	    try {
    		Thread.sleep( 250 );
	    }
		catch ( InterruptedException e ) {
	    }
	    newDev = findNewDevice( errBase + ": " );
	}
	if ( newDev == null )  
	    throw new DeviceLostException( errBase + ": No new device found" );

// init new device
	Usb_Device_Descriptor dd = newDev.getDescriptor();
	int vid = dd.getIdVendor() & 65535;
	int pid = dd.getIdProduct() & 65535;
	try {
	    dev = new ZtexDevice1( newDev, vid, pid );
	}
	catch ( ZtexDescriptorException e ) {
	    if ( vid == ZtexDevice1.cypressVendorId && pid == ZtexDevice1.cypressProductId ) {
		dev = new ZtexDevice1( newDev, -1, -1 );
	    }
	    else {
		throw e;
	    }
	}
	handle = LibusbJava.usb_open( dev.dev() );
    }

// ******* uploadFirmware ******************************************************
//  returns upload time in ms
    public long uploadFirmware ( String ihxFileName, boolean force ) throws IncompatibleFirmwareException, FirmwareUploadException, UsbException, ZtexDescriptorException, DeviceLostException {
// load the ihx file
	ZtexIhxFile1 ihxFile;
	try {
	    ihxFile = new ZtexIhxFile1( ihxFileName );
	}
	catch ( IOException e ) {
	    throw new FirmwareUploadException( e.getLocalizedMessage() );
	}
	catch ( IhxFileDamagedException e ) {
	    throw new FirmwareUploadException( e.getLocalizedMessage() );
	}
//	ihxFile.dataInfo(System.out);
//	System.out.println(ihxFile);

// check for compatibility
	if ( ! force && dev.valid() ) {
	    if ( ihxFile.interfaceVersion() != 1 )
		throw new IncompatibleFirmwareException("Wrong interface version: Expected 1, got " + ihxFile.interfaceVersion() );
	
	    if ( ! dev.compatible ( ihxFile.productId(0), ihxFile.productId(1), ihxFile.productId(2), ihxFile.productId(3) ) )
		throw new IncompatibleFirmwareException("Incompatible productId's: Current firmware: " + ZtexDevice1.byteArrayString(dev.productId()) 
		    + "  Ihx File: " + ZtexDevice1.byteArrayString(ihxFile.productId()) );
	}

// scan the bus for comparison
	findOldDevices();
	
// upload the firmware
	long time = EzUsb.uploadFirmware( handle, ihxFile );
	
// find and init new device
	initNewDevice("Device lost after uploading Firmware");
	
	return time;
    }

// ******* resetEzUsb **********************************************************
    public void resetEzUsb () throws IncompatibleFirmwareException, FirmwareUploadException, UsbException, ZtexDescriptorException, DeviceLostException {
// scan the bus for comparison
	findOldDevices();
	
// reset the EZ-USB
	EzUsb.reset(handle,true);
	try {
	    EzUsb.reset(handle,false);		// error (may caused re-numeration) can be ignored
	}
	catch ( FirmwareUploadException e ) {
	}
	
// find and init new device
	initNewDevice( "Device lost after resetting the EZ-USB" );
    }

// ******* toString ************************************************************
    public String toString () {
	return dev.toString();
    }

}    
