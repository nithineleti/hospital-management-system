#!/bin/bash

yum update -y

yum install -y java-17-amazon-corretto-headless maven git

git clone https://github.com/nithineleti/hospital-management-system.git

cd hospital-management-system

./mvnw clean package -DskipTests

nohup java -jar target/hospital-management-system-1.0.0.jar &
