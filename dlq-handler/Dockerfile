FROM eclipse-temurin:17-jdk as dlqhandler
RUN addgroup --system --gid 201 ripa && \
    adduser --system -u 201 --ingroup ripa ripa
USER ripa
COPY target/ristiinopiskelu-dlqhandler.jar ./app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
