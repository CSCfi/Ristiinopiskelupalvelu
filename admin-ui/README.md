# Development instructions

## Backend
* Run backend module from IDE or command line (AdminUiApplication) with Spring profile "dev". Check the application-dev.yml file for required environment variables and set them in your env accordingly.
* Backend will listen to port 8081 by default. 

## Frontend
* Simply run 'npm run serve' from the frontend module.
* Frontend will listen to port 8080 by default.

## Authentication
* Simply open the backend module (http://localhost:8081/admin-ui) in your browser and it will log you in as a superuser.
* After that, you can use the Vue dev server (http://localhost:8080/admin-ui) as usual.

TODO: fix it so that separate login via browser to backend is not required :) 
