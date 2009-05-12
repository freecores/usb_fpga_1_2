if [ "$1" = ""  ]; then
    echo "Usage: $0 <C file> <bmp flags> <sdcc flags>"
    exit 1
fi

exe=""
if [ "$WINDIR" != "" ]; then
    exe=".exe"
fi    

BINDIR=`dirname $0`
BMP=bmp
if [ -x "$BINDIR/bmp$exe" ]; then
    BMP="$BINDIR/bmp$exe"
fi    

if [ -f "$BINDIR/peeph.def" ]; then
    PEEPH="--peep-file $BINDIR/peeph.def"
fi    


inp="$1"
if [ ! -f "$inp" -a -f "$inp.c" ]; then 
    inp="$1.c"
fi
base=${inp%*.c}    

if [ ! -f "$inp" ]; then 
    echo "File not found: $inp"
    exit 2
fi    

bytePad=""
while true; do
    if [ "$LINEINFO" = "no" ]; then
        li='//line %2 "%1"'
    else
        li='#line %2 "%1"'
    fi
    $BMP -c -l "$li" $2 $bytePad $inp -o $base.tmp.c
    ec="$?"
    if [ "$ec" != 0 ]; then
	exit $ec
    fi

    sdcc -mmcs51 --code-loc 0x0200 --code-size 0x2e00 --xram-loc 0x3200 --xram-size 0x1000 --iram-size 256 $PEEPH $3 $base.tmp.c -o $base.ihx
    ec="$?"
    if [ "$ec" != 0 ]; then
	exit $ec
    fi

    addr=`grep ".*:.* _DeviceDescriptor" $base.map | (
        read a b
        echo 0x${a#*:}
    )`    
    echo "Addr=$addr"
    if [ $((addr & 1)) = "0" ]; then
	exit 0
    else
	if [ "$bytePad" != "" ]; then
	    echo "USB Descriptors not word aligned, -DPAD_BYTE has no effect"
	    exit 3
	else
	    bytePad="-D PAD_BYTE"
	fi
    fi
done    