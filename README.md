# bilibili-dynamic-roller-java
JAVA 实现的B站动态抽奖程序

# Credits
抽奖逻辑的实现基本参考自 https://github.com/LeoChen98/BiliRaffle/blob/master/BiliRaffle/Raffle.cs , 特此感谢原作者！  

# requirements
以`Maven`为例:
```
<!-- fastjson -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.73</version>
</dependency>

<!-- https://mvnrepository.com/artifact/com.google.guava/guava -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>30.1-jre</version>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.5.7</version>
</dependency>
```

# 其它说明
B站登录账号的cookie可以通过使用F12开发者工具里获得
