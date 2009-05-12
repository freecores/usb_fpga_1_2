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

package ztex;

import ch.ntb.usb.*;

public class UsbException extends Exception {
    public UsbException(String msg) {
	super( msg );
    }

    public UsbException(Usb_Device dev,  String msg) {
	super( "bus=" + dev.getBus().getDirname() + "  device=" + dev.getFilename() + ": " + msg );
    }
}    