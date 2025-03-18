# FATImageTool
Java library for manipulating raw disk images containing a FAT file system

Right now, this library is capable of reading information from the Master Boot Record and the Volume Boot Record of a DOS
disk image.  In addition, this library can currently create new directories.

The end goal for the library is to create a formatted, bootable disk image for DOS.

This library is very alpha and will probably change as I get around to adding more capabilities.  Right now, being able
to perform cluster-level operations (read, write, allocate) provides a generous amount of functionality.

Storage and structure representation probably is less than ideal.
