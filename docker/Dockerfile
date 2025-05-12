from apache/hive:3.1.3

USER root

RUN mkdir -p /tmp/hive/ && mkdir -p /tmp/hive/ && chmod -R 755 /tmp/hive/

ADD "https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.1.2/hadoop-aws-3.1.2.jar" /opt/hive/lib/
ADD "https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.11.375/aws-java-sdk-bundle-1.11.375.jar" /opt/hive/lib/

RUN chmod 644 /opt/hive/lib/hadoop-aws-3.1.2.jar \
 && chmod 644 /opt/hive/lib/aws-java-sdk-bundle-1.11.375.jar \
 && chown root:staff /opt/hive/lib/hadoop-aws-3.1.2.jar \
 && chown root:staff /opt/hive/lib/aws-java-sdk-bundle-1.11.375.jar

RUN chmod 777 /tmp/hive/

COPY ./docker/hive-site.xml /opt/hive/conf/hive-site.xml
COPY ./docker/core-site.xml /opt/hadoop/etc/hadoop/core-site.xml

USER hive