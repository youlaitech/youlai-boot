# 基础镜像
FROM openjdk:17

# 维护者信息
LABEL maintainer="youlai <youlaitech@163.com>"

# 设置时区（Debian直接使用环境变量）
ENV TZ=Asia/Shanghai

# 在运行时自动挂载 /tmp 目录为匿名卷
VOLUME /tmp

# 添加应用
ADD target/youlai-boot.jar app.jar

# 启动命令
CMD java \
    -Xms512m -Xmx512m \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /app.jar

# 暴露端口
EXPOSE 8989
