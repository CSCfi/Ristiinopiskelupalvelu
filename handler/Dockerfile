# Images
FROM eclipse-temurin:17-jdk as handler
RUN addgroup --system --gid 201 ripa && \
    adduser --system -u 201 --ingroup ripa ripa
USER ripa
COPY target/ristiinopiskelu-handler.jar ./app.jar
ENTRYPOINT ["sh","-c","java $HANDLER_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]

