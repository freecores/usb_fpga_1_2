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
    Functions for USB devices with ZTEX descriptor 1, Interface 1
    Interface capabilities and vendor requests (VR) / commands (VC):
    0.0  : EEPROM support
    0.1  : FPGA Configuration
	VR 0x31 : FPGA state
	    Returns:
	    Offs	Description
	    0		1: unconfigured, 0:configured
	    1		checksum
	    2-5		transfered bytes
	    6  		INIT_B state
	
*/
package ztex;

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;


public class Ztex1v1 extends Ztex1 {
    public static final String capabilityStrings[] = {
	"EEPROM read/write" ,
	"FPGA configuration"
    };
    
    public boolean certainWorkarounds = false;  // setting to true will enable certain workarounds which may be required for vmware + windows

    private boolean fpgaConfigured = false;
    private int fpgaChecksum = 0;
    private int fpgaBytes = 0;
    private int fpgaInitB = 0;
    
    public int eepromBytes = 0;
    public int eepromChecksum = 0;
    public boolean eepromReady = true;

// ******* Ztex1v1 *************************************************************
    public Ztex1v1 ( ZtexDevice1 pDev ) throws UsbException, ZtexDescriptorException {
	super ( pDev );
	certainWorkarounds = false;
    }

// ******* valid ***************************************************************
    public boolean valid ( ) {
	return dev.valid() && dev.interfaceVersion()==1;
    }

