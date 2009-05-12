/*!
   Firmware / Bitstream loader for the ZTEX Firmware Kit
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
    Firmware Loader and FPGA Configurater
*/

import java.io.*;
import java.util.*;

import ch.ntb.usb.*;

import ztex.*;

class FWLoader {

// ******* main ****************************************************************
    public static void main (String args[]) {
	LibusbJava.usb_init();

	final String helpMsg = new String (
			"Global parameters:\n"+
			"    -c               Scan for Cypres EZ-USB devices without ZTEX firmware\n"+
			"    -v <VID> <PID>   Scan for devices with ZTEX firmware and the given Vendor ID and Product ID\n"+
			"    -vc              Equal to -v 0x4b4 0x8613\n"+
			"    -d <number>      Device Number (default: 0)\n"+
			"    -f               Force uploads\n"+
			"    -p               Print bus info\n"+
			"    -w                Enable certain workaraounds which may be required for vmware + windows\n"+
			"    -h               This help \n\n"+
			"Ordered parameters:\n"+
			"    -i               Info\n"+
			"    -ii              Info + capabilities\n"+
			"    -ru              Reset EZ-USB Microcontroller\n"+
			"    -uu <ihx file>   Upload EZ-USB Firmware\n"+
			"    -rf              Reset FPGA\n"+
			"    -uf <bitstream>  Upload <bitstream>\n"+
			"    -re              Reset EEPROM Firmware\n"+
			"    -ue <ihx file>   Upload Firmware to EEPROM" );
	
	
// process global parameters
	try {

	    int usbVendorId = -1;
	    int usbProductId = -1;
	    boolean cypress = false;
	    int devNum = 0;
	    boolean forceUpload = false;
	    boolean printBus = false;
	    boolean workarounds = false;
	    
	    for (int i=0; i<args.length; i++ ) {
		if ( args[i].equals("-c") ) {
		    cypress = true;
		}
		else if ( args[i].equals("-v") ) {
		    i++;
		    try {
			if (i>=args.length) 
			    throw new Exception();
    			usbVendorId = Integer.decode( args[i] );
		    } 
		    catch (Exception e) {
			System.err.println("Error: Vendor ID expected after -v");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		    i++;
		    try {
			if (i>=args.length) 
			    throw new Exception();
    			usbProductId = Integer.decode( args[i] );
		    } 
		    catch (Exception e) {
			System.err.println("Error: Product ID expected after -v <VID>");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		}
		else if ( args[i].equals("-vc") ) {
		    usbVendorId = 0x4b4;
		    usbProductId = 0x8613;
		}
		else if ( args[i].equals("-f") ) {
		    forceUpload = true;
		}
		else if ( args[i].equals("-p") ) {
		    printBus = true;
		}
		else if ( args[i].equals("-w") ) {
		    workarounds = true;
		}
		else if ( args[i].equals("-d") ) {
		    i++;
		    try {
			if (i>=args.length) 
			    throw new Exception();
    			devNum = Integer.parseInt( args[i] );
		    } 
		    catch (Exception e) {
			System.err.println("Error: Device number expected after -d");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		}
		else if ( args[i].equals("-h") ) {
		        System.err.println(helpMsg);
	    	        System.exit(0);
		}
		else if ( args[i].equals("-i") || args[i].equals("-ii") || args[i].equals("-ru") || args[i].equals("-rf") || args[i].equals("-re")) {
		}
		else if ( args[i].equals("-uu") || args[i].equals("-uf") || args[i].equals("-ue") ) {
		    i+=1;
		}
		else {
		    System.err.println("Error: Invalid Parameter: "+args[i]);
		    System.err.println(helpMsg);
		    System.exit(1);
		}
	    }
	    
// process ordered parameters
	    ZtexScanBus1 bus = new ZtexScanBus1( usbVendorId, usbProductId, cypress, false, 1);
	    if ( bus.numberOfDevices() <= 0 ) {
		System.err.println("No devices found");
		System.exit(0);
	    }
    	    if ( printBus )
		bus.printBus(System.out);

	    Ztex1v1 ztex = new Ztex1v1 ( bus.device(devNum) );
	    ztex.certainWorkarounds = workarounds;
	
	    for (int i=0; i<args.length; i++ ) {
		if ( args[i].equals("-i") ) {
		    System.out.println( ztex );
		} 
		if ( args[i].equals("-ii") ) {
		    System.out.println( ztex );
		    String str = ztex.capabilityInfo("      ");
		    if ( str.equals("") ) {
			System.out.println( "   No capabilities");
		    }	
		    else {
			System.out.println( "   Capabilities:\n"+str);
		    }
		} 
		else if ( args[i].equals("-ru") ) {
		    ztex.resetEzUsb();
		} 
		else if ( args[i].equals("-uu") ) {
		    i++;
    	    	    if ( i >= args.length ) {
			System.err.println("Error: Filename expected after -uu");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		    System.out.println("Firmware upload time: " + ztex.uploadFirmware( args[i], forceUpload ) + " ms");
		}
		else if ( args[i].equals("-re") ) {
		    ztex.eepromDisable();
		} 
		else if ( args[i].equals("-ue") ) {
		    i++;
    	    	    if ( i >= args.length ) {
			System.err.println("Error: Filename expected after -ue");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		    System.out.println("Firmware to EEPROM upload time: " + ztex.eepromUpload( args[i], forceUpload ) + " ms");
		}
		else if ( args[i].equals("-rf") ) {
		    ztex.resetFpga();
		}
		else if ( args[i].equals("-uf") ) {
		    i++;
    	    	    if ( i >= args.length ) {
			System.err.println("Error: Filename expected after -uf");
			System.err.println(helpMsg);
			System.exit(1);
		    }
		    System.out.println("FPGA configuration time: " + ztex.configureFpga( args[i], forceUpload ) + " ms");
		} 
	    } 
	} 
	catch (Exception e) {
	    System.out.println("Error: "+e.getLocalizedMessage() );
	}  
   } 
   
}
