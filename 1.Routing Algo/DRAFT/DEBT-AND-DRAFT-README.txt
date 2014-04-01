#
# DRAFT and DEBT running instructions
# By Matthew Orlinski <me@shigs.co.uk>, copyright 2013
#
# Tested using Debian Squeeze and JVM 1.7
# 
#

Step 1: Copy ALL of the files from the zip into your ./one_1.4.1 directory
Step 2: run `./compile.bat` without quotes.


Running DRAFT example
=====================
These instructions will let you run the DRAFT protocol as published 
in the Journal of Ad Hoc Networks. Please cite using the details on the following DOI:
http://dx.doi.org/10.1016/j.adhoc.2013.03.003
http://www.sciencedirect.com/science/article/pii/S1570870513000334

Included is the Infocom5 dataset in the ONE standard event reader format.
To run DRAFT using the Infocom5 dataset simply run the following command in your ONE
directory:

(Linux)> ./one.sh -b 1 ./scenarios/DRAFT/DRAFT-infocom5-example.txt
(Windows)> ./one.bat -b 1 ./scenarios/DRAFT/DRAFT-infocom5-example.txt

This simulation should take less than 5 minutes on a standard desktop PC
(tested on a E8600 Intel quad-core with 8GB of DDR2)


Running DEBTT, DEBTE, and DEBTC
===============================

These instructions will let you run the DEBTT protocol as published 
at Wireless Days 2012. Please cite using the details on the following link:
http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=6402810

DEBTE, produces a relatively high delivery probability and overheads.
DEBTT, should produce the same delivery probability (or very close) to DEBTE, with lower overheads.
DEBTC, should produce a lower delivery probability with low overheads.

Included in this package is the Infocom5 dataset in the ONE standard event reader format.
To run DEBTT, DEBTC, or DEBTE using the Infocom5 dataset simply run one
of the following commands in your ONE directory:

(Linux)> ./one.sh -b 1 ./scenarios/DEBT/DEBTT/DEBTT-infocom5-example.txt
(Linux)> ./one.sh -b 1 ./scenarios/DEBT/DEBTE/DEBTE-infocom5-example.txt
(Linux)> ./one.sh -b 1 ./scenarios/DEBT/DEBTC/DEBTC-infocom5-example.txt


My DEBTT code is very slow. This simulation will take longer than DRAFT.
Current version of DEBTT also needs a lot of RAM to be allocated to the JVM (~3GB for a 41 node simulation).
Assuming you have enough RAM, increase the number allocated to -Xmx in the file one.sh.

