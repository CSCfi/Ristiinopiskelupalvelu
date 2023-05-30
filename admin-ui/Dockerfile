FROM eclipse-temurin:17-jdk as admin-ui
RUN addgroup --system --gid 201 ripa && \
    adduser --system -u 201 --ingroup ripa ripa
USER ripa
COPY backend/target/ristiinopiskelu-admin-ui.jar ./app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
