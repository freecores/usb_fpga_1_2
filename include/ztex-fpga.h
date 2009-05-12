/*!
   ZTEX Firmware Kit for EZ-USB Microcontrollers
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
    FPGA configuration support
*/    

#ifndef[ZTEX_FPGA_H]
#define[ZTEX_FPGA_H]

#define[@CAPABILITY_FPGA;]

xdata BYTE fpga_checksum = 0;         // checksum
xdata DWORD fpga_bytes = 0;           // transfered bytes
xdata BYTE fpga_init_b = 0;           // init_b state (should be 222 after configuration)

/* *********************************************************************
   ***** resetFPGA *****************************************************
   ********************************************************************* */
static void resetFPGA () {		// reset FPGA
    unsigned short k;
    IFCONFIG = bmBIT7;
    SYNCDELAY; 
    PORTACFG = 0;
    PORTCCFG = 0;
    PORTECFG = 0;

    OEA = bmBIT1;
    IOA1 = 0;
    wait(10);
		    
    OEB = 0xff;				// setup IO's
    OEC = bmBIT2 | bmBIT3;
    IOC = bmBIT3;
    
    OED = bmBIT0;
    IOD0 = 0;				

    IOA1 = 1;				// ready for configuration
    IOD0 = 1;				
    k=0;
    while (!IOC0 && k<65535)
	k++;

    fpga_init_b = IOC0 ? 200 : 100;
    fpga_bytes = 0;
    fpga_checksum = 0;
    
    IOC = 0;
}    

/* *********************************************************************
   ***** initFPGAConfiguration *****************************************
   ********************************************************************* */
static void initFPGAConfiguration () {
    {
	PRE_FPGA_RESET
    }
    resetFPGA();			// reset FPGA
}    

/* *********************************************************************
   ***** finishFPGAConfiguration ***************************************
   ********************************************************************* */
static void finishFPGAConfiguration () {
    fpga_init_b += IOC0 ? 20 : 10;
    IOD0 = 0;  IOB = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1;
    IOD0 = 0;  IOD0 = 1; 
    IOD0 = 0;  IOD0 = 1; 
    IOD0 = 0;  IOD0 = 1; 
    IOD0 = 0;  IOD0 = 1; 
    OEB = 0;
//    OEC = 0;
    OED = 0;
    OEA = 0;
    fpga_init_b += IOC0 ? 2 : 1;
    if ( IOA1 )  {
	POST_FPGA_CONFIG
    }

    IOA1 = 1;		
    OEA |= bmBIT1;
}    


/* *********************************************************************
   ***** EP0 vendor request 0x30 ***************************************
   ********************************************************************* */
ADD_EP0_VENDOR_REQUEST((0x30,,		// get FPGA state
    MEM_COPY1(fpga_checksum,EP0BUF+1,6);    
    OEA &= ~bmBIT1;
    if ( IOA1 )  {
	EP0BUF[0] = 0; 	 		// FPGA configured 
	IOA1 = 1;		
	OEA |= bmBIT1;
    }
    else {
        EP0BUF[0] = 1;			// FPGA unconfigured 
	resetFPGA();			// prepare FPGA for configuration
    }
    
    EP0BCH = 0;
    EP0BCL = 7;
,,));;


/* *********************************************************************
   ***** EP0 vendor command 0x31 ***************************************
   ********************************************************************* */
ADD_EP0_VENDOR_COMMAND((0x31,,initFPGAConfiguration();,,));;	// reset FPGA


/* *********************************************************************
   ***** EP0 vendor command 0x32 ***************************************
   ********************************************************************* */
ADD_EP0_VENDOR_COMMAND((0x32,,		// send FPGA configuration data
    EP0BCL=1;
,,
    fpga_bytes += EP0BCL;
    _asm
	mov 	_AUTOPTRL1,#(_EP0BUF)
	mov 	_AUTOPTRH1,#(_EP0BUF >> 8)
	mov 	_AUTOPTRSETUP,#0x07
	mov	dptr,#_EP0BCL
	movx	a,@dptr
	jz 	010000$
  	mov	r2,a
	mov	dptr,#_fpga_checksum
	movx 	a,@dptr
	mov 	r1,a
	mov	dptr,#_XAUTODAT1
010001$:
	clr	_IOD0
	movx	a,@dptr
	mov	_IOB,a
	add 	a,r1
	mov 	r1,a
	setb	_IOD0
	djnz	r2, 010001$

	mov	dptr,#_fpga_checksum
	mov	a,r1
	movx	@dptr,a
	
010000$:
    	_endasm; 

    if ( EP0BCL<64 ) {
    	finishFPGAConfiguration();
    } 
));;
		

#endif  /*ZTEX_FPGA_H*/
