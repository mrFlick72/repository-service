FROM openjdk:11

ADD build/libs/repository-service.jar /usr/local/repository-service/

VOLUME /var/log/onlyone-portal

WORKDIR /usr/local/repository-service/

CMD ["java",  "-jar", "repository-service.jar"]