# Makefile for libusbJava, a Java libusb wrapper
# Copyright (C) 2009 ZTEX e.K.
# http://www.ztex.de
#
# This Makefile ia a part of the reorganized version of the libusbJava source tree
# with proper makefiles. (SVN version of Apr. 6, 2009).
#
# Please visit http://libusbjava.sourceforge.net for more information about the
# original project.

#########################
# Configuration section #
#########################

# $(JAVAPREFIX)/include should contain jni.h
JAVAPREFIX=/usr/local/java

###############################
# this should not be modified #
###############################
GCC=gcc
JAVAC=javac
RM=rm -f
INSTALL=install
INSTALLDIR=$(INSTALL) -d 
INSTALLEXE=$(INSTALL) -m 0755
INSTALLFILE=$(INSTALL) -m 0644
LIBSRCS=LibusbJava.c
LIBEXTRADEPS=LibusbJava.h
JAVASRCS=$(shell echo ch/ntb/usb/*.java) $(shell echo ch/ntb/usb/logger/*.java)

LIBTARGET=libusbJava.so
LIBLIBS=-lusb
LIBINCS=-I $(JAVAPREFIX)/include -I $(JAVAPREFIX)/include/linux

.PHONY:	all libs classes install2 clean distclean

all: libs classes

classes: classes.made

libs: $(LIBTARGET)

$(LIBTARGET): $(LIBSRCS) $(LIBEXTRADEPS)
	$(GCC) -shared -std=c99 -Wall -Wl,-static,-soname,$(LIBTARGET) $(LIBINCS) -static $(LIBSRCS) -o $(LIBTARGET) $(LIBLIBS)

classes.made: $(JAVASRCS)
	$(JAVAC) $(JAVASRCS)
	echo > classes.made

install2: all
	$(INSTALLDIR) ../libusbJava/ch/ntb/usb/logger
	$(INSTALLFILE) $(LIBTARGET) ../libusbJava
	$(INSTALLFILE) ch/ntb/usb/*.class ../libusbJava/ch/ntb/usb
	$(INSTALLFILE) ch/ntb/usb/logger/*.class ../libusbJava/ch/ntb/usb/logger

clean:
	$(RM) *.o

distclean:
	$(RM) $(LIBTARGET) ch/ntb/usb/*.class ch/ntb/usb/logger/*.class classes.made
