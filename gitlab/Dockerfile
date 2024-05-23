# This Dockerfile creates a static build image for CI

FROM eclipse-temurin:17

# Just matched `app/build.gradle.kts`
ENV ANDROID_COMPILE_SDK "33"
# Just matched `app/build.gradle.kts`
ENV ANDROID_BUILD_TOOLS "33.0.2"

ENV ANDROID_HOME /android-sdk-linux
ENV PATH="${PATH}:/android-sdk-linux/platform-tools/"

RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"

# install OS packages
RUN apt-get --quiet update --yes
RUN apt-get --quiet install --yes wget apt-utils tar unzip lib32stdc++6 lib32z1 build-essential ruby ruby-dev
# We use this for xxd hex->binary
RUN apt-get --quiet install --yes vim-common
# install Android SDK
RUN wget --quiet --output-document=android-sdk.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
RUN unzip android-sdk.zip -d android-sdk-linux/
RUN mkdir android-sdk-linux/cmdline-tools/latest
RUN mv android-sdk-linux/cmdline-tools/* android-sdk-linux/cmdline-tools/latest || true
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "platforms;android-${ANDROID_COMPILE_SDK}"
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "platform-tools"
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS}"
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "extras;android;m2repository"
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "extras;google;google_play_services"
RUN echo y | android-sdk-linux/cmdline-tools/latest/bin/sdkmanager "extras;google;m2repository"
