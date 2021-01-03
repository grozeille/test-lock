mvn clean package spring-boot:build-image

docker push grozeille/executor
docker push grozeille/gateway

kubectl apply -f kubernetes\deployment.yaml