    public boolean valid ( int i, int j) {
	return dev.valid() && dev.interfaceVersion()==1 && dev.interfaceCapabilities(i,j);
    }

// ******* compatible **********************************************************
    public boolean compatible ( int productId0, int productId1, int productId2, int productId3 ) {
	return dev.valid() && dev.compatible ( productId0, productId1, productId2, productId3 ) && dev.interfaceVersion()==1;
    }

// ******* checkValid **********************************************************
    public void checkValid () throws InvalidFirmwareException {
	super.checkValid();
	if ( dev.interfaceVersion() != 1 )
	    throw new InvalidFirmwareException(this, "Wrong interface: " + dev.interfaceVersion() + ", expected: 1" );
    }

// ******* checkCapability *****************************************************
    public void checkCapability ( int i, int j ) throws InvalidFirmwareException, CapabilityException {
	checkValid();
	if ( ! dev.interfaceCapabilities(i,j) ) {
	    int k = i*8 + j;
	    if ( k>=0 && k<capabilityStrings.length )
	    throw new CapabilityException( this, ( k>=0 && k<=capabilityStrings.length ) ? capabilityStrings[k] : ("Capabilty " + i + "," + j) ); 
	}
    }

// ******* checkCompatible *****************************************************
    public void checkCompatible ( int productId0, int productId1, int productId2, int productId3 ) throws InvalidFirmwareException {
	checkValid();
	if ( ! dev.compatible ( productId0, productId1, productId2, productId3 ) )
	    throw new InvalidFirmwareException(this, "Incompatible Product ID");
    }

// ******* getFpgaState ********************************************************
    private void getFpgaState () throws UsbException, InvalidFirmwareException, CapabilityException {
	byte[] buffer = new byte[7];
	checkCapability(0,1);
	vendorRequest2(0x30, "getFpgaState", buffer, 7);
	fpgaConfigured = buffer[0] == 0;
	fpgaChecksum = buffer[1] & 0xff;
	fpgaBytes = ((buffer[5] & 0xff)<<24) | ((buffer[4] & 0xff)<<16) | ((buffer[3] & 0xff)<<8) | (buffer[2] & 0xff);
	fpgaInitB = buffer[6] & 0xff;
    }

// ******* getFpgaConfiguration ************************************************
    public boolean getFpgaConfiguration () throws UsbException, InvalidFirmwareException, CapabilityException {
	getFpgaState ();
	return fpgaConfigured;
    }

// ******* getFpgaConfigurationStr *********************************************
    public String getFpgaConfigurationStr () throws UsbException, InvalidFirmwareException, CapabilityException {
	getFpgaState ();
	return fpgaConfigured ? "FPGA configured" : "FPGA unconfigured";
    }

// ******* resetFGPA ***********************************************************
    public void resetFpga () throws UsbException, InvalidFirmwareException, CapabilityException {
	checkCapability(0,1);
	vendorCommand(0x31, "resetFpga" );
    }

// ******* configureFpga *******************************************************
//  returns configuration time in ms
    public long configureFpga ( String fwFileName, boolean force ) throws BitstreamReadException, UsbException, BitstreamUploadException, AlreadyConfiguredException, InvalidFirmwareException, CapabilityException {
	final int transactionBytes = certainWorkarounds ? 256 : 2048;
	long t0 = 0;

	checkCapability(0,1);
	
	if ( !force && getFpgaConfiguration() )
	    throw new AlreadyConfiguredException(); 

// read the bitstream file	
        byte[][] buffer = new byte[4*1024*1024/transactionBytes][];
	int size = 0;
	try {
	    InputStream inputStream = JInputStream.getInputStream( fwFileName );
	    int j = transactionBytes;
	    for ( int i=0; i<buffer.length && j==transactionBytes; i++ ) {
		buffer[i] = new byte[transactionBytes]; 
		j = inputStream.read( buffer[i] );
		if ( j < 0 ) 
		    j = 0;
		if ( j < transactionBytes && j % 64 == 0 )	// ensures size % 64 != 0
		    j+=1;
		size += j;
	    }
	    
	    try {
		inputStream.close();
	    }
	    catch ( Exception e ) {
		System.err.println( "Warning: Error closing file " + fwFileName + ": " + e.getLocalizedMessage() );
	    }
	}
	catch (IOException e) {
	    throw new BitstreamReadException(e.getLocalizedMessage());
	}
	if ( size < 64 || size % 64 == 0 ) 
	    throw new BitstreamReadException("Invalid file size: " + size );
	    
// upload the bitstream file	
	for ( int tries=10; tries>0; tries-- ) {
	    
	    resetFpga();

	    try {
		t0 = -new Date().getTime();
		int cs = 0;
		int bs = 0;
		    
	    	for ( int i=0; i<buffer.length && i*transactionBytes < size; i++ ) {
		    int j = size-i*transactionBytes;
		    if (j>transactionBytes) 
			j = transactionBytes;
		    vendorCommand2(0x32, "sendFpgaData", 0,0, buffer[i], j);

		    bs+=j;
		    for ( int k=0; k<buffer[i].length; k++ ) 
		        cs = ( cs + (buffer[i][k] & 0xff) ) & 0xff;
		}

		getFpgaState();
//		System.err.println("fpgaConfigred=" + fpgaConfigured + "   fpgaBytes="+fpgaBytes + " ("+bs+")   fpgaChecksum="+fpgaChecksum + " ("+cs+")   fpgaInitB="+fpgaInitB );
		if ( ! fpgaConfigured ) {
		    throw new BitstreamUploadException( "FPGA configuration failed: DONE pin does not go high (size=" + fpgaBytes + " ,  " + (bs - fpgaBytes) + " bytes went lost;  checksum=" 
			+ fpgaChecksum + " , should be " + cs + ";  INIT_B_HIST=" + fpgaInitB +", should be 222)" );
		}
		if ( fpgaInitB != 222 )
		    System.err.println ( "Warning: FPGA configuration may have failed: DONE pin has gone high but INIT_B states are wrong: " + fpgaInitB +", should be 222");
			
		tries = 0;
		t0 += new Date().getTime();

	    } 
	    catch ( BitstreamUploadException e ) {
		if ( tries>1 ) 
		    System.err.println("Warning: " + e.getLocalizedMessage() +": Retrying it ...");
		else 
		    throw e;
	    }
	}

    	try {
    	    Thread.sleep( 200 );
    	}
	catch ( InterruptedException e) {
        } 
	
	return t0;
    } 

// ******* eepromState *********************************************************
// returns true if EEPROM is ready
    public boolean eepromState ( ) throws UsbException, InvalidFirmwareException, CapabilityException {
	byte[] buf = new byte[4];
	checkCapability(0,0);
	vendorRequest2(0x3A, "EEPROM State", 0, 0, buf, 4);
	eepromBytes = (buf[0] & 255) | (buf[1] & 255)<<8;
	eepromChecksum = buf[2] & 255;
	eepromReady = buf[3] == 0;
	return eepromReady;
    }

// ******* eepromWrite *********************************************************
    public void eepromWrite ( int addr, byte[] buf, int length ) throws UsbException, InvalidFirmwareException, CapabilityException {
	checkCapability(0,0);
	vendorCommand2( 0x39, "EEPROM Write", addr, 0, buf, length );
    }

// ******* eepromRead **********************************************************
    public void eepromRead ( int addr, byte[] buf, int length ) throws UsbException, InvalidFirmwareException, CapabilityException {
	checkCapability(0,0);
        for ( int tries=4; tries>0; tries-- ) {
	    try {
	        vendorRequest2( 0x38, "EEPROM Read", addr, 0, buf, length );		// sometimes a little bit slow
		tries = 0;
	    }
	    catch ( UsbException e ) {
		if ( tries<=1 ) throw e;
	    }
	}
    }

// ******* eepromUpload ********************************************************
//  returns upload time in ms
    public long eepromUpload ( String ihxFileName, boolean force ) throws IncompatibleFirmwareException, FirmwareUploadException, InvalidFirmwareException, CapabilityException {
	final int pagesMax = 256;
	final int pageSize = 256;
	int pages = 0;
	byte[][] buffer = new byte[pagesMax][];

	checkCapability(0,0);
	
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

	Usb_Device_Descriptor dd = dev.dev().getDescriptor();
	int vid = dd.getIdVendor() & 65535;
	int pid = dd.getIdProduct() & 65535;

	buffer[0] = new byte[pageSize];
	buffer[0][0] = (byte) 0xc2;
	buffer[0][1] = (byte) (vid & 255);
	buffer[0][2] = (byte) ((vid >> 8) & 255);
	buffer[0][3] = (byte) (pid & 255);
	buffer[0][4] = (byte) ((pid >> 8) & 255);
	buffer[0][5] = 0;
	buffer[0][6] = 0;
	buffer[0][7] = 0;
	
	int ptr = 8, i = 0;
	
	while ( i < ihxFile.ihxData.length ) {
	    if ( ihxFile.ihxData[i]>=0 && ihxFile.ihxData[i]<256 ) {			// new data block
		int j = 1;
		while ( i+j<ihxFile.ihxData.length && ihxFile.ihxData[i+j]>=0 && ihxFile.ihxData[i+j]<255 ) 
		    j++;

		for (int k=ptr/pageSize + 1; k < (ptr+j+4)/pageSize + 1; k++ )	// also considers 5 bytes for the last data block
		    buffer[k] = new byte[pageSize];

		buffer[(ptr+0)/pageSize][(ptr+0) % pageSize] = (byte) ((j >> 8) & 255);	
		buffer[(ptr+1)/pageSize][(ptr+1) % pageSize] = (byte) (j & 255);		// length
		buffer[(ptr+2)/pageSize][(ptr+2) % pageSize] = (byte) ((i >> 8) & 255);
		buffer[(ptr+3)/pageSize][(ptr+3) % pageSize] = (byte) (i & 255);		// address
		ptr+=4;
		for ( int k=0; k<j; k++ )  					// data
		    buffer[(ptr+k)/pageSize][(ptr+k) % pageSize] = (byte) ihxFile.ihxData[i+k];
		ptr+=j;
		i+=j;
	    }
	    else {
		i+=1;
	    }
	}
	
	buffer[(ptr+0)/pageSize][(ptr+0) % pageSize] = (byte) 0x80;		// last data block
	buffer[(ptr+1)/pageSize][(ptr+1) % pageSize] = (byte) 0x01;
	buffer[(ptr+2)/pageSize][(ptr+2) % pageSize] = (byte) 0xe6;
	buffer[(ptr+3)/pageSize][(ptr+3) % pageSize] = (byte) 0x00;
	buffer[(ptr+3)/pageSize][(ptr+4) % pageSize] = (byte) 0x00;
	ptr+=5;


	long t0 = new Date().getTime();
	byte[] rbuf = new byte[pageSize];

	for ( i=(ptr-1)/pageSize; i>=0; i-- ) {
	    
	    int k = (i+1)*pageSize < ptr ? pageSize : ptr-i*pageSize;
	    int cs = 0;
	    for (int j=0; j<k; j++ ) {
		cs = ( cs + (buffer[i][j] & 255) ) & 255;
	    }

	    for ( int tries=4; tries>0; tries-- ) {
	    	try {
		    eepromWrite(i*pageSize, buffer[i], k);
		    eepromState();
		    if ( eepromBytes!=k )
			throw new FirmwareUploadException("Error writing data to EEPROM: Wrote " + eepromBytes + " bytes instead of "  + k + " bytes" );
		    if ( eepromChecksum!=cs )
			throw new FirmwareUploadException("Error writing data to EEPROM: Checksum error");

    		    eepromRead(i*pageSize, rbuf, k);
		    for (int j=0; j<k; j++ ) {
			if ( rbuf[j] != buffer[i][j] )
			    throw new FirmwareUploadException("Error writing data to EEPROM: Verification failed");
		    }
		    tries = 0;
		}
		catch ( Exception e ) {
		    if ( tries > 1 ) {
			System.err.println("Warning: " + e.getLocalizedMessage() +": Retrying it ...");
		    }
		    else {
			throw new FirmwareUploadException(e.getLocalizedMessage());
		    }
		}
	    } 
	}
	
	return new Date().getTime() - t0;
    }
    

// ******* eepromDisable ********************************************************
    public void eepromDisable ( ) throws FirmwareUploadException, InvalidFirmwareException, CapabilityException {
	byte[] buf = { 0 };

	for ( int tries=4; tries>0; tries-- ) {
	    try {
	        eepromWrite(0, buf, 1);

    		eepromRead(0, buf, 1);
		if ( buf[0] != 0 )
		    throw new FirmwareUploadException("Error disabeling EEPROM firmware: Verification failed");
		tries = 0;

    	    }
	    catch ( Exception e ) {
	        if ( tries > 1 ) {
	    	    System.err.println("Warning: " + e.getLocalizedMessage() +": Retrying it ...");
		}
		else {
		    throw new FirmwareUploadException(e.getLocalizedMessage());
		}
	    } 
	}
    } 
    
// ******* toString ************************************************************
    public String toString () {
	String str = dev.toString();
	try {
	    str += "\n   " + getFpgaConfigurationStr();
	}
	catch ( Exception e ) {
	}
	return str;
    }

// ******* capabilityInfo ******************************************************
    public String capabilityInfo ( String pf ) {
	String str = "";
	for ( int i=0; i<6; i++ ) 
	    for (int j=0; j<8; j++ ) 
		if ( dev.interfaceCapabilities(i,j) ) {
		    if ( ! str.equals("") ) 
			str+="\n";
		    if (i*8+j < capabilityStrings.length) 
			str+=pf+capabilityStrings[i*8+j];
		    else
			str+=pf+i+"."+j;
		}
	return str;
    }
}    

