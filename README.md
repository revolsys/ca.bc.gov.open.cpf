# CPF Docker Compose

1. Create the config files from the samples

```
cd config
cp cpf-sample.properties cpf.properties
cp db-sample.properties db.properties
cp postgres-sample.env postgres.env
```

2. Edit the config files for your environment

3. Copy the pub#cpf.war and pub#cpf-worker.war to web/webapps

4. Start docker

docker-compose up

5. Access the app in a web browser

http://localhost:27380/pub/cpf/admin/
http://localhost:27380/pub/cpf/ws/

User: cpf_admin
Password: 4dm1n157r470r

