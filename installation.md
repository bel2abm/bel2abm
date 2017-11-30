# Installation and running of BEL2ABM

## Installation instructions

- install Java 1.6 or compatible from http://java.com
- install Apache Jena 2.6.4 or compatible from jena.apache.org
- install the OpenBEL Framework 2.0.0 Dragon Fruit from https://github.com/OpenBEL/openbel-framework/downloads
- install NetLogo 5.0.5 or compatible from http://ccl.northwestern.edu/netlogo/
- set environment for java, jena and OpenBEL
- download BEL2ABM source code and example folder (SORL1 Case Study folder) from https://github.com/pybel/bel2abm

## Example run

Using the data from the example folder, load `APP_SORLA.bel` as a KAM into the framework:
CD into the installation of the OpenBEL Framework and run 

Windows

```sh
belc.cmd –f path_to_APP_SORLA.bel –k APP_SROLA –d "SORLA KAM" –v 
```

Mac and Linux

```sh
belc.sh –f path_to_APP_SORLA.bel –k APP_SROLA –d "SORLA KAM" –v 
```

Make sure the files from the example directory are in BEL2ABM/src/de/fraunhofer/scai,
CD into `BEL2ABM/src/de/fraunhofer/scai` and run 

```sh
javac BEL2ABM.java
java -cp . BEL2ABM -l -k APP_SORLA -ABMCode BEL2ABM_code.nlogo -v
```

9/25/2017 MG
