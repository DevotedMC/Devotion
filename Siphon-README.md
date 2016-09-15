==== SIPHON ====
======

Siphon is a new submodule for Devotion which can be run continuously in the background, safely and quietly making compressed subdivisions of your data for removal from the server and deployment elsewhere.

It can be run easily with an invocation like:

    java -classpath "../spigot/spigot-1.10.jar;target/devotion-1.1.3.jar" com.programmerdan.minecraft.devotion.siphon.Siphon local-siphon.yml

Replace each jar file in the classpath with the proper ones, and replace the local-siphon.yml with your siphon yml. See the resources folder for an example configuration. Enjoy.

