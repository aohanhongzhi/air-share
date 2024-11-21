#!/bin/bash
./gradlew clean bootJar -x test
# 服务器密码 jLhobpsF6RNKSS-islA5s8ScLZ2EYOThPb1wLWARtSOHyVqUD3qDE3ye47E7z8u_evlWHNzzJ5JgVj9puhQxXuyOUqG0_MlhyWrKy87WFuVx5n8m2VIyGn2JrCaewA0z93h_z2EuxG68uIpLVV4vuzNfm1Q5bxFuiGBBTFzmNvUCteBT4PwTUwBqCW-nIS9xefQ25FM-9JW9X9OSJzBGgx4LI2cJfyCTRKoztMRBKpRkcunKhPoa1kAcktzRhBiYyGu2KKyUB0Ud6sxuc38uECJE5lRzI7Kfz8PsFrXAQJwCFJwt8BsAOvN5AGYsQl7Y2kcs8QhqGKuGMcMxHjtP6A
scp build/libs/air-share-0.0.1-SNAPSHOT.jar insite@files.cupb.top:/home/insite/app/air-share
scp asset/shell/start3.sh insite@files.cupb.top:/home/insite/app/air-share
ssh insite@files.cupb.top  sh /home/insite/app/air-share/start3.sh
