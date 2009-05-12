# This Makefile defines the standard rules for building the project.
#
# The following variables are used:
#
# ZTEXPREFIX
#   Defines location if this firmware / driver kit.
#   Must be defined!
#   Example: ZTEXPREFIX=../../..
#
# JARTARGET
#   The name of the jar archive
#   Example: JARTARGET=UCEcho.jar
#
# CLASSTARGETS
#   Java Classes that have to be build 
#   Example: CLASSTARGETS=UCEcho.class
#
# CLASSEXTRADEPS
#   Extra dependencies for Java Classes
#   Example: CLASSEXTRADEPS:=$(shell echo $(ZTEXPREFIX)/java/ztex/*.java)
#
# IHXTARGETS
#   ihx files (firmware ROM files) that have to be build 
#   Example: IHXTARGETS=ucecho.ihx
#
# IHXEXTRADEPS
#   Extra Dependencies for ihx files
#   Example: IHXEXTRADEPS:=$(shell echo $(ZTEXPREFIX)/include/*.h)
#
# EXTRAJARFILES
#   Extra files that should be included into th jar achieve
#   Example: EXTRAJARFILES=ucecho.ihx fpga/ucecho.bin

.PHONY: all ihx jar clean distclean

JAVAC=javac
SDCC=$(ZTEXPREFIX)/bin/bmpsdcc.sh
CLASSPATH:=.:$(ZTEXPREFIX)/libusbJava:$(ZTEXPREFIX)/java:$(CLASSPATH)
INCLUDES=-I $(ZTEXPREFIX)/include/

all : ihx jar 
ihx : $(IHXTARGETS)
jar : $(JARTARGET)

%.ihx: %.c $(IHXEXTRADEPS)
	$(SDCC) $< "$(INCLUDES)"

%.class: %.java $(CLASSEXTRADEPS)
	$(JAVAC) -cp "$(CLASSPATH)" $<

$(JARTARGET) : $(CLASSTARGETS) $(EXTRAJARFILES)
	jar cf $(JARTARGET) *.class $(EXTRAJARFILES) -C $(ZTEXPREFIX)/libusbJava . $(shell cd $(ZTEXPREFIX)/java; ls ztex/*.class | while read a; do echo "-C $(ZTEXPREFIX)/java $$a"; done)

clean: 
	rm -f *~ *.bak *.old
	rm -f *.class 
	rm -f *.rel *.rst *.lnk *.lst *.map *.asm *.sym *.mem *.tmp.c 

distclean: clean
	rm -f $(JARTARGET)
	rm -f *.ihx
