air-share
===

# 目前实现的功能

1. 分块上传
3. 并发上传
2. 断点续传
2. 断点下载

## 企业级推荐

功能比较完善

https://github.com/filebrowser/filebrowser

https://filebrowser.org

线上部署

http://disk.cupb.top/

### 热重启

https://github.com/HotswapProjects/HotswapAgent

VM 参数

```shell
-XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=fatjar
```

#### 介绍

文件分享或者传输中心。可以对标：https://airportal.cn/。
私有化部署。

![img.png](asset/index.png)

![img_1.png](asset/file.png)

#### 软件架构

软件架构说明

第一版本采用SpringBoot的SpringWeb构建，后期采用SpringWebFlux构建。采用Netty做领拷贝。

前端采用 https://www.openzui.com/ 支持文件上传分片。

后期需要加上文件断点上传。

### 部署

```shell
./gradlew clean bootJar -x test
```

```shell
gradlew.bat clean bootJar -x test
```

```shell
ssh insite@insite.cupb.top
```

上传到服务器

```shell
scp build/libs/air-share-0.0.1-SNAPSHOT.jar insite@file.cupb.top:/home/insite/app/
```

上传数据库

```shell
scp airshare.db insite@insite.cupb.top:/home/insite/app/
```

启动

```shell
nohup /opt/jbr/bin/java -Dfile.encoding=utf-8 -Duser.timezone=GMT+08  -jar /home/insite/app/air-share-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod -Xmx1G -Xms512M -server -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError >>/home/insite/app/air-share.log  2>&1 &
```

```shell
nohup /opt/jbr/bin/java -Dfile.encoding=utf-8 -Duser.timezone=GMT+08 -XX:+HeapDumpOnOutOfMemoryError -jar /home/insite/app/air-share-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod --hxy.print.absolute-file-path=/mnt/resource/data/air-share --spring.datasource.url=jdbc:sqlite:/home/insite/app/airshare.db -Xmx1G -Xms512M -server -XX:+UseG1GC >> /home/insite/app/air-share.log 2>&1 &
``` 

服务器显示日志调试

```shell
/media/data/jdk/bin/java -Dfile.encoding=utf-8 -Duser.timezone=GMT+08 -XX:+HeapDumpOnOutOfMemoryError -jar /home/insite/app/air-share-0.0.1-SNAPSHOT.jar --spring.profiles.active=beta --hxy.print.absolute-file-path=/media/data/data/air-share/air-share
```

# docker启动nginx

```shell
docker run -d -p 80:80 -p 443:443 --name rblc-nginx1 -v  /mnt/resource/data/docker/nginx/www:/usr/share/nginx -v /mnt/resource/data/docker/nginx/config/:/etc/nginx/ nginx
```

docker run -d -p 80:80 -p 443:443 --name rblc-nginx1 -v /mnt/resource/data/docker/nginx/www:/usr/share/nginx -v
/mnt/resource/data/docker/nginx/config/:/etc/nginx/ nginx

服务器内存占用

第一次观察

![img.png](asset/server-mem.png)

400720KB / 1024 = 391MB

第二次观察

![img.png](asset/server-memory.png)

484202KB / 1024 = 472MB

也就是air-share启动的时候占用了 391MB的内存，显然这么个小程序占用这么大的内存着实有点浪费了，这也是SpringBoot非常占用内存的名声来源了。

```shell
ps aux |grep air
```

![img.png](asset/ps-aux.png)

下图是 air-share(SpringBoot)和[filebrowser](https://github.com/filebrowser/filebrowser)(Go)
的对比。filebrowser的功能要比air-share更加丰富，但是内存只有26分之一。

![img.png](asset/memory.png)

```nginx配置

server {
    listen       80;
    listen  [::]:80;
    server_name files.cupb.top;

    location / {
         root /usr/share/nginx/html/dist;
         index  index.html index.htm;
    }
    location /api/ {
        proxy_set_header HOST $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://10.2.0.4:8888/;
        proxy_next_upstream error timeout invalid_header http_500 http_503 http_404;
    }


}


server {
    listen     443  ssl http2;
    server_name  files.cupb.top;
    add_header Cache-Control no-store;
    add_header Cache-Control private;
           
    ssl_certificate    /etc/nginx/files.cupb.top/files.cupb.top.pem;
    ssl_certificate_key   /etc/nginx/files.cupb.top/files.cupb.top.key;

    # ssl_certificate    /etc/nginx/files.cupb.top_ecc/fullchain.cer;
    # ssl_certificate_key   /etc/nginx/files.cupb.top_ecc/files.cupb.top.key;

    ssl_session_cache    shared:SSL:1m;
    ssl_session_timeout  5m;

    ssl_protocols TLSv1.3;    
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;       
    ssl_prefer_server_ciphers  on;

    location / {
         root /usr/share/nginx/html/dist;
         index  index.html index.htm;
    }
    location /api/ {
        proxy_set_header HOST $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass http://10.2.0.4:8888/;
        proxy_next_upstream error timeout invalid_header http_500 http_503 http_404;
    }


    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   html;
    }
}

```

# TODO

-[ ] 断点续传

# 拓展

只能查看文件列表

```
python3 -m http.server -b 192.168.0.118 8080
```

# 学习

[SpringBoot SpringMVC文件下载，大文件断点续传，可以实时播放视频,拖动进度条](https://blog.csdn.net/qq_41389354/article/details/105043312)

# 其他类似轮子

https://github.com/beavailable/share

https://gitee.com/folder-share

/media/data/data/air-share/air-share/2022-12-15

# 性能监控

http://localhost:8888/koTime

http://files.cupb.top/api/koTime

![img_1.png](asset/kotime.png)