FROM frekele/gradle:3.3-jdk8


RUN apt-get update
RUN apt-get install -y netcat 

RUN mkdir -p /tmp/

WORKDIR /tmp/

COPY build /tmp/build
COPY build.test.gradle /tmp/build.gradle
COPY runTests.sh /tmp/
RUN chmod 700 /tmp/runTests.sh

