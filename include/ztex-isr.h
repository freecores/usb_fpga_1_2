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
   Interrupt routines
*/

#ifndef[ZTEX_ISR_H]
#define[ZTEX_ISR_H]

xdata BYTE prevSetupRequest = 0xff;

xdata WORD ISOFRAME_COUNTER[4] = {0, 0, 0, 0}; 	// counters for iso frames automatically reset by sync frame request

/* *********************************************************************
   ***** toggleData ****************************************************
   ********************************************************************* */
static void resetToggleData () {
#define[RESET_TOGGLE_DATA_EP(][);][
#ifeq[EP$0_DIR][OUT]
    TOGCTL = $0;				// EP$0 out
    TOGCTL = $0 | bmBIT5;
#endif    
#ifeq[EP$0_DIR][IN]
    TOGCTL = bmBIT4 | $0;			// EP$0 in
    TOGCTL = bmBIT4 | $0 | bmBIT5;
#endif]

    TOGCTL = 0;				// EP0 out
    TOGCTL = 0 | bmBIT5;
    TOGCTL = 0x10;			// EP0 in
    TOGCTL = 0x10 | bmBIT5;
#ifeq[EP1OUT_DIR][OUT]
    TOGCTL = 1;				// EP1 out
    TOGCTL = 1 | bmBIT5;
#endif    
#ifeq[EP1IN_DIR][IN]
    TOGCTL = 0x11;			// EP1 in
    TOGCTL = 0x11 | bmBIT5;
#endif    
    RESET_TOGGLE_DATA_EP(2);
    RESET_TOGGLE_DATA_EP(4);
    RESET_TOGGLE_DATA_EP(6);
    RESET_TOGGLE_DATA_EP(8);
}

/* *********************************************************************
   ***** getStringDescriptor *******************************************
   ********************************************************************* */
#define[SEND_STRING_DESCRIPTOR(][,$1);][sendStringDescriptor(LSB($1), MSB($1), sizeof($1) );]

static void sendStringDescriptor (BYTE loAddr, BYTE hiAddr, BYTE size)
{
    BYTE i;
    if ( size > 31)
	size = 31;
    AUTOPTRL1 = loAddr;
    AUTOPTRH1 = hiAddr;
    AUTOPTRL2 = (BYTE)(((unsigned short)(&EP0BUF))+1);
    AUTOPTRH2 = (BYTE)((((unsigned short)(&EP0BUF))+1) >> 8);
    XAUTODAT2 = 3;
    AUTOPTRSETUP = 7;
    for (i=0; i<size; i++) {
	XAUTODAT2 = XAUTODAT1;
	XAUTODAT2 = 0;
    }
    i = (size+1) << 1;
    EP0BUF[0] = i;
    EP0BUF[1] = 3;
    EP0BCH = 0;
    EP0BCL = i;
}


/* *********************************************************************
   ***** SUDAV_ISR *****************************************************
   ********************************************************************* */
