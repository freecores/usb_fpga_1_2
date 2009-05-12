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
   Puts everything in the right order together.
*/

#ifndef[ZTEX_H]
#define[ZTEX_H]

/* *********************************************************************
   ***** include the basic functions ***********************************
   ********************************************************************* */
#include[ztex-utils.h]


/* *********************************************************************
   ***** Flash memory support ******************************************
   ********************************************************************* */
#ifeq[FLASH_ENABLED][1]
#ifeq[PRODUCT_IS][UFM-1_1]
#include[ztex-flash1.h]
#elifeq[PRODUCT_IS][UFM-1_2]
#include[ztex-flash1.h]
#else
#warning[FLASH option is not supported by this product]
#define[FLASH_ENABLED][0]
#endif
#endif


/* *********************************************************************
   ***** EEPROM support and some I2c helper functions ******************
   ********************************************************************* */
#ifneq[EEPROM_DISABLED][1]
#include[ztex-eeprom.h]
#endif

/* *********************************************************************
   ***** FPGA configuration support ************************************
   ********************************************************************* */
#ifeq[PRODUCT_IS][UFM-1_0]
#include[ztex-fpga.h]
#elifeq[PRODUCT_IS][UFM-1_1]
#include[ztex-fpga.h]
#elifeq[PRODUCT_IS][UFM-1_2]
#include[ztex-fpga.h]
#endif

/* *********************************************************************
   ***** define the descriptors and the interrupt routines *************
   ********************************************************************* */
#include[ztex-descriptors.h]
#include[ztex-isr.h]


/* *********************************************************************
   ***** init_USB ******************************************************
   ********************************************************************* */
#define[EPXCFG(][);][    EP$0CFG = 
#ifeq[EP$0_DIR][IN]
	bmBIT7 | bmBIT6
#elifeq[EP$0_DIR][OUT]
	bmBIT7
#else
	0
#endif
#ifeq[EP$0_TYPE][BULK]
	| bmBIT5 
#elifeq[EP$0_TYPE][ISO]
	| bmBIT4
#elifeq[EP$0_TYPE][INT]
	| bmBIT5 | bmBIT4
#endif
#ifeq[EP$0_SIZE][1024]
	| bmBIT3
#endif
#ifeq[EP$0_BUFFERS][2]
	| bmBIT1
#elifeq[EP$0_BUFFERS][3]
	| bmBIT1 | bmBIT0
#endif	
	;
	SYNCDELAY;
]

#define[EP1XCFG(][);][#ifeq[EP$0_TYPE][BULK]
	EP$0CFG = bmBIT7 | bmBIT5;
#elifeq[EP$0_TYPE][ISO]
	EP$0CFG = bmBIT7 | bmBIT4;
#elifeq[EP$0_TYPE][INT]
	EP$0CFG = bmBIT7 | bmBIT5 | bmBIT4;
#else 	
	EP$0CFG = 0;
#endif
	SYNCDELAY;
]

void init_USB ()
{

    CPUCS = bmBIT4 | bmBIT1;
    CKCON &= ~7;
    
    IOA1 = 1;		
    OEA |= bmBIT1;
    
    EA = 0;

    ENABLE_AVUSB;
    
    INIT_INTERRUPT_VECTOR(INTVEC_SUDAV, SUDAV_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_SOF, SOF_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_SUTOK, SUTOK_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_SUSPEND, SUSP_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_USBRESET, URES_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_HISPEED, HSGRANT_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP0ACK, EP0ACK_ISR);

    INIT_INTERRUPT_VECTOR(INTVEC_EP0IN, EP0IN_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP0OUT, EP0OUT_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP1IN, EP1IN_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP1OUT, EP1OUT_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP2, EP2_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP4, EP4_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP6, EP6_ISR);
    INIT_INTERRUPT_VECTOR(INTVEC_EP8, EP8_ISR);

    EXIF &= ~bmBIT4;
    USBIRQ = 0x7f;
    USBIE |= 0x7f; 
    EPIRQ = 0xff;
    EPIE = 0xff;
    
    EUSB = 1;
    EA = 1;

    USBCS |= 0x08;
    USBCS |= bmBIT7 | bmBIT1;
    wait(500);
    USBCS &= ~0x08;
    wait(500);  
    
    EP1XCFG(1IN);
    EP1XCFG(1OUT);
    EPXCFG(2);
    EPXCFG(4);
    EPXCFG(6);
    EPXCFG(8);
}

#endif   /* ZTEX_H */
