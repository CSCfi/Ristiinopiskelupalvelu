# Images
FROM rockylinux:9.1-minimal as denormalizer

ENV MICRODNF_CMD "microdnf -y --setopt=install_weak_deps=0"

RUN groupadd --system --gid 201 ripa && \
    useradd --system -u 201 -g ripa ripa

RUN $MICRODNF_CMD update \
    && $MICRODNF_CMD install java-17-openjdk \
    && $MICRODNF_CMD clean all

USER ripa
COPY target/ristiinopiskelu-denormalizer.jar ./app.jar
ENTRYPOINT ["sh","-c","java $DENORMALIZER_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
