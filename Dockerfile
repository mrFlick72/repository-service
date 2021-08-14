FROM ghcr.io/graalvm/graalvm-ce:21.2.0

ADD target/repository-service /usr/local/repository-service/

VOLUME /var/log/onlyone-portal

WORKDIR /usr/local/repository-service/

CMD ["repository-service"]