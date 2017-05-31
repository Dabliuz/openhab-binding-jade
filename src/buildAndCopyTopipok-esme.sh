#!/bin/sh

mvn clean package
scp de.unidue.stud.sehawagn.openhab.binding.jade/target/de.unidue.stud.sehawagn.openhab.binding.jade-2.0.0-SNAPSHOT.jar pipok-esme:/opt/openhab/openhab-2.0.0/addons
scp de.unidue.stud.sehawagn.openhab.binding.jadeservice/target/de.unidue.stud.sehawagn.openhab.binding.jadeservice-2.0.0-SNAPSHOT.jar pipok-esme:/opt/openhab/openhab-2.0.0/addons
cp de.unidue.stud.sehawagn.openhab.binding.jade/target/de.unidue.stud.sehawagn.openhab.binding.jade-2.0.0-SNAPSHOT.jar /home/friesenkiwi/workspace/HyGrid/lib/AgentGui/projects/hygrid/lib/openHAB/
