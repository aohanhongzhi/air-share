#!/bin/bash
./gradlew clean bootJar -x test
scp build/libs/air-share-0.0.1-SNAPSHOT.jar insite@files.cupb.top:/home/insite/app/air-share
scp asset/shell/start3.sh insite@files.cupb.top:/home/insite/app/air-share
ssh insite@files.cupb.top  sh /home/insite/app/air-share/start3.sh
