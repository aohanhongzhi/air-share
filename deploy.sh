#!/bin/bash
./gradlew clean bootJar -x test
scp build/libs/air-share-0.0.1-SNAPSHOT.jar insite@file.cupb.top:/home/insite/app/
scp asset/shell/restart.sh insite@file.cupb.top:/home/insite/app/
ssh insite@file.cupb.top  sh /home/insite/app/restart.sh
