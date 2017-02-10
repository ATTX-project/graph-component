#FROM openjdk:8
FROM frekele/gradle:3.3-jdk8

RUN apt-get update
RUN apt-get install -y netcat
# Create a non-root user
#RUN useradd -G docker user
#USER user
#WORKDIR /home/user

RUN mkdir -p /tmp/ 
COPY src /tmp/src
COPY build.gradle /tmp/
COPY runTests.sh /tmp/
RUN chmod 755 /tmp/runTests.sh

# Set up environment variables
#ENV GRADLE_URL="https://services.gradle.org/distributions/gradle-3.3-all.zip"
#VOLUME /root/.gradle
#VOLUME /tmp/build

# Install Gradle
#RUN wget $GRADLE_URL -O gradle.zip \
# && unzip gradle.zip \
# && mv gradle-3.3 gradle \
# && rm gradle.zip

#ENV PATH="/gradle/bin:${PATH}"



WORKDIR /tmp/
#CMD ["./runTests"]