static void SUDAV_ISR () interrupt
{
    BYTE a;
    prevSetupRequest = bRequest;
    SUDPTRCTL = 1;
    // standard USB requests
    switch ( bRequest ) {
	case 0x00:	// get status 
    	    switch(SETUPDAT[0]) {
		case 0x80:  		// self powered and remote 
		    EP0BUF[0] = 0;	// not self-powered, no remote wakeup
		    EP0BUF[1] = 0;
		    EP0BCH = 0;
		    EP0BCL = 2;
		    break;
		case 0x81:		// interface (reserved)
		    EP0BUF[0] = 0; 	// Always return zeros
		    EP0BUF[1] = 0;
		    EP0BCH = 0;
		    EP0BCL = 2;
		    break;
		case 0x82:	
		    switch ( SETUPDAT[4] ) {
			case 0x00 :
			case 0x80 :
			    EP0BUF[0] = EP0CS & bmBIT0;
			    break;
			case 0x01 :
			    EP0BUF[0] = EP1OUTCS & bmBIT0;
			    break;
			case 0x81 :
			    EP0BUF[0] = EP1INCS & bmBIT0;
			    break;
			default:
			    EP0BUF[0] = EPXCS[ ((SETUPDAT[4] >> 1)-1) & 3 ] & bmBIT0;
			    break;
			}
		    EP0BUF[1] = 0;
		    EP0BCH = 0;
		    EP0BCL = 2;
		    break;
	    }
	    break;
	case 0x01:	// disable feature, e.g. remote wake, stall bit
	    if ( SETUPDAT[0] == 2 && SETUPDAT[2] == 0 ) {
		switch ( SETUPDAT[4] ) {
		    case 0x00 :
		    case 0x80 :
			EP0CS &= ~bmBIT0;
			break;
		    case 0x01 :
			EP1OUTCS &= ~bmBIT0;
			break;
		    case 0x81 :
		         EP1INCS &= ~bmBIT0;
			break;
		    default:
			 EPXCS[ ((SETUPDAT[4] >> 1)-1) & 3 ] &= ~bmBIT0;
			break;
		} 
	    }
	    break;
	case 0x03:      // enable feature, e.g. remote wake, test mode, stall bit
	    if ( SETUPDAT[0] == 2 && SETUPDAT[2] == 0 ) {
		switch ( SETUPDAT[4] ) {
		    case 0x00 :
		    case 0x80 :
			EP0CS |= bmBIT0;
			break;
		    case 0x01 :
			EP1OUTCS |= bmBIT0;
			break;
		    case 0x81 :
		         EP1INCS |= bmBIT0;
			break;
		    default:
			 EPXCS[ ((SETUPDAT[4] >> 1)-1) & 3 ] |= ~bmBIT0;
			break;
		}
		a = ( (SETUPDAT[4] & 0x80) >> 3 ) | (SETUPDAT[4] & 0x0f);
		TOGCTL = a;
		TOGCTL = a | bmBIT5;
	    } 
	    break;
	case 0x06:			// get descriptor
	    switch(SETUPDAT[3]) {
		case 0x01:		// device
		    SUDPTRH = MSB(&DeviceDescriptor);
		    SUDPTRL = LSB(&DeviceDescriptor);
		    break;
		case 0x02: 		// configuration
		    if (USBCS & bmBIT7) {
    	    	        SUDPTRH = MSB(&HighSpeedConfigDescriptor);
			SUDPTRL = LSB(&HighSpeedConfigDescriptor);
		    }
		    else {
    	    	        SUDPTRH = MSB(&FullSpeedConfigDescriptor);
			SUDPTRL = LSB(&FullSpeedConfigDescriptor);
		    }
		    break; 
		case 0x03:		// strings
		    switch (SETUPDAT[2]) {
			case 1:
			    SEND_STRING_DESCRIPTOR(1,manufacturerString);
			    break;
			case 2:
			    SEND_STRING_DESCRIPTOR(2,productString);
			    break;
			case 3:
			    SEND_STRING_DESCRIPTOR(3,SN_STRING);
			    break;
			case 4:
			    SEND_STRING_DESCRIPTOR(4,configurationString);
			    break; 
			default:
			    SUDPTRH = MSB(&EmptyStringDescriptor);
			    SUDPTRL = LSB(&EmptyStringDescriptor);
			    break;
			}	
		    break;
		case 0x06:		// device qualifier
		    SUDPTRH = MSB(&DeviceQualifierDescriptor);
		    SUDPTRL = LSB(&DeviceQualifierDescriptor);
		    break;
		case 0x07: 		// other speed configuration
		    if (USBCS & bmBIT7) {
    	    	        SUDPTRH = MSB(&FullSpeedConfigDescriptor);
			SUDPTRL = LSB(&FullSpeedConfigDescriptor);
		    }
		    else {
    	    	        SUDPTRH = MSB(&HighSpeedConfigDescriptor);
			SUDPTRL = LSB(&HighSpeedConfigDescriptor);
		    }
		    break; 
		default:
		    EP0CS |= 0x01;	// set stall, unknown descriptor
	    }
	    break;
	case 0x07:			// set descriptor
	    break;			
	case 0x08:			// get configuration
	    EP0BUF[0] = 0;		// only one configuration
	    EP0BCH = 0;
	    EP0BCL = 1;
	    break;
	case 0x09:			// set configuration
	    resetToggleData();
	    break;			// do nothing since we have only one configuration
	case 0x0a:			// get alternate setting for an interface
	    EP0BUF[0] = 0;		// only one alternate setting
	    EP0BCH = 0;
	    EP0BCL = 1;
	    break;
	case 0x0b:			// set alternate setting for an interface
	    resetToggleData();
	    break;			// do nothing since we have only on alternate setting
	case 0x0c:			// sync frame
	    if ( SETUPDAT[0] == 0x82 ) {
		ISOFRAME_COUNTER[ ((SETUPDAT[4] >> 1)-1) & 3 ] = 0;
		EP0BUF[0] = USBFRAMEL;	// use current frame as sync frame, i hope that works
		EP0BUF[1] = USBFRAMEH;	
		EP0BCH = 0;
    		EP0BCL = 2;
	    }
	    break;			// do nothing since we have only on alternate setting
	    
    }

    // vendor request and commands
    switch ( bmRequestType ) {
	case 0xc0: 					// vendor request 
	    switch ( bRequest ) {
		case 0x22: 				// get ZTEX descriptor
		    SUDPTRCTL = 0;
		    EP0BCH = 0;
		    EP0BCL = ZTEX_DESCRIPTOR_LEN;
		    SUDPTRH = MSB(ZTEX_DESCRIPTOR_OFFS);
		    SUDPTRL = LSB(ZTEX_DESCRIPTOR_OFFS); 
		    break;
		EP0_VENDOR_REQUESTS_SU;
		default:
		    EP0CS |= 0x01;			// set stall, unknown request
	    }
	    break;
	case 0x40: 					// vendor command
	    switch ( bRequest ) {
		EP0_VENDOR_COMMANDS_SU;
		default:
		    EP0CS |= 0x01;			// set stall, unknown request
	    }
	    break;
    }

    EP0CS |= 0x80;
    EXIF &= ~bmBIT4;
    USBIRQ = bmBIT0;
}

