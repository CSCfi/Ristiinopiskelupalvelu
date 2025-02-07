FROM rockylinux:9.1-minimal as httpd

ENV MICRODNF_CMD "microdnf -y --setopt=install_weak_deps=0"

EXPOSE 8080 8443

COPY apache/yum.repos.d/shibboleth.repo /etc/yum.repos.d/shibboleth.repo

RUN $MICRODNF_CMD update \
    && $MICRODNF_CMD install wget httpd shibboleth mod_ssl \
    && $MICRODNF_CMD clean all

COPY apache/bin/httpd-shibd-foreground /usr/local/bin/

#WORKDIR /var/www/html/
#RUN mkdir doc
#COPY target/swagger/output/ ./doc/

RUN openssl req -nodes -subj '/CN=localhost' -x509 -newkey rsa:4096 -keyout /etc/pki/tls/private/ripa.key -out /etc/pki/tls/certs/ripa.pem -days 365 && \
    chmod +x /usr/local/bin/httpd-shibd-foreground && \
    chown apache:apache /var/{log,run}/httpd && \
    chown apache:apache /var/{log,run}/shibboleth

USER apache

CMD ["httpd-shibd-foreground"]
