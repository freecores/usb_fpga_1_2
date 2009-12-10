/*!
   flashdemo -- Flash memory example
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

#include[ztex-conf.h]	// Loads the configuration macros, see ztex-conf.h for the available macros
#include[ztex-utils.h]	// include basic functions and variables

// Cypress vendor ID and product ID may only (!) be used for experimental purposes
SET_VPID(0x4b4,0x8613);		

// select ZTEX USB Module 1.0 as target (required for FLASH support)
IDENTITY_UM_1_0(10.20.0.0,0);	 
// uncomment the following line and comment the line above to run this example on ZTEX USB Module 1.2
//IDENTITY_UFM_1_2(10.11.0.0,0);	 

// enable Flash support
ENABLE_FLASH;

// this product string is also used for identification of the firmware in FlashDemo.java
#define[PRODUCT_STRING]["Flash demo"]

code char flash_string[] = "Hello World!";

// include the main part of the firmware kit, define the descriptors, ...
#include[ztex.h]

void main(void)	
{
    xdata DWORD sector;

    init_USB();						// init everything
    
    if ( flash_enabled ) {
	flash_read_init( 0 ); 				// prepare reading sector 0
	flash_read(&sector, 4); 			// read the number of last sector 
	flash_read_finish(flash_sector_size - 4);	// dummy-read the rest of the sector + finish read operation
	
	sector++;					
	if ( sector > flash_sectors || sector == 0 ) {
	    sector = 1;
	}

	flash_write_init( 0 ); 				// prepare writing sector 0
	flash_write(&sector, 4); 			// write the current sector number
	flash_write_finish(flash_sector_size - 4);	// dummy-write the rest of the sector + finish write operation
	
	flash_write_init( sector ); 			// prepare writing sector sector
	flash_write((xdata*) flash_string, sizeof(flash_string)); 	// write the string 
	flash_write_finish(flash_sector_size - sizeof(flash_string));	// dummy-write the rest of the sector + finish write operation
    } 
    
    while (1) {	}					//  twiddle thumbs
}