/* *********************************************************************
   ***** SOF_ISR ******************************************************* 
   ********************************************************************* */
void SOF_ISR() interrupt
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT1;
}

/* *********************************************************************
   ***** SUTOK_ISR ***************************************************** 
   ********************************************************************* */
void SUTOK_ISR() interrupt 
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT2;
}

/* *********************************************************************
   ***** SUSP_ISR ****************************************************** 
   ********************************************************************* */
void SUSP_ISR() interrupt
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT3;
}

/* *********************************************************************
   ***** URES_ISR ****************************************************** 
   ********************************************************************* */
void URES_ISR() interrupt
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT4;
}

/* *********************************************************************
   ***** HSGRANT_ISR *************************************************** 
   ********************************************************************* */
void HSGRANT_ISR() interrupt
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT5;
}        

/* *********************************************************************
   ***** AP0ACK_ISR **************************************************** 
   ********************************************************************* */
void EP0ACK_ISR() interrupt
{
        EXIF &= ~bmBIT4;
	USBIRQ = bmBIT6;
}

/* *********************************************************************
   ***** EP0IN_ISR *****************************************************
   ********************************************************************* */
static void EP0IN_ISR () interrupt
{
    switch ( prevSetupRequest ) {
	EP0_VENDOR_REQUESTS_DAT;
	default:
	    EP0BCH = 0;
	    EP0BCL = 0;
    }
    EP0CS |= 0x80;
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT0;
}

/* *********************************************************************
   ***** EP0OUT_ISR ****************************************************
   ********************************************************************* */
static void EP0OUT_ISR () interrupt
{
    switch ( prevSetupRequest ) {
	EP0_VENDOR_COMMANDS_DAT;
    }

    EP0BCL = 1;

    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT1;
}


/* *********************************************************************
   ***** EP1IN_ISR *****************************************************
   ********************************************************************* */
void EP1IN_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT2;

}

/* *********************************************************************
   ***** EP1OUT_ISR ****************************************************
   ********************************************************************* */
void EP1OUT_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT3;
}

/* *********************************************************************
   ***** EP2_ISR *******************************************************
   ********************************************************************* */
void EP2_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT4;
}

/* *********************************************************************
   ***** EP4_ISR *******************************************************
   ********************************************************************* */
void EP4_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT5;
}

/* *********************************************************************
   ***** EP6_ISR *******************************************************
   ********************************************************************* */
void EP6_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT6;
}

/* *********************************************************************
   ***** EP8_ISR *******************************************************
   ********************************************************************* */
void EP8_ISR() interrupt
{
    EXIF &= ~bmBIT4;
    EPIRQ = bmBIT7;
}

#endif   /* ZTEX_ISR_H */
