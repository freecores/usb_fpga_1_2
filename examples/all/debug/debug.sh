make -C ../../../java distclean all || exit
make distclean all || exit
java -cp Debug.jar Debug $